package i.am.a.cat.streemify.gesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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

public class BrightnessGesture extends GestureDetector.SimpleOnGestureListener {
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