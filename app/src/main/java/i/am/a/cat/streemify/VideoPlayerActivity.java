package i.am.a.cat.streemify;

import static android.widget.Toast.LENGTH_SHORT;

import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {
    public static Boolean isFullScreen = false;
    private final Handler handler = new Handler();
    LinearLayout swipeControlLayout;
    private Boolean isBound = false;
    private Boolean isVideoLocked = false;
    private Boolean isLockScreenVisible = false;
    private PlaybackService playbackService;
    private PlayerView videoPlayer;
    private RecyclerView availableVideo;
    private VideoAdapter videoAdapter;
    private ArrayList<Video> videoList;
    private AppCompatImageButton backArrow, settingButton, previousButton, playPauseButton, nextButton, fullScreenButton;
    private TextView videoName, videoDuration, lockscreenButton, bottomSheetLockscreen, audioTrackButton;
    private DefaultTimeBar progressBar;
    private MediaController mediaController;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlaybackService.PlayBackBinder binder = (PlaybackService.PlayBackBinder) iBinder;
            playbackService = binder.getService();
            isBound = true;

            // MediaController ফিউচার পান
            ListenableFuture<MediaController> controllerFuture = playbackService.getControllerFuture();

            // MediaController সেটআপ করুন
            Futures.addCallback(controllerFuture, new FutureCallback<MediaController>() {
                @Override
                public void onSuccess(MediaController result) {
                    mediaController = result;
                    // PlayerView-এ MediaController সেট করুন
                    videoPlayer.setPlayer(mediaController);
                    videoPlayer.setControllerAutoShow(false);

                    // UI আপডেট করুন
                    updateUIWithController();
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    Toast.makeText(VideoPlayerActivity.this, "ভিডিও প্লেয়ার কানেক্ট হয়নি!", LENGTH_SHORT).show();
                    mediaController = null;

                }
            }, ContextCompat.getMainExecutor(VideoPlayerActivity.this)); // সঠিক এক্সিকিউটর
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
            playbackService = null;
            mediaController = null;
        }

        private void updateUIWithController() {
            if (playbackService != null) {
                String currentVideoTitle = playbackService.getCurrentVideoName();
                videoName.setText(currentVideoTitle);

                playbackService.setCallback(new PlaybackService.Callback() {
                    @Override
                    public void onVideoTitleChanged(String title) {
                        runOnUiThread(() -> {
                            videoName.setText(title);
                            videoPlayer.hideController();
                        });
                    }
                });
            }

            videoAdapter = new VideoAdapter(VideoPlayerActivity.this, videoList, VideoPlayerActivity.this::onVideoClick);
            availableVideo.setAdapter(videoAdapter);
            updateProgressBar();
        }
    };
    private GestureDetector gestureDetector, gestureDetectorBrightness, gestureDetectorVolume;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.player), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI Components
        initializeUiComponents();

        videoList = getIntent().getParcelableArrayListExtra("videoList");
        String currentVideoPath = getIntent().getStringExtra("videoPath");

        //Service+Activity কানেক্ট করা
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        serviceIntent.putExtra("videoPath", currentVideoPath);
        serviceIntent.putParcelableArrayListExtra("videoList", videoList);
        startService(serviceIntent);

        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(VideoPlayerActivity.this);
                bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog);
                bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
                bottomSheetDialog.setCancelable(true);
                bottomSheetDialog.setCanceledOnTouchOutside(true);

                bottomSheetLockscreen = bottomSheetDialog.findViewById(R.id.bottom_sheet_lock_screen);
                bottomSheetLockscreen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomSheetDialog.dismiss();
                        if (!isFullScreen) {
                            fullscreenOn();
                        }

                        isVideoLocked = true;
                        videoPlayer.setUseController(false);

                        lockscreenButton.setVisibility(View.VISIBLE); // লক বাটন দেখাবে


                        videoPlayer.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                if (!isVideoLocked) {
                                    return false;
                                }
                                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                    if (isLockScreenVisible) {
                                        lockscreenButton.setVisibility(View.GONE);
                                    } else {
                                        lockscreenButton.setVisibility(View.VISIBLE);
                                    }
                                    isLockScreenVisible = !isLockScreenVisible;
                                }
                                return true;
                            }
                        });
                    }
                });

                lockscreenButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        isVideoLocked = false;
                        videoPlayer.setUseController(true);
                        lockscreenButton.setVisibility(View.GONE);
                        isLockScreenVisible = false; // আনলক করলে বাটন লুকাবে

                        Toast.makeText(VideoPlayerActivity.this, "আনলক হয়েছে", LENGTH_SHORT).show();
                        videoPlayer.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                int screenWidth = view.getWidth();
                                float touchX = motionEvent.getX();

                                // 🔹 **Zoom করা হলে স্কেল রিসেট করবো**
                                if (Gesture.SwipeGesture.isScaling) {
                                    Gesture.SwipeGesture.isScaling = false; // ✅ স্কেল রিসেট করছি
                                    videoPlayer.setScaleX(1.0f);
                                    videoPlayer.setScaleY(1.0f);
                                    fullscreenOn();
                                    isFullScreen = true; // Fullscreen মোড চালু
                                    return true;
                                }

                                // 🔹 **Fullscreen মোড না থাকলে Swipe Gesture কাজ করবে**
                                if (!isFullScreen) {
                                    return gestureDetector.onTouchEvent(motionEvent);
                                }

                                // 🔹 **Fullscreen মোডে থাকলে Gesture কাজ করবে**
                                if (isFullScreen) {
                                    if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                                        Log.d("GESTURE", "Scrolling detected");

                                        if (touchX >= (screenWidth * 0.75)) {
                                            // ✅ ডান পাশে (৭৫%+) Volume Gesture কাজ করবে
                                            Log.d("GESTURE", "Volume Gesture Triggered");
                                            return gestureDetectorVolume.onTouchEvent(motionEvent);
                                        } else if (touchX <= (screenWidth * 0.25)) {
                                            // ✅ বাম পাশে (২৫%-) Brightness Gesture কাজ করবে
                                            Log.d("GESTURE", "Brightness Gesture Triggered");
                                            return gestureDetectorBrightness.onTouchEvent(motionEvent);
                                        } else {
                                            // ❌ মাঝখানের ৫০% অংশ Gesture নিষ্ক্রিয় করবো
                                            Log.d("GESTURE", "No Gesture in Center Zone");
                                            return false;
                                        }
                                    }

                                    // ✅ Gesture চেক করবো
                                    boolean swipeDetected = gestureDetector.onTouchEvent(motionEvent);
                                    boolean brightnessDetected = gestureDetectorBrightness.onTouchEvent(motionEvent);
                                    boolean volumeDetected = gestureDetectorVolume.onTouchEvent(motionEvent);

                                    return swipeDetected || brightnessDetected || volumeDetected;
                                }

                                return false; // যদি কোনও gesture detect না হয়
                            }
                        });
                    }
                });


                TextView quality = bottomSheetDialog.findViewById(R.id.bottom_sheet_quality);

                if (playbackService.getExoPlayer().getVideoFormat() != null) {
                    int width = playbackService.getExoPlayer().getVideoFormat().width;
                    int height = playbackService.getExoPlayer().getVideoFormat().height;

                    // ভিডিও রেজোলিউশন নির্ধারণ
                    String resolution;
                    if (height >= 2160) {
                        resolution = "4K (2160p)";
                    } else if (height >= 1440) {
                        resolution = "2K (1440p)";
                    } else if (height >= 1080) {
                        resolution = "Full HD (1080p)";
                    } else if (height >= 720) {
                        resolution = "HD (720p)";
                    } else if (height >= 480) {
                        resolution = "SD (480p)";
                    } else if (height >= 360) {
                        resolution = "360p";
                    } else if (height >= 240) {
                        resolution = "240p";
                    } else {
                        resolution = "Low Resolution";
                    }

                    quality.setText(resolution);
                } else {
                    quality.setText("Resolution not available");
                }

                // Spinner সেটআপ
                Spinner spinner = bottomSheetDialog.findViewById(R.id.speedSpinner);
                ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.playback_speeds, android.R.layout.simple_spinner_item);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerAdapter);
                spinner.setSelection(2);
                // Spinner item selection listener
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        float speed;
                        switch (i) {
                            case 0:
                                speed = 0.25f;
                                bottomSheetDialog.dismiss();
                                spinner.setSelection(0);
                                break;
                            case 1:
                                speed = 0.5f;
                                spinner.setSelection(1);
                                bottomSheetDialog.dismiss();
                                break;
                            case 2:
                                speed = 1.0f;
                                spinner.setSelection(2);
                                break;
                            case 3:
                                speed = 1.5f;
                                spinner.setSelection(3);
                                bottomSheetDialog.dismiss();
                                break;
                            case 4:
                                speed = 2.0f;
                                spinner.setSelection(4);
                                bottomSheetDialog.dismiss();
                                break;
                            case 5:
                                speed = 4.0f;
                                spinner.setSelection(5);
                                bottomSheetDialog.dismiss();
                                break;
                            case 6:
                                speed = 5.0f;
                                spinner.setSelection(6);
                                bottomSheetDialog.dismiss();
                                break;
                            default:
                                speed = 1.0f;

                        }
                        playbackService.getExoPlayer().setPlaybackParameters(new PlaybackParameters(speed));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }

                });

                audioTrackButton = bottomSheetDialog.findViewById(R.id.bottom_sheet_audio_track);
                audioTrackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(VideoPlayerActivity.this, "This Feature not available right now", LENGTH_SHORT).show();
                    }
                });


                bottomSheetDialog.show();
            }
        });


        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaController != null) {
                    mediaController.seekToPrevious();
                }
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaController != null) {
                    if (mediaController.isPlaying()) {
                        mediaController.pause();
                        playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
                    } else {
                        mediaController.play();
                        playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
                    }
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mediaController != null) {
                    mediaController.seekToNextMediaItem();
                    playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
                }
            }
        });

        progressBar.addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(@NonNull TimeBar timeBar, long position) {
            }

            @Override
            public void onScrubMove(@NonNull TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
                if (!canceled) {
                    mediaController.seekTo(position);
                }
            }
        });

        fullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFullScreen) {
                    fullscreenOn();
                } else {
                    fullScreenOff();
                }
            }
        });


        gestureDetector = new GestureDetector(this, new Gesture.SwipeGesture(this, videoPlayer, availableVideo));
        gestureDetectorBrightness = new GestureDetector(this, new Gesture.BrightnessGesture(this, videoPlayer, swipeControlLayout));
        gestureDetectorVolume = new GestureDetector(this, new Gesture.VolumeGesture(this, videoPlayer, swipeControlLayout));

        videoPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int screenWidth = view.getWidth();
                float touchX = motionEvent.getX();

                // 🔹 **Zoom করা হলে স্কেল রিসেট করবো**
                if (Gesture.SwipeGesture.isScaling) {
                    Gesture.SwipeGesture.isScaling = false; // ✅ স্কেল রিসেট করছি
                    videoPlayer.setScaleX(1.0f);
                    videoPlayer.setScaleY(1.0f);
                    fullscreenOn();
                    return true;
                }

                // 🔹 **Fullscreen মোড না থাকলে Swipe Gesture কাজ করবে**
                if (!isFullScreen) {
                    return gestureDetector.onTouchEvent(motionEvent);
                }

                // 🔹 **Fullscreen মোডে থাকলে Gesture কাজ করবে**
                if (isFullScreen) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        Log.d("GESTURE", "Scrolling detected");

                        if (touchX >= (screenWidth * 0.75)) {
                            // ✅ ডান পাশে (৭৫%+) Volume Gesture কাজ করবে
                            Log.d("GESTURE", "Volume Gesture Triggered");
                            return gestureDetectorVolume.onTouchEvent(motionEvent);
                        } else if (touchX <= (screenWidth * 0.25)) {
                            // ✅ বাম পাশে (২৫%-) Brightness Gesture কাজ করবে
                            Log.d("GESTURE", "Brightness Gesture Triggered");
                            return gestureDetectorBrightness.onTouchEvent(motionEvent);
                        } else {
                            // ❌ মাঝখানের ৫০% অংশ Gesture নিষ্ক্রিয় করবো
                            Log.d("GESTURE", "No Gesture in Center Zone");
                            return false;
                        }
                    }

                    // ✅ Gesture চেক করবো
                    boolean swipeDetected = gestureDetector.onTouchEvent(motionEvent);
                    boolean brightnessDetected = gestureDetectorBrightness.onTouchEvent(motionEvent);
                    boolean volumeDetected = gestureDetectorVolume.onTouchEvent(motionEvent);

                    return swipeDetected || brightnessDetected || volumeDetected;
                }

                return false; // যদি কোনও gesture detect না হয়
            }
        });


        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFullScreen) {
                    fullScreenOff();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            fullScreenOff();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    private void initializeUiComponents() {
        videoPlayer = findViewById(R.id.videoPlayer);
        availableVideo = findViewById(R.id.availableVideo);
        backArrow = videoPlayer.findViewById(R.id.backArrow);
        videoName = videoPlayer.findViewById(R.id.videoName);
        settingButton = videoPlayer.findViewById(R.id.settingsButton);
        previousButton = videoPlayer.findViewById(R.id.previousButton);
        playPauseButton = videoPlayer.findViewById(R.id.playButton);
        nextButton = videoPlayer.findViewById(R.id.nextButton);
        videoDuration = videoPlayer.findViewById(R.id.videoDuration);
        progressBar = videoPlayer.findViewById(R.id.progressBar);
        fullScreenButton = videoPlayer.findViewById(R.id.fullScreenButton);
        lockscreenButton = findViewById(R.id.lockscreenButton);
        swipeControlLayout = findViewById(R.id.swipe_control_layout);

    }

    private void fullscreenOn() {
        if (isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        // ভিডিও প্লেয়ারের সাইজ পুরো স্ক্রীন করার জন্য সেট করা হচ্ছে
        ViewGroup.LayoutParams params = videoPlayer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videoPlayer.setLayoutParams(params);

        fullScreenButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fullscreen_exit, null));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        isFullScreen = true;
    }


    private void fullScreenOff() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // ভিডিও প্লেয়ারের সাইজ স্বাভাবিক করা হচ্ছে
        ViewGroup.LayoutParams params = videoPlayer.getLayoutParams();
        params.height = getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._175sdp);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        videoPlayer.setLayoutParams(params);

        availableVideo.setVisibility(View.VISIBLE);

        fullScreenButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_fullscreen_enter, null));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        isFullScreen = false;
    }


    private void updateProgressBar() {
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                if (mediaController != null) {
                    long currentPosition = mediaController.getCurrentPosition();
                    long totalDuration = mediaController.getDuration();

                    String durationText = videoAdapter.formatDuration(totalDuration - currentPosition);
                    videoDuration.setText(durationText);

                    progressBar.setPosition(currentPosition);
                    progressBar.setBufferedPosition(mediaController.getBufferedPosition());
                    progressBar.setDuration(totalDuration);
                }
                handler.postDelayed(this, 100); // প্রতি 100 মিলিসেকেন্ডে রিফ্রেশ হবে
            }
        };
        handler.post(updateTask);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            ViewGroup.LayoutParams params = videoPlayer.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            videoPlayer.setLayoutParams(params);

        } else {
            availableVideo.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = videoPlayer.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._175sdp);
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            videoPlayer.setLayoutParams(params);
        }
        videoPlayer.hideController();
    }

    private Boolean isPortrait() {
        if (mediaController != null && mediaController.isPlaying()) {
            int height = Objects.requireNonNull(playbackService.getExoPlayer().getVideoFormat()).height;
            int width = playbackService.getExoPlayer().getVideoFormat().width;

            // Rotation চেক করা
            int rotation = playbackService.getExoPlayer().getVideoFormat().rotationDegrees;
            if (rotation == 90 || rotation == 270) {
                // Rotation 90° বা 270° হলে height এবং width উল্টো হয়ে যাবে
                return height < width;
            }

            return height > width;
        }
        return false;
    }

    @Override
    public void onVideoClick(Video video) {
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        serviceIntent.putExtra("videoPath", video.getPath());
        serviceIntent.putParcelableArrayListExtra("videoList", videoList);
        startService(serviceIntent);
        playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mediaController.isPlaying()) {
                startPipMode();

            }
        }
    }

    private void startPipMode() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
            if (!isPortrait()) {
                Rational aspectRatio = new Rational(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                pipBuilder.setAspectRatio(aspectRatio);
            } else {
                Rational aspectRatio = new Rational(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                pipBuilder.setAspectRatio(aspectRatio);
            }

            enterPictureInPictureMode(pipBuilder.build());
            videoPlayer.setControllerAutoShow(false);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!playbackService.getExoPlayer().isPlaying()) {
            if (isBound) {
                unbindService(serviceConnection);
                isBound = false;
            }
            playbackService.stopSelf();
        }
        handler.removeCallbacksAndMessages(handler);
    }
}