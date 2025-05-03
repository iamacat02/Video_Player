package i.am.a.cat.streemify.utils;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.OptIn;
import androidx.core.content.res.ResourcesCompat;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.databinding.ActivityVideoPlayerBinding;
import i.am.a.cat.streemify.gesture.SwipeGesture;
import i.am.a.cat.streemify.ui.screen.VideoPlayerActivity;

public class VideoPlayerHelper {

    public static void fullscreenOn(Activity activity, ActivityVideoPlayerBinding binding, View fullScreenButton, boolean isPortrait) {
        if (isPortrait) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        ViewGroup.LayoutParams params = binding.videoPlayer.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        binding.videoPlayer.setLayoutParams(params);

        fullScreenButton.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_fullscreen_exit, null));

        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public static void fullscreenOff(Activity activity, ActivityVideoPlayerBinding binding, View fullScreenButton, View availableVideo) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ViewGroup.LayoutParams params = binding.videoPlayer.getLayoutParams();
        params.height = activity.getResources().getDimensionPixelSize(com.intuit.sdp.R.dimen._175sdp);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        binding.videoPlayer.setLayoutParams(params);

        availableVideo.setVisibility(View.VISIBLE);

        fullScreenButton.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_fullscreen_enter, null));

        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void enterPictureInPicture(Activity activity, PlayerView playerView, boolean isPortrait) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();

            Rational aspectRatio = isPortrait ? new Rational(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) : new Rational(16, 9);

            pipBuilder.setAspectRatio(aspectRatio);
            activity.enterPictureInPictureMode(pipBuilder.build());

            playerView.setControllerAutoShow(false);
        }
    }

    public static String getVideoResolutionText(Format format) {
        int height = format.height;

        if (height >= 2160) return "4K (2160p)";
        if (height >= 1440) return "2K (1440p)";
        if (height >= 1080) return "Full HD (1080p)";
        if (height >= 720) return "HD (720p)";
        if (height >= 480) return "SD (480p)";
        if (height >= 360) return "360p";
        if (height >= 240) return "240p";
        return "Low Resolution";
    }

    @OptIn(markerClass = UnstableApi.class)
    public static boolean handleTouchEvent(View view, MotionEvent motionEvent, GestureDetector gestureDetector, GestureDetector gestureDetectorVolume, GestureDetector gestureDetectorBrightness, boolean isFullScreen, Activity activity, ActivityVideoPlayerBinding binding, View fullScreenButton, boolean isPortrait) {

        int screenWidth = view.getWidth();
        float touchX = motionEvent.getX();
        if (SwipeGesture.isScaling) {
            SwipeGesture.isScaling = false;
            binding.videoPlayer.setScaleX(1.0f);
            binding.videoPlayer.setScaleY(1.0f);
            fullscreenOn(activity, binding, fullScreenButton, isPortrait);
            VideoPlayerActivity.isFullScreen = true;
            return true;
        }

        if (isFullScreen) {
            if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                if (touchX >= (screenWidth * 0.75)) {
                    return gestureDetectorVolume.onTouchEvent(motionEvent);
                } else if (touchX <= (screenWidth * 0.25)) {
                    return gestureDetectorBrightness.onTouchEvent(motionEvent);
                } else {
                    return false;
                }
            }

            boolean swipeDetected = gestureDetector.onTouchEvent(motionEvent);
            boolean brightnessDetected = gestureDetectorBrightness.onTouchEvent(motionEvent);
            boolean volumeDetected = gestureDetectorVolume.onTouchEvent(motionEvent);

            return swipeDetected || brightnessDetected || volumeDetected;
        }

        return gestureDetector.onTouchEvent(motionEvent);
    }

}