package video.player.qrplayer.adapter;


import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import video.player.qrplayer.R;
import video.player.qrplayer.activity.HomeActivity;
import video.player.qrplayer.activity.VideoListActivity;

public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    HomeActivity HomeActivity;
    ArrayList<String> folderName;
    ArrayList<String> folderp;
    ArrayList<Integer> videoCount;
    int set = 4;
    int pos = 0;

    public HomeAdapter(HomeActivity HomeActivity, ArrayList<String> folderName, ArrayList<String> folderp, ArrayList<Integer> videoCount) {
        this.folderName = folderName;
        this.HomeActivity = HomeActivity;
        this.folderp = folderp;
        this.videoCount = videoCount;
    }

    @Override
    public int getItemViewType(int position) {
        return folderName.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View viewCategories = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder_list, parent, false);
        return new ListViewHolder(viewCategories);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        ListViewHolder listViewHolder = (ListViewHolder) holder;

        if (folderName.get(position).equals("0")) {
            listViewHolder.folderName.setText("Internal Storage");
        } else {
            Log.e("Internal :", "inter");
            listViewHolder.folderName.setText(folderName.get(position));
        }
        listViewHolder.filesCount.setText(videoCount.get(position).toString() + " Videos");
        listViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(HomeActivity, VideoListActivity.class);
                in.putExtra("foldername", folderp.get(position));
                HomeActivity.startActivity(in);
                HomeActivity.finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderName.size();
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.foldername)
        TextView folderName;

        @BindView(R.id.filescount)
        TextView filesCount;

        @BindView(R.id.cardimage)
        ImageView cardImage;

        public ListViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
