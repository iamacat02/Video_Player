package i.am.a.cat.streemify.ui.screen;

import android.content.Intent;
import android.os.Bundle;
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

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.adapter.VideoAdapter;
import i.am.a.cat.streemify.data.Video;
import i.am.a.cat.streemify.databinding.ActivityVideoListBinding;
import i.am.a.cat.streemify.utils.VideoLoader;

public class VideoListActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener {

    private ActivityVideoListBinding binding;
    private ArrayList<Video> videoList;
    private VideoAdapter videoAdapter;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.appBarLayout.toolBar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        String folderPath = getIntent().getStringExtra("folderPath");

        if (folderPath != null) {
            binding.appBarLayout.toolbarText.setText(folderPath);
        }

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            videoList = VideoLoader.loadVideo(getApplicationContext(), folderPath);

            runOnUiThread(() -> {
                if (videoList != null) {
                    videoAdapter = new VideoAdapter(VideoListActivity.this, videoList, VideoListActivity.this);
                    binding.recycleVideoView.setAdapter(videoAdapter);
                }
            });
        });
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