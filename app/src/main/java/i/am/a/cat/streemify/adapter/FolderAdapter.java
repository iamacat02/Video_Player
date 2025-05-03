package i.am.a.cat.streemify.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import i.am.a.cat.streemify.R;
import i.am.a.cat.streemify.ui.screen.VideoListActivity;
import i.am.a.cat.streemify.data.Folder;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
    private final Context context;
    private final List<Folder> folderList;

    public FolderAdapter(Context context, List<Folder> folderList) {
        this.context = context;
        this.folderList = folderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.folder_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Folder folder = folderList.get(position);
        holder.folderImage.setImageResource(R.drawable.ic_folder);
        holder.folderName.setText(folder.getFolderName());
        holder.videoCount.setText(String.valueOf(folder.getVideoCount() + " Videos"));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, VideoListActivity.class);
                intent.putExtra("folderPath", folder.getFolderName());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView folderImage;
        TextView folderName, videoCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderImage = itemView.findViewById(R.id.folderImg);
            folderName = itemView.findViewById(R.id.folderName);
            videoCount = itemView.findViewById(R.id.videoCount);
        }
    }
}
