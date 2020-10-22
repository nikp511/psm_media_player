package video.player.qrplayer.fragment;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import video.player.qrplayer.R;
import video.player.qrplayer.activity.VideoPlayActivity;
import video.player.qrplayer.utils.Utils;

public class SavedStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int width;
    ArrayList<String> statusList = new ArrayList<>();
    FragmentActivity activity;
    private String TAG = getClass().getSimpleName();

    public SavedStatusAdapter(FragmentActivity activity, ArrayList<String> statusList) {
        this.activity = activity;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        this.statusList = statusList;

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.item_saved_status, parent, false);
        myclass myclass = new myclass(view);
        return myclass;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        myclass holder = (myclass) viewHolder;
        if (position % 2 == 0) {
            holder.imgShape.setImageResource(R.drawable.left_shape);
        } else {
            holder.imgShape.setImageResource(R.drawable.right_shape);
        }

        final String path = statusList.get(position);
        Glide.with(activity).load(path).thumbnail(0.3f).into(holder.imageView);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        retriever.release();
        long seconds = Integer.parseInt(time);
        String vidLength = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds)),
                TimeUnit.MILLISECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds)));
        holder.txtDuration.setText(vidLength);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.playPosition = position;
                Utils.listSize = statusList.size();
                Utils.mainPlayList = statusList;
                Intent in = new Intent(activity, VideoPlayActivity.class);
                in.putExtra("playid", "two");
                activity.startActivity(in);
                activity.finish();
            }
        });
        holder.imgShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareAll(statusList.get(position));
            }
        });
    }

    void shareAll(String url) {
        Log.d(TAG, "myUrl : " + url);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        Uri screenshotUri = Uri.parse(url);
        sharingIntent.setType("image/png");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
        activity.startActivity(Intent.createChooser(sharingIntent, "Share image using"));
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    public class myclass extends RecyclerView.ViewHolder {
        ImageView imageView, imgShape;
        TextView txtDuration;
        ImageView imgShare;

        public myclass(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgStatus);
            imgShape = itemView.findViewById(R.id.imgShape);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            imgShare = itemView.findViewById(R.id.imgShare);
            imageView.getLayoutParams().width = width / 2;
            imageView.getLayoutParams().height = (int) ((width / 2) * 1.5);
            imgShape.getLayoutParams().width = width / 2;
            imgShape.getLayoutParams().height = (int) ((width / 2) * 1.5);
        }
    }
}

