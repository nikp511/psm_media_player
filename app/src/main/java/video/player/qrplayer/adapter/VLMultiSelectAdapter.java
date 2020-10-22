package video.player.qrplayer.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import video.player.qrplayer.R;
import video.player.qrplayer.activity.HomeActivity;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VLMultiSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "VLMultiSelectAdapter";
    private final int width;


    ArrayList<String> folderName;
    public ArrayList<String> folderPath;
    ArrayList<Integer> videoCount;
    public ArrayList<String> multiselectList;

    public VLMultiSelectAdapter(HomeActivity homeActivity, ArrayList<String> folderName, ArrayList<String> folderPath, ArrayList<Integer> videoCount, ArrayList<String> multiselectList) {
        this.folderName = folderName;
        this.folderPath = folderPath;
        this.videoCount = videoCount;
        this.multiselectList = multiselectList;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        homeActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder_list, parent, false);

        return new ListViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VLMultiSelectAdapter.ListViewHolder listViewHolder = (ListViewHolder) holder;
        listViewHolder.folderName.setText(folderName.get(position));
        listViewHolder.filesCount.setText(videoCount.get(position).toString() + " Videos");

        if (multiselectList.contains(folderPath.get(position)))
            listViewHolder.imgFolder.setImageResource(R.drawable.ic_checked);
        else
            listViewHolder.imgFolder.setImageResource(R.drawable.folder_icon_simple);

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

        /*@BindView(R.id.imgCheck)
        ImageView imgCheck;*/

        @BindView(R.id.imgFolder)
        ImageView imgFolder;

        public ListViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}

