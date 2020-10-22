package video.player.qrplayer.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import video.player.qrplayer.R;
import video.player.qrplayer.activity.AllVideoActivity;
import video.player.qrplayer.utils.Utils;

public class AllVideoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final String TAG = "AllVideoAdapter";
    public ArrayList<String> videoList;
    public ArrayList<String> multiselectList;
    public ArrayList<String> mFilteredList;
    AllVideoActivity homeActivity;
    int ITEM_TYPE = 0;


    public AllVideoAdapter(AllVideoActivity homeActivity, ArrayList<String> videoList, ArrayList<String> multiselectList) {
        this.videoList = videoList;
        mFilteredList = videoList;
        this.homeActivity = homeActivity;
        this.multiselectList = multiselectList;

    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        if (viewType == ITEM_TYPE) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_allvideo_list, parent, false);
            return new ListViewHolder(itemView);
        } else {
            return null;
        }
    }

    @Override
    public int getItemViewType(int position) {

            return ITEM_TYPE;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListViewHolder) {
            ListViewHolder listViewHolder = (ListViewHolder) holder;
            // Glide.with(homeActivity).load(folderThumb.get(position)).into(listViewHolder.imgFolder);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.default_thumb);
            Glide.with(homeActivity).setDefaultRequestOptions(requestOptions).load(mFilteredList.get(position))
                    .into(listViewHolder.imgFolder);


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

            ((ListViewHolder) holder).txtDuration.setText(vidLength);
            ((ListViewHolder) holder).txtSize.setText(getFileSize(length));

            if (new File(mFilteredList.get(position)).getName().equals("0")) {
                listViewHolder.folderName.setText("Internal Storage");
            } else {
                listViewHolder.folderName.setText(new File(mFilteredList.get(position)).getName());
            }
//        listViewHolder.folderName.setText(folderName.get(position));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (multiselectList.contains(mFilteredList.get(position))) {
                    Drawable d = homeActivity.getResources().getDrawable(R.drawable.folder_shape);
                    d.setTint(homeActivity.getResources().getColor(R.color.test));
                    ((ListViewHolder) holder).imgshape.setImageDrawable(d);
                    listViewHolder.rlSelect.setBackgroundColor(homeActivity.getResources().getColor(R.color.test));
                } else {
                    Drawable d = homeActivity.getResources().getDrawable(R.drawable.folder_shape);
                    d.setTint(homeActivity.getResources().getColor(R.color.white));
                    ((ListViewHolder) holder).imgshape.setImageDrawable(d);
                    listViewHolder.rlSelect.setBackgroundColor(Color.parseColor("#00000000"));
                }
            } else {
                if (multiselectList.contains(mFilteredList.get(position))) {
                    listViewHolder.rlSelect.setBackgroundColor(homeActivity.getResources().getColor(R.color.test));
                } else {
                    listViewHolder.rlSelect.setBackgroundColor(Color.parseColor("#00000000"));
                }
            }

        } else if (holder instanceof BannerAdHolder) {
            BannerAdHolder bannerAdHolder = (BannerAdHolder) holder;
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdHolder.adView.loadAd(adRequest);
        }
    }

    @Override
    public int getItemCount() {
        Utils.listSize = mFilteredList.size();
        return mFilteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                String charString = charSequence.toString();

                if (charString.isEmpty()) {

                    mFilteredList = videoList;
                } else {

                    ArrayList<String> filteredList = new ArrayList<>();

                    for (String androidVersion : videoList) {

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

    public static class BannerAdHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.adView)
        AdView adView;

        public BannerAdHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.foldername)
        TextView folderName;

        @BindView(R.id.txtDuration)
        TextView txtDuration;

        @BindView(R.id.rlselect)
        RelativeLayout rlSelect;

        @BindView(R.id.imgFolder)
        ImageView imgFolder;

        @BindView(R.id.imgshape)
        ImageView imgshape;

        @BindView(R.id.txtSize)
        TextView txtSize;

        public ListViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}