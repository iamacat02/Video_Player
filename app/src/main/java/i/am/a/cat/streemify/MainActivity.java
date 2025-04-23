package i.am.a.cat.streemify;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerViewFolder, recyclerViewVideo;
    private List<Folder> folderList;
    private List<Video> videoList;
    private FolderAdapter adapter;
    private VideoAdapter adapter2;
    private ActivityResultLauncher<Intent> writeSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerViewFolder = findViewById(R.id.recycleFolderList);
        recyclerViewVideo = findViewById(R.id.recycleVideoViewList);

        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        writeSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkAndRequestPermissions()
        );

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // WRITE_SETTINGS পারমিশন চেক
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            writeSettingsLauncher.launch(intent);
            return;
        }

        // READ_STORAGE & MEDIA_ACCESS পারমিশন চেক
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionX.init(this)
                    .permissions(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS)
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            loadFolder();
                            loadVideoFromFileApi();
                        } else {
                            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            PermissionX.init(this)
                    .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            loadFolder();
                            loadVideoFromFileApi();
                        } else {
                            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    private void loadFolder() {
        folderList = getFolderFromStorage();
        adapter = new FolderAdapter(this, folderList);
        recyclerViewFolder.setAdapter(adapter);
    }
    private void loadVideoFromFileApi() {
        videoList = new ArrayList<>();
        File directory = new File("/storage/emulated/0/");

        scanDirectoryForVideos(directory, videoList);

         adapter2 = new VideoAdapter(this, videoList, this::onVideoClick);
        recyclerViewVideo.setAdapter(adapter2);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void onVideoClick(Video video) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoPath", video.getPath()); // ক্লিক করা ভিডিও পাথ পাঠান
        intent.putParcelableArrayListExtra("videoList", (ArrayList<? extends Parcelable>) videoList); // ভিডিও লিস্ট পাঠান
        startActivity(intent);
    }

    private void scanDirectoryForVideos(File dir, List<Video> videos) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // শুধুমাত্র ফাইল যদি ভিডিও হয়, তা হলে অ্যাড করা হবে
                    if (!file.isDirectory() && isVideoFile(file)) {
                        videos.add(new Video(
                                file.getPath(),
                                file.getName(),
                                file.getAbsolutePath(),
                                0L,                                // duration অনুমান করা যাচ্ছে না File API থেকে
                                file.lastModified(),
                                "xxx:xxxx",                        // resolution File API থেকে পাওয়া যাবে না
                                file.length()
                        ));
                    }
                }
            }
        }
    }
    

    private boolean isVideoFile(File file) {
        String[] videoExtensions = {".mp4", ".mkv", ".webm", ".avi", ".3gp"};
        String fileName = file.getName().toLowerCase();
        for (String ext : videoExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }


    private List<Folder> getFolderFromStorage() {
        List<Folder> folders = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA};

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                HashMap<String, Integer> folderMap = new HashMap<>();

                // কার্সরের মধ্যে প্রতিটি ভিডিও এবং তার ফোল্ডার তথ্য বের করা
                while (cursor.moveToNext()) {
                    String folderName;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android Q বা তার পরে BUCKET_DISPLAY_NAME ব্যবহার করা
                        folderName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
                    } else {
                        // পুরনো ভার্সনের জন্য ফাইল পাথ থেকে ফোল্ডার নাম বের করা
                        String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        File file = new File(filePath);
                        folderName = file.getParentFile() != null ? file.getParentFile().getName() : "";
                    }

                    // ফোল্ডার নাম যাচাই করা এবং যোগ করা
                    if (folderName != null && !folderName.trim().isEmpty()) {
                        if (folderMap.containsKey(folderName)) {
                            folderMap.put(folderName, folderMap.get(folderName) + 1);
                        } else {
                            folderMap.put(folderName, 1);
                        }

                    }
                }

                // folderMap থেকে ফোল্ডার গুলি লোড করা
                for (Map.Entry<String, Integer> entry : folderMap.entrySet()) {
                    folders.add(new Folder(entry.getKey(), "", entry.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return folders;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}