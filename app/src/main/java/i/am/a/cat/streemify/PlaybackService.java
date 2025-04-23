package i.am.a.cat.streemify;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.session.MediaController;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class PlaybackService extends MediaSessionService {
    private final IBinder binder = new PlayBackBinder();
    private final Handler handler = new Handler();
    private ExoPlayer exoPlayer;
    private MediaSession mediaSession;
    private ListenableFuture<MediaController> controllerFuture;
    private DefaultRenderersFactory defaultRenderersFactory;
    private DefaultTrackSelector trackSelector;
    private ArrayList<Video> videoList;
    private int currentVideoIndex = 0;
    private Callback callback;
    private NotificationHelper notificationHelper;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // ভিডিও লিস্ট তৈরি করা
        videoList = new ArrayList<>();

        // ExoPlayer এবং MediaSession ইন্সট্যান্টিয়েট করা
        if (exoPlayer == null) {
            //হার্ডওয়্যার ডিকোডিং এনাবল
            defaultRenderersFactory = new DefaultRenderersFactory(this).setMediaCodecSelector(MediaCodecSelector.DEFAULT);

            //ভিডিও ট্রাক সিলেক্ট
            trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(trackSelector.buildUponParameters().setRendererDisabled(C.TRACK_TYPE_AUDIO, false));

            //Exoplayer ইনিশিয়ালাইজেশন
            exoPlayer = new ExoPlayer.Builder(this).setTrackSelector(trackSelector).setRenderersFactory(defaultRenderersFactory).setUseLazyPreparation(false).build();

            //মিডিয়া সেশান তৈরি করা
            mediaSession = new MediaSession.Builder(this, exoPlayer).setCallback(new MediaSession.Callback() {
                @NonNull
                @Override
                public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller, @NonNull List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
                    return MediaSession.Callback.super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs);
                }
            }).build();

            // MediaController তৈরি করা
            controllerFuture = new MediaController.Builder(this, mediaSession.getToken()).setApplicationLooper(Looper.getMainLooper()).buildAsync();

            // নোটিফিকেশন হেল্পার সেটআপ
            notificationHelper = new NotificationHelper(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // Intent থেকে ভিডিও লিস্ট আপডেট করা
        if (intent != null && intent.hasExtra("videoList")) {
            videoList = intent.getParcelableArrayListExtra("videoList");
        }

        // প্লেলিস্ট তৈরি করা (একাধিক মিডিয়া আইটেম যোগ করা)
        if (videoList != null && !videoList.isEmpty()) {
            List<MediaItem> mediaItems = new ArrayList<>(); // মিডিয়া আইটেম এর ArrayList তৈরি করা
            for (Video video : videoList) {
                mediaItems.add(new MediaItem.Builder().setUri(video.getPath()).setMediaId(video.getPath())  // মিডিয়া আইডি সেট করা
                        .setMediaMetadata(new MediaMetadata.Builder().setTitle(video.getName()).build()) // মিডিয়া মেটা ডাটা সেট করা
                        .build());
            }
            exoPlayer.setMediaItems(mediaItems); // সমস্ত ভিডিও একসাথে Playlist তৈরি করা
            exoPlayer.prepare();    // প্লে হওয়ার জন্য প্রস্তুত করা
        }

        // নির্দিষ্ট ভিডিও চালু করা (যদি দেওয়া থাকে)
        String videoPath = intent != null ? intent.getStringExtra("videoPath") : null;
        if (videoPath != null && !videoPath.isEmpty()) {
            currentVideoIndex = getVideoIndex(videoPath);
            exoPlayer.seekTo(currentVideoIndex, 0);  // প্লেলিস্টের নির্দিষ্ট পজিশনে যাওয়া
            exoPlayer.play();

            exoPlayer.addListener(new Player.Listener() {   // মিডিয়া আইটেম এর ভিডিও চেঞ্জ হলে ভিডিও টাইটেল আপডেট করা
                @Override
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    callback.onVideoTitleChanged(getCurrentVideoName());
                }
            });
        }

        notificationHelper.setCurrentVideoDetails(videoPath, videoList);
        notificationHelper.createMediaNotification(exoPlayer, mediaSession.getSessionCompatToken()); // নোটিফিকেশান তৈরি করা + সেশান টোকেন পাঠানো 

        // ফোরগ্রাউন্ড নোটিফিকেশন চালু করা
        startForeground(NotificationHelper.NOTIFICATION_ID, notificationHelper.getNotification());

        return START_STICKY;
    }


    // ভিডিও ইন্ডেক্স পাওয়া
    private int getVideoIndex(String videoPath) {
        for (int i = 0; i < videoList.size(); i++) {
            if (videoList.get(i).getPath().equals(videoPath)) {
                return i;
            }
        }
        return 0;
    }

    // বর্তমান ভিডিওর নাম
    protected String getCurrentVideoName() {
        MediaItem mediaItem = exoPlayer.getCurrentMediaItem();
        return mediaItem != null && mediaItem.mediaMetadata.title != null ? mediaItem.mediaMetadata.title.toString() : "Unknown";
    }

    //এক্সপ্লায়ের এর instance তৈরি করা
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    // 📌 বর্তমানে প্লেয়ারে থাকা অডিও ট্র্যাক লিস্ট রিটার্ন করবে
    public List<String> getAudioTrackList() {
        List<String> trackList = new ArrayList<>();
        Tracks tracks = exoPlayer.getCurrentTracks();
        for (int i = 0; i < tracks.getGroups().size(); i++) {
            Tracks.Group group = tracks.getGroups().get(i);
            if (group.getType() == C.TRACK_TYPE_AUDIO) {
                for (int j = 0; j < group.length; j++) {
                    Format format = group.getTrackFormat(j);
                    String lang = format.language != null ? format.language : "Unknown";
                    trackList.add("Track " + (j + 1) + " - " + lang);
                }
            }
        }
        return trackList;
    }

    // 📌 নির্দিষ্ট অডিও ট্র্যাক সিলেক্ট করা
    public void selectAudioTrack(int trackIndex) {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) return;

        for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            if (trackGroups.length > 0 && exoPlayer.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                DefaultTrackSelector.Parameters.Builder parametersBuilder = trackSelector.buildUponParameters();
                DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(0, trackIndex);
                parametersBuilder.setSelectionOverride(rendererIndex, trackGroups, override);
                trackSelector.setParameters(parametersBuilder.build()); // build() ব্যবহার করুন
                return;
            }
        }
    }

    // মিডিয়া কানেক্টর এর কন্ট্রোলার get করা
    public ListenableFuture<MediaController> getControllerFuture() {
        return controllerFuture;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {    // ExoPlayer বন্ধ করা
            exoPlayer.release();
            exoPlayer = null;
        }

        // Notification বন্ধ করা
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }

        // MediaSession বন্ধ করা
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        // Handler রিসেট করা
        handler.removeCallbacksAndMessages(null);
    }

    // Interface ভিডিও নাম activity তে পাঠানো
    public interface Callback {
        void onVideoTitleChanged(String title);
    }

    // Binder তৈরি করা
    public class PlayBackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}