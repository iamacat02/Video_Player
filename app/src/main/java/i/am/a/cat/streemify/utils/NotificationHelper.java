package i.am.a.cat.streemify.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerNotificationManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;

import i.am.a.cat.streemify.data.Video;
import i.am.a.cat.streemify.ui.screen.VideoPlayerActivity;

@UnstableApi
public class NotificationHelper {

    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "video_playback_channel";

    private final Context context;
    private final NotificationManager notificationManager;
    private Notification notification;
    private PlayerNotificationManager playerNotificationManager;
    private String currentVideoPath;
    private ArrayList<Video> currentVideoList;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    public void setCurrentVideoDetails(String path, ArrayList<Video> videoList) {
        this.currentVideoPath = path;
        this.currentVideoList = videoList;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Video Playback", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public void createMediaNotification(ExoPlayer player, MediaSessionCompat.Token token) {
        playerNotificationManager = new PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @NonNull
                    @Override
                    public CharSequence getCurrentContentTitle(@NonNull Player player) {
                        MediaItem mediaItem = player.getCurrentMediaItem();
                        return mediaItem != null && mediaItem.mediaMetadata != null && mediaItem.mediaMetadata.title != null
                                ? mediaItem.mediaMetadata.title
                                : "Playing Video";
                    }

                    @Nullable
                    @Override
                    public PendingIntent createCurrentContentIntent(@NonNull Player player) {
                        Intent intent = new Intent(context, VideoPlayerActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("videoPath", currentVideoPath);
                        intent.putParcelableArrayListExtra("videoList", currentVideoList);

                        return PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );
                    }

                    @NonNull
                    @Override
                    public CharSequence getCurrentContentText(@NonNull Player player) {
                        return "Playing";
                    }

                    @Nullable
                    @Override
                    public Bitmap getCurrentLargeIcon(@NonNull Player player, @NonNull PlayerNotificationManager.BitmapCallback callback) {
                        String imageUrl = player.getCurrentMediaItem() != null ? player.getCurrentMediaItem().mediaId : null;

                        if (imageUrl != null) {
                            Glide.with(context)
                                    .asBitmap()
                                    .load(imageUrl)
                                    .override(512, 512)
                                    .fitCenter()
                                    .into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            callback.onBitmap(resource);
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {
                                        }
                                    });
                        }

                        return null;
                    }
                })
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        if (dismissedByUser) {
                            cancelNotification();
                        }
                    }

                    @Override
                    public void onNotificationPosted(int notificationId, @NonNull Notification notification, boolean ongoing) {
                        NotificationHelper.this.notification = notification;
                    }
                })
                .build();

        playerNotificationManager.setPlayer(player);
        playerNotificationManager.setMediaSessionToken(token);
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public Notification getNotification() {
        if (notification == null) {
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        }
        return notification;
    }
}