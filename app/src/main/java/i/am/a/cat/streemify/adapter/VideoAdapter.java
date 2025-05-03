package i.am.a.cat.streemify.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.data.Video;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<Video> videoList;
    private final OnVideoClickListener listener;

    public VideoAdapter(Context context, List<Video> videoList, OnVideoClickListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.videoName.setText(video.getName());
        holder.videoDuration.setText(formatDuration(video.getDuration()));
        holder.dateAdded.setText(formatDate(video.getDateAdded()));
        holder.videoSize.setText(formatSize(video.getSize()));
        holder.videoResolution.setText(video.getResolution());

        Glide.with(context).load(video.getThumbnail()).into(holder.videoThumbnail);

        holder.itemView.setOnClickListener(v -> {
            listener.onVideoClick(video);
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    // Method to format the video duration from milliseconds to mm:ss format
    public String formatDuration(long durationMillis) {
        long minutes = (durationMillis / 1000) / 60;  // Get minutes
        long seconds = (durationMillis / 1000) % 60; // Get seconds

        // Format the string as mm:ss
        return String.format("%02d:%02d", minutes, seconds);
    }
    public String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Date date = new Date(timestamp * 1000); // Unix timestamp is in seconds, so multiply by 1000
        return sdf.format(date);
    }

    // Method to format the file size from bytes to KB/MB/GB
    public String formatSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";

        final long KB = 1024;
        final long MB = KB * 1024;
        final long GB = MB * 1024;

        if (sizeInBytes < KB) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < MB) {
            return String.format("%.1f KB", sizeInBytes / (float) KB);
        } else if (sizeInBytes < GB) {
            return String.format("%.1f MB", sizeInBytes / (float) MB);
        } else {
            return String.format("%.1f GB", sizeInBytes / (float) GB);
        }
    }

    // Define interface for click listener
    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView videoName, videoDuration, videoResolution, dateAdded, videoSize;
        ImageView videoThumbnail;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoName = itemView.findViewById(R.id.videoName);
            videoDuration = itemView.findViewById(R.id.videoDuration);
            videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
            dateAdded = itemView.findViewById(R.id.dateAdded);
            videoResolution = itemView.findViewById(R.id.videoResolution);
            videoSize = itemView.findViewById(R.id.videoSize);
        }
    }
}