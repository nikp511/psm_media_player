package video.player.qrplayer.adapter;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import video.player.qrplayer.R;
import video.player.qrplayer.activity.VideoListActivity;
import video.player.qrplayer.activity.VideoPlayActivity;
import video.player.qrplayer.utils.Utils;

public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    public static String TAG = VideoListAdapter.class.getSimpleName();
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    VideoListActivity videoListActivity;
    String folder;
    ArrayList<String> vidname;
    ArrayList<String> mFilteredList;

    public VideoListAdapter(VideoListActivity videoListActivity, String folder, ArrayList<String> vidname) {
        this.videoListActivity = videoListActivity;
        this.folder = folder;
        this.vidname = vidname;
        mFilteredList = vidname;

    }

    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public int getItemViewType(int position) {
        return vidname.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View viewCategories = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_list, parent, false);
        return new MainViewHolder(viewCategories);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        MainViewHolder mainViewHolder = (MainViewHolder) holder;
        Utils.mainPlayList = mFilteredList;
        mainViewHolder.vname.setText(new File(mFilteredList.get(position)).getName());
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.default_thumb);
        Glide.with(videoListActivity).setDefaultRequestOptions(requestOptions).load(mFilteredList.get(position))
                .into(mainViewHolder.vthumb);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mFilteredList.get(position));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release();
        long seconds = Long.parseLong(time);
        String vidLength = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds)),
                TimeUnit.MILLISECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds)));

        File f = new File(mFilteredList.get(position));
        long length = f.length();

        ((MainViewHolder) holder).txtDuration.setText(vidLength);
        ((MainViewHolder) holder).txtSize.setText(getFileSize(length));

        mainViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "You Click Here : " + position);
                Utils.playPosition = position;
                Intent in = new Intent(videoListActivity, VideoPlayActivity.class);
                in.putExtra("playid", "one");
                videoListActivity.startActivity(in);
                videoListActivity.finish();
            }
        });

    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                String charString = charSequence.toString();

                if (charString.isEmpty()) {

                    mFilteredList = vidname;
                } else {

                    ArrayList<String> filteredList = new ArrayList<>();

                    for (String androidVersion : vidname) {

                        if (androidVersion.toLowerCase().contains(charString) || androidVersion.toLowerCase().contains(charString) || androidVersion.toLowerCase().contains(charString)) {

                            filteredList.add(androidVersion);
                        }
                    }

                    mFilteredList = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredList = (ArrayList<String>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemCount() {
        Utils.listSize = mFilteredList.size();
        return mFilteredList.size();
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.custimage)
        ImageView vthumb;

        @BindView(R.id.custvname)
        TextView vname;

        @BindView(R.id.txtDuration)
        TextView txtDuration;

        @BindView(R.id.txtSize)
        TextView txtSize;

        public MainViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
