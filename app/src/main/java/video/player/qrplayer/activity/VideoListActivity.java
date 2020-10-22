package video.player.qrplayer.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.clockbyte.admobadapter.bannerads.AdmobBannerRecyclerAdapterWrapper;
import com.clockbyte.admobadapter.bannerads.BannerAdViewWrappingStrategyBase;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import video.player.qrplayer.R;
import video.player.qrplayer.adapter.VideoListAdapter;

//import androidx.core.widget.SwipeRefreshLayout;

public class VideoListActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();
    public static String folder;
    public static ArrayList<String> vidname;
    public static ConnectivityManager cm;
    public static Context context;
    static VideoListAdapter listAdapter;
    RecyclerView rviewVideoList;
    LinearLayoutManager layoutManager;


    SwipeRefreshLayout mSwipeRefreshLayout;
    ImageView imgBack;
    TextView txtTitle;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        final Intent in = getIntent();
        folder = in.getStringExtra("foldername");
        Log.d("fname1", folder);
        vidname = new ArrayList<>();
        mSwipeRefreshLayout = findViewById(R.id.swipe);


        imgBack = findViewById(R.id.imgBack);
        txtTitle = findViewById(R.id.txtTitle);
        if (new File(folder).getName().equals("0")) {
            txtTitle.setText("Internal Storage");
        } else {
            txtTitle.setText(new File(folder).getName());
        }
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VideoListActivity.this, HomeActivity.class);
                intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                finish();
            }
        });

        context = this;

        getdata();
        listAdapter = new VideoListAdapter(VideoListActivity.this, folder, vidname);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                vidname.clear();
                getdata();
                listAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright),
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_orange_light),
                getResources().getColor(android.R.color.holo_red_light),
                getResources().getColor(android.R.color.black)
        );

        AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(this)
                .setLimitOfAds(10)
                .setFirstAdIndex(2)
                .setSingleAdSize(AdSize.LARGE_BANNER)
                .setNoOfDataBetweenAds(5)
                .setAdapter(listAdapter)
                .setSingleAdUnitId(getString(R.string.adUnitID))
                .setAdViewWrappingStrategy(new BannerAdViewWrappingStrategyBase() {
                    @NonNull
                    @Override
                    protected ViewGroup getAdViewWrapper(ViewGroup parent) {
                        return (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_ad,
                                parent, false);
                    }

                    @Override
                    protected void recycleAdViewWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                       // ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                       // for (int i = 0; i < container.getChildCount(); i++) {
                         //   View v = container.getChildAt(i);
                           // if (v instanceof AdView) {
                             //   container.removeViewAt(i);
                               // break;
                            //}
                        //}
                    }

                    @Override
                    protected void addAdViewToWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        container.addView(ad);
                    }
                })
                .build();


        Log.e(TAG , "Here");
        if (isNetworkConnected()) {
            rviewVideoList.setAdapter(adapterWrapper);
        } else {
            rviewVideoList.setAdapter(listAdapter);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        vidname.clear();
        getdata();
        listAdapter.notifyDataSetChanged();
    }

    void getdata() {
        String[] proj = new String[]{
                MediaStore.Video.Media.DATA
        };
        cursor = this.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);

        while (cursor.moveToNext()) {
            int ind = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
            String path = cursor.getString(ind);
            Log.d("vidpath", path);
            if (isFileExits(path)) {
                if (folder.equals(new File(path).getParent())) {
                    vidname.add(path);
                }
            }

        }
        Log.v("pathmain", String.valueOf(vidname));
        layoutManager = new LinearLayoutManager(this);
        rviewVideoList = (RecyclerView) findViewById(R.id.recycle);
        rviewVideoList.setLayoutManager(layoutManager);
        Collections.sort(vidname);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private boolean isFileExits(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(VideoListActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
