package i.am.a.cat.streemify;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

public class Gesture {

    public static class SwipeGesture extends GestureDetector.SimpleOnGestureListener {
        public static Boolean isScaling = false;

        private final Context context;

        private final PlayerView playerView;
        private final RecyclerView recyclerView;
        private final float MIN_SCALE = 0.5f;
        private final float MAX_SCALE = 1.4f;
        private float DEFAULT_SCALE = 1.0f;

        public SwipeGesture(Context context, PlayerView playerView, RecyclerView recyclerView) {

            this.context = context;
            this.playerView = playerView;
            this.recyclerView = recyclerView;
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean isScrollingEnabled = distanceY > 0;
            if (isScrollingEnabled) {
                if (Math.abs(distanceY) > Math.abs(distanceX)) {
                    isScaling = true; // স্কেলিং শুরু হয়েছে

                    recyclerView.setVisibility(View.GONE);
                    DEFAULT_SCALE += 0.1f;
                    // স্কেলের সীমা নির্ধারণ
                    DEFAULT_SCALE = Math.max(MIN_SCALE, Math.min(DEFAULT_SCALE, MAX_SCALE));
                    playerView.setScaleX(DEFAULT_SCALE);
                    playerView.setScaleY(DEFAULT_SCALE);
                    playerView.hideController();
                }
            }
            return isScrollingEnabled;
        }
    }

    public static class BrightnessGesture extends GestureDetector.SimpleOnGestureListener {
        private final Context context;
        private final PlayerView playerView;
        private final LinearLayout layout;
        private final ImageView imageView;
        private final ProgressBar progressBar;
        private final TextView textView;
        private final long swipeCooldown = 150;
        private long lastSwipeTime = 0;

        public BrightnessGesture(Context context, PlayerView playerView, LinearLayout layout) {
            this.context = context;
            this.playerView = playerView;
            this.layout = layout;
            this.imageView = layout.findViewById(R.id.swipe_control_img);
            this.progressBar = layout.findViewById(R.id.swipe_control_progressBar);
            this.textView = layout.findViewById(R.id.swipe_control_text);

            progressBar.setMax(100);  // Use 0-100 scale for percentage

            try {
                // Get current brightness and set progress
                int currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                int brightnessPercentage = Math.round((currentBrightness / 255f) * 100);
                progressBar.setProgress(brightnessPercentage);
                textView.setText(brightnessPercentage + "%");
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        }

        private Handler fadeOutHandler = new Handler(Looper.getMainLooper());
        private Runnable fadeOutRunnable;

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) > Math.abs(distanceX) && Math.abs(distanceY) > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSwipeTime < swipeCooldown) return false;
                lastSwipeTime = currentTime;

                try {
                    ContentResolver resolver = context.getContentResolver();

                    // Ensure manual brightness mode
                    if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                            == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    }

                    int currentBrightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
                    int currentPercentage = Math.round((currentBrightness / 255f) * 100);

                    // Change brightness by 5% step
                    int step = 5;
                    int newPercentage = distanceY > 0 ?
                            Math.min(100, currentPercentage + step) :
                            Math.max(0, currentPercentage - step);

                    int newBrightness = Math.round((newPercentage / 100f) * 255);
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness);

                    // UI Update
                    layout.setVisibility(View.VISIBLE);
                    layout.setAlpha(1f); // Reset opacity if previously faded

                    progressBar.setProgress(newPercentage);
                    textView.setText(newPercentage + "%");
                    imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_brightness));
                    playerView.setControllerAutoShow(false);

                    // Cancel any previous fade out
                    if (fadeOutHandler != null && fadeOutRunnable != null) {
                        fadeOutHandler.removeCallbacks(fadeOutRunnable);
                    }

                    // Prepare new fade out
                    fadeOutRunnable = () -> {
                        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(layout, "alpha", 1f, 0f);
                        fadeOut.setDuration(400); // Fade duration

                        fadeOut.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                layout.setVisibility(View.GONE);
                                layout.setAlpha(1f); // Reset for next use
                            }
                        });

                        fadeOut.start();
                    };

                    // Post new fade out with 1500ms delay
                    fadeOutHandler.postDelayed(fadeOutRunnable, 1500);

                    return true;

                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }


    public static class VolumeGesture extends GestureDetector.SimpleOnGestureListener {
        private static final long VOLUME_CHANGE_INTERVAL_MS = 100; // 100ms delay between changes
        private final Context context;
        private final AudioManager audioManager;
        private final int maxVolume;
        private final PlayerView playerView;
        private final ImageView imageView;
        private final ProgressBar progressBar;
        private final TextView textView;
        private final LinearLayout layout;
        private final int screenHeight;
        private int currentVolume;
        private long lastVolumeChangeTime = 0;
        private final Handler fadeOutHandler = new Handler(Looper.getMainLooper());
        private Runnable fadeOutRunnable;

        // Constructor
        public VolumeGesture(Context context, PlayerView playerView, LinearLayout layout) {
            this.context = context;
            this.playerView = playerView;
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            this.currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            this.layout = layout;
            this.imageView = layout.findViewById(R.id.swipe_control_img);
            this.progressBar = layout.findViewById(R.id.swipe_control_progressBar);
            this.textView = layout.findViewById(R.id.swipe_control_text);
            this.screenHeight = playerView.getHeight();

            // Initialize UI
            progressBar.setMax(maxVolume);
            progressBar.setProgress(currentVolume);
            imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sound));
            updateText(currentVolume);
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) > Math.abs(distanceX) && Math.abs(distanceY) > 0) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastVolumeChangeTime < VOLUME_CHANGE_INTERVAL_MS) {
                    return false; // Delay not passed, skip update
                }

                // Determine direction: Scroll up (distanceY > 0) = increase, Scroll down (distanceY < 0) = decrease
                int step = 1; // Volume step per scroll
                int newVolume;
                if (distanceY > 0) {
                    newVolume = currentVolume + step;
                } else {
                    newVolume = currentVolume - step;
                }

                newVolume = Math.max(0, Math.min(maxVolume, newVolume));

                if (newVolume != currentVolume) {
                    lastVolumeChangeTime = currentTime;

                    currentVolume = newVolume;
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_SHOW_UI);

                    // Update UI
                    progressBar.setProgress(currentVolume);
                    updateText(currentVolume);

                    // Show layout if not visible
                    if (layout.getVisibility() != View.VISIBLE) {
                        layout.setVisibility(View.VISIBLE);
                        layout.setAlpha(1f); // Reset alpha if faded
                        progressBar.setProgress(currentVolume);
                    }

                    imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sound));
                    playerView.setControllerAutoShow(false);

                    // Cancel any previous fade out
                    if (fadeOutHandler != null && fadeOutRunnable != null) {
                        fadeOutHandler.removeCallbacks(fadeOutRunnable);
                    }

                    // Schedule fade out
                    fadeOutRunnable = () -> {
                        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(layout, "alpha", 1f, 0f);
                        fadeOut.setDuration(400);
                        fadeOut.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                layout.setVisibility(View.GONE);
                            }
                        });
                        fadeOut.start();
                    };
                    fadeOutHandler.postDelayed(fadeOutRunnable, 1500); // 1.5 seconds

                    return true;
                }
            }
            return false;
        }


        private void updateText(int volume) {
            int volumePercentage = (int) ((volume / (float) maxVolume) * 100);
            textView.setText(volumePercentage + "%");
        }
    }

}