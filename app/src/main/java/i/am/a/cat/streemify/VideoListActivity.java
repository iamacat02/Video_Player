package i.am.a.cat.streemify;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoListActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private RecyclerView recyclerVideoView;
    private ArrayList<Video> videoList;
    private VideoAdapter videoAdapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        recyclerVideoView = findViewById(R.id.recycleVideoView);
        TextView toolbarText = findViewById(R.id.toolbar_text);

        String folderPath = getIntent().getStringExtra("folderPath");

        if (folderPath != null) {
            toolbarText.setText(folderPath);
        }

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                videoList = getVideosFromFolder(folderPath);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoList != null) {
                            videoAdapter = new VideoAdapter(VideoListActivity.this, videoList, VideoListActivity.this);
                            recyclerVideoView.setAdapter(videoAdapter);
                        }
                    }
                });
            }
        });
    }

    private ArrayList<Video> getVideosFromFolder(String folderPath) {
        ArrayList<Video> videos = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RESOLUTION,
                MediaStore.Video.Media.SIZE
        };

        String selection;
        String[] selectionArgs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = MediaStore.Video.Media.RELATIVE_PATH + " LIKE ?";
            selectionArgs = new String[]{"%" + folderPath + "%"};
        } else {
            selection = MediaStore.Video.Media.DATA + " LIKE ?";
            selectionArgs = new String[]{"%" + folderPath + "%"};
        }

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long videoId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                    Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);

                    String videoName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                    long videoDuration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                    long videoDateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED));
                    String videoResolution = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION));
                    long videoSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

                    videos.add(new Video(videoUri.toString(), videoName, videoUri.toString(), videoDuration, videoDateAdded, videoResolution, videoSize));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videos;
    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onVideoClick(Video video) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoPath", video.getPath()); // ক্লিক করা ভিডিও পাথ পাঠান
        intent.putParcelableArrayListExtra("videoList", videoList); // ভিডিও লিস্ট পাঠান
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}