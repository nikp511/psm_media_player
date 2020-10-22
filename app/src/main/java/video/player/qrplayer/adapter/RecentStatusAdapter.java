package video.player.qrplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import video.player.qrplayer.R;
import video.player.qrplayer.activity.VideoPlayActivity;
import video.player.qrplayer.utils.Utils;

public class RecentStatusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int adCounter = 0;
    private final int width;
    FragmentActivity activity;
    ArrayList<String> whatsappStatusList;
    InterstitialAd mInterstitialAd;

    public RecentStatusAdapter(FragmentActivity activity, ArrayList<String> whatsappStatusList) {
        this.activity = activity;
        this.whatsappStatusList = whatsappStatusList;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        mInterstitialAd = new InterstitialAd(activity);
       // mInterstitialAd.setAdUnitId(activity.getResources().getString(R.string.Interstitial));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void addMedia(Context c, File f) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(f));
        c.sendBroadcast(intent);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.item_recent_status, parent, false);
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

        final String path = whatsappStatusList.get(position);
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
        String videoName = new File(whatsappStatusList.get(position)).getName();
        String folderName = Environment.getExternalStorageDirectory() + File.separator + Utils.FOLDER_NAME + "/";
        File directory = new File(folderName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        if (!FetchVideos().contains(videoName)) {
            holder.imgDownload.setImageResource(R.drawable.download_select);
        } else {
            holder.imgDownload.setImageResource(R.drawable.download_done);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.mainPlayList = whatsappStatusList;
                Utils.listSize = whatsappStatusList.size();
                Utils.playPosition = position;
                Intent in = new Intent(activity, VideoPlayActivity.class);
                in.putExtra("playid", "two");
                activity.startActivity(in);
                activity.finish();
            }
        });
        holder.imgDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videoName = new File(whatsappStatusList.get(position)).getName();
                String folderName = Environment.getExternalStorageDirectory() + File.separator + Utils.FOLDER_NAME + "/";
                File directory = new File(folderName);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                if (!FetchVideos().contains(videoName)) {
                    File from = new File(whatsappStatusList.get(position));
                    File to = new File(folderName + new File(whatsappStatusList.get(position)).getName());
                    Log.e("WhatsApp : ", from.toString());
                    Log.e("WhatsApp : ", to.toString());
                    try {
                        copyFile(from, to);
                        addMedia(activity, to);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(activity, "Video Status Saved", Toast.LENGTH_SHORT).show();
                    holder.imgDownload.setImageResource(R.drawable.download_done);
                    if (mInterstitialAd.isLoaded()) {
                        if (adCounter % 2 == 0) {
                            mInterstitialAd.show();
                        }
                        adCounter++;
                    } else {
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());
                    }
                } else {
                    Toast.makeText(activity, "Video has been already Downloaded", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private ArrayList<String> FetchVideos() {

        ArrayList<String> filenames = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory()
                + File.separator + Utils.FOLDER_NAME;

        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {

            String file_name = files[i].getName();
            // you can store name to arraylist and use it later
            filenames.add(file_name);
        }
        return filenames;
    }

    @Override
    public int getItemCount() {
        return whatsappStatusList.size();
    }

    class myclass extends RecyclerView.ViewHolder {
        ImageView imageView, imgShape;
        ImageView imgDownload;
        TextView txtDuration;


        public myclass(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgStatus);
            imgShape = itemView.findViewById(R.id.imgShape);
            imgDownload = itemView.findViewById(R.id.imgDownload);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            imageView.getLayoutParams().width = width / 2;
            imageView.getLayoutParams().height = (int) ((width / 2) * 1.5);
            imgShape.getLayoutParams().width = width / 2;
            imgShape.getLayoutParams().height = (int) ((width / 2) * 1.5);
        }
    }
}
