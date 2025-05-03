package i.am.a.cat.streemify.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
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

import i.am.a.cat.streemify.R;

public class VolumeGesture extends GestureDetector.SimpleOnGestureListener {
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
