package i.am.a.cat.streemify.ui.screen;

import static android.widget.Toast.LENGTH_SHORT;
import static i.am.a.cat.streemify.utils.VideoPlayerHelper.fullscreenOn;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Objects;

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.adapter.VideoAdapter;
import i.am.a.cat.streemify.data.Video;
import i.am.a.cat.streemify.databinding.ActivityVideoPlayerBinding;
import i.am.a.cat.streemify.gesture.BrightnessGesture;
import i.am.a.cat.streemify.gesture.SwipeGesture;
import i.am.a.cat.streemify.gesture.VolumeGesture;
import i.am.a.cat.streemify.services.PlaybackService;
import i.am.a.cat.streemify.utils.VideoPlayerHelper;

@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {
    public static Boolean isFullScreen = false;
    private final Handler handler = new Handler();
    LinearLayout swipeControlLayout;
    private ActivityVideoPlayerBinding binding;
    private Boolean isBound = false;
    private Boolean isVideoLocked = false;
    private Boolean isLockScreenVisible = false;
    private PlaybackService playbackService;
    private VideoAdapter videoAdapter;
    private ArrayList<Video> videoList;
    private AppCompatImageButton playPauseButton, fullScreenButton;
    private TextView videoName, videoDuration, bottomSheetLockscreen, audioTrackButton;
    private DefaultTimeBar progressBar;
    private MediaController mediaController;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlaybackService.PlayBackBinder binder = (PlaybackService.PlayBackBinder) iBinder;
            playbackService = binder.getService();
            isBound = true;

            ListenableFuture<MediaController> controllerFuture = playbackService.getControllerFuture();

            Futures.addCallback(controllerFuture, new FutureCallback<MediaController>() {
                @Override
                public void onSuccess(MediaController result) {
                    mediaController = result;
                    binding.videoPlayer.setPlayer(mediaController);
                    binding.videoPlayer.setControllerAutoShow(false);

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

                playbackService.setCallback(title -> runOnUiThread(() -> {
                    videoName.setText(title);
                    binding.videoPlayer.hideController();
                }));
            }

            videoAdapter = new VideoAdapter(VideoPlayerActivity.this, videoList, VideoPlayerActivity.this);
            binding.availableVideo.setAdapter(videoAdapter);
            updateProgressBar();
        }
    };
    private GestureDetector gestureDetector, gestureDetectorBrightness, gestureDetectorVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.player), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeUiComponents();

        videoList = getIntent().getParcelableArrayListExtra("videoList");
        String currentVideoPath = getIntent().getStringExtra("videoPath");

        //Service+Activity কানেক্ট
        Intent serviceIntent = new Intent(this, PlaybackService.class);
        serviceIntent.putExtra("videoPath", currentVideoPath);
        serviceIntent.putParcelableArrayListExtra("videoList", videoList);
        startService(serviceIntent);

        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        binding.videoPlayer.findViewById(R.id.backArrow).setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        binding.videoPlayer.findViewById(R.id.settingsButton).setOnClickListener(view -> {

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(VideoPlayerActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog);
            bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            bottomSheetDialog.setCancelable(true);
            bottomSheetDialog.setCanceledOnTouchOutside(true);

            bottomSheetLockscreen = bottomSheetDialog.findViewById(R.id.bottom_sheet_lock_screen);
            bottomSheetLockscreen.setOnClickListener(view2 -> {
                bottomSheetDialog.dismiss();
                if (!isFullScreen) {
                    fullscreenOn(VideoPlayerActivity.this, binding, fullScreenButton, isPortrait());
                    isFullScreen = true;
                }

                isVideoLocked = true;
                binding.videoPlayer.setUseController(false);

                binding.lockscreenButton.setVisibility(View.VISIBLE); // লক বাটন দেখাবে


                binding.videoPlayer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view2, MotionEvent motionEvent) {
                        if (!isVideoLocked) {
                            return false;
                        }
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            if (isLockScreenVisible) {
                                binding.lockscreenButton.setVisibility(View.GONE);
                            } else {
                                binding.lockscreenButton.setVisibility(View.VISIBLE);
                            }
                            isLockScreenVisible = !isLockScreenVisible;
                        }
                        return true;
                    }
                });
            });

            binding.lockscreenButton.setOnClickListener(view1 -> {
                isVideoLocked = false;
                binding.videoPlayer.setUseController(true);
                binding.lockscreenButton.setVisibility(View.GONE);
                isLockScreenVisible = false;

                Toast.makeText(VideoPlayerActivity.this, "আনলক হয়েছে", LENGTH_SHORT).show();
                binding.videoPlayer.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view1, MotionEvent motionEvent) {
                        return VideoPlayerHelper.handleTouchEvent(view1, motionEvent, gestureDetector, gestureDetectorVolume, gestureDetectorBrightness, isFullScreen, VideoPlayerActivity.this, binding, fullScreenButton, isPortrait());
                    }
                });
            });


            TextView quality = bottomSheetDialog.findViewById(R.id.bottom_sheet_quality);

            Format format = playbackService.getExoPlayer().getVideoFormat();
            quality.setText(VideoPlayerHelper.getVideoResolutionText(format));


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
        });


        binding.videoPlayer.findViewById(R.id.previousButton).setOnClickListener(view -> {
            if (mediaController != null) {
                mediaController.seekToPrevious();
            }
        });

        playPauseButton.setOnClickListener(view -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                    playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, null));
                } else {
                    mediaController.play();
                    playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
                }
            }
        });

        binding.videoPlayer.findViewById(R.id.nextButton).setOnClickListener(view -> {
            if (mediaController != null) {
                mediaController.seekToNextMediaItem();
                playPauseButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, null));
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

        binding.videoPlayer.findViewById(R.id.fullScreenButton).setOnClickListener(view -> {
            if (!isFullScreen) {
                fullscreenOn(VideoPlayerActivity.this, binding, fullScreenButton, isPortrait());
            } else {
                VideoPlayerHelper.fullscreenOff(VideoPlayerActivity.this, binding, fullScreenButton, binding.availableVideo);
                isFullScreen = false;
            }
        });


        gestureDetector = new GestureDetector(this, new SwipeGesture(this, binding.videoPlayer, binding.availableVideo));
        gestureDetectorBrightness = new GestureDetector(this, new BrightnessGesture(this, binding.videoPlayer, swipeControlLayout));
        gestureDetectorVolume = new GestureDetector(this, new VolumeGesture(this, binding.videoPlayer, swipeControlLayout));

        binding.videoPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return VideoPlayerHelper.handleTouchEvent(view, motionEvent, gestureDetector, gestureDetectorVolume, gestureDetectorBrightness, isFullScreen, VideoPlayerActivity.this, binding, fullScreenButton, isPortrait());
            }
        });


        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFullScreen) {
                    VideoPlayerHelper.fullscreenOff(VideoPlayerActivity.this, binding, fullScreenButton, binding.availableVideo);
                    isFullScreen = false;
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            VideoPlayerHelper.fullscreenOff(this, binding, fullScreenButton, binding.availableVideo);
            isFullScreen = false;
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
        videoName = binding.videoPlayer.findViewById(R.id.videoName);
        playPauseButton = binding.videoPlayer.findViewById(R.id.playPauseButton);
        videoDuration = binding.videoPlayer.findViewById(R.id.videoDuration);
        progressBar = binding.videoPlayer.findViewById(R.id.progressBar);
        fullScreenButton = binding.videoPlayer.findViewById(R.id.fullScreenButton);
        swipeControlLayout = findViewById(R.id.swipe_control_layout);

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
                handler.postDelayed(this, 100);
            }
        };
        handler.post(updateTask);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            ViewGroup.LayoutParams params = binding.videoPlayer.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            binding.videoPlayer.setLayoutParams(params);

        } else {
            binding.availableVideo.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = binding.videoPlayer.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._175sdp);
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            binding.videoPlayer.setLayoutParams(params);
        }
        binding.videoPlayer.hideController();
    }

    private Boolean isPortrait() {
        if (mediaController != null && mediaController.isPlaying()) {
            int height = Objects.requireNonNull(playbackService.getExoPlayer().getVideoFormat()).height;
            int width = playbackService.getExoPlayer().getVideoFormat().width;

            // Rotation চেক করা
            int rotation = playbackService.getExoPlayer().getVideoFormat().rotationDegrees;
            if (rotation == 90 || rotation == 270) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mediaController.isPlaying()) {
            VideoPlayerHelper.enterPictureInPicture(this, binding.videoPlayer, isPortrait());
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