package i.am.a.cat.streemify.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeGesture extends GestureDetector.SimpleOnGestureListener {
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
                isScaling = true; // Zooming started

                recyclerView.setVisibility(View.GONE);
                DEFAULT_SCALE += 0.1f;
                // Clamp scale between MIN_SCALE and MAX_SCALE
                DEFAULT_SCALE = Math.max(MIN_SCALE, Math.min(DEFAULT_SCALE, MAX_SCALE));
                playerView.setScaleX(DEFAULT_SCALE);
                playerView.setScaleY(DEFAULT_SCALE);
                playerView.hideController();
            }
        }
        return isScrollingEnabled;
    }
}
