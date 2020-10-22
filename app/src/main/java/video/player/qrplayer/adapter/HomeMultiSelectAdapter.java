package video.player.qrplayer.adapter;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import video.player.qrplayer.activity.HomeActivity;
import video.player.qrplayer.R;
public class HomeMultiSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "HomeMultiSelectAdapter";
    public ArrayList<String> folderPath;
    public ArrayList<String> multiselectList;
    HomeActivity homeActivity;
    ArrayList<String> folderName;
    ArrayList<Integer> videoCount;
    ArrayList<String> folderThumb;

    int ITEM_TYPE = 0;


    public HomeMultiSelectAdapter(HomeActivity homeActivity, ArrayList<String> folderName, ArrayList<String> folderPath, ArrayList<Integer> videoCount, ArrayList<String> multiselectList, ArrayList<String> folderThumb) {
        this.folderName = folderName;
        this.folderPath = folderPath;
        this.videoCount = videoCount;
        this.folderThumb = folderThumb;
        this.homeActivity = homeActivity;
        this.multiselectList = multiselectList;

    }

    private static long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory()) {
                    result += dirSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }

    private static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public int getItemViewType(int position) {

        return ITEM_TYPE;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        if (viewType == ITEM_TYPE) {
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_folder_list, parent, false);
            return new ListViewHolder(itemView);
        } else {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListViewHolder) {
            ListViewHolder listViewHolder = (ListViewHolder) holder;
            // Glide.with(homeActivity).load(folderThumb.get(position)).into(listViewHolder.imgFolder);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.default_thumb);
            Glide.with(homeActivity).setDefaultRequestOptions(requestOptions).load(folderThumb.get(position))
                    .into(listViewHolder.imgFolder);

            ((ListViewHolder) holder).txtSize.setText(getFileSize(dirSize(new File(new File(folderThumb.get(position)).getParent()))));

            if (folderName.get(position).equals("0")) {
                listViewHolder.folderName.setText("Internal Storage");
            } else {
                listViewHolder.folderName.setText(folderName.get(position));
            }
//        listViewHolder.folderName.setText(folderName.get(position));
            listViewHolder.filesCount.setText(videoCount.get(position).toString() + " Videos");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (multiselectList.contains(folderPath.get(position))) {
//            listViewHolder.imgSelect.setVisibility(View.VISIBLE);
                    Drawable d = homeActivity.getResources().getDrawable(R.drawable.folder_shape);
                    d.setTint(homeActivity.getResources().getColor(R.color.test));
                    ((ListViewHolder) holder).imgshape.setImageDrawable(d);
                    listViewHolder.rlSelect.setBackgroundColor(homeActivity.getResources().getColor(R.color.test));
                } else {
//            listViewHolder..setVisibility(View.GONE);
                    Drawable d = homeActivity.getResources().getDrawable(R.drawable.folder_shape);
                    d.setTint(homeActivity.getResources().getColor(R.color.white));
                    ((ListViewHolder) holder).imgshape.setImageDrawable(d);
                    listViewHolder.rlSelect.setBackgroundColor(Color.parseColor("#00000000"));
                }
            } else {
                if (multiselectList.contains(folderPath.get(position))) {
                    listViewHolder.rlSelect.setBackgroundColor(homeActivity.getResources().getColor(R.color.test));
                } else {
                    listViewHolder.rlSelect.setBackgroundColor(Color.parseColor("#00000000"));
                }
            }

        }


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

        @BindView(R.id.txtSize)
        TextView txtSize;

        @BindView(R.id.cardimage)
        ImageView cardImage;

        @BindView(R.id.llselect)
        LinearLayout llselect;

        @BindView(R.id.rlselect)
        RelativeLayout rlSelect;

        @BindView(R.id.imgshape)
        ImageView imgshape;

        @BindView(R.id.imgFolder)
        ImageView imgFolder;

        public ListViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }


}

