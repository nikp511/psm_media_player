package video.player.qrplayer.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;

import video.player.qrplayer.BuildConfig;
import video.player.qrplayer.R;
import video.player.qrplayer.adapter.HomeMultiSelectAdapter;
import video.player.qrplayer.multiselect.AlertDialogHelper;
import video.player.qrplayer.multiselect.RecyclerItemClickListener;
import video.player.qrplayer.services.BackgroundSoundService;
import video.player.qrplayer.utils.Utils;

//import androidx.multidex.BuildConfig;


@SuppressLint("RestrictedApi")
public class HomeActivity extends AppCompatActivity implements AlertDialogHelper.AlertDialogListener {
    public static final boolean DEBUG = !BuildConfig.BUILD_TYPE.equals("release");
    public static String folder;
    //public static InterstitialAd mInterstitialAd;
    public static ArrayList<String> videoList = new ArrayList<>();
    public static int adCounter = 0;
    RecyclerView RviewFolders;
    LinearLayoutManager layoutManager;
    ArrayList<String> folderPath;
    ArrayList<String> folderName;
    ArrayList<String> allfolderpath = new ArrayList<>();
    ArrayList<Integer> videoCount = new ArrayList<Integer>();
    ArrayList<String> multiselectList = new ArrayList<>();
    ArrayList<String> folderp;
    String[] allvidFile;
    SharedPreferences preferences;
    FloatingActionButton fab;
    String vidurl;
    SwipeRefreshLayout mSwipeRefreshLayout;
    CardView cardView, cardView1;
    boolean isMultiSelect = false;
    boolean isToolVisible = false;
    ActionMode mActionMode;
    AlertDialogHelper alertDialogHelper;
    HomeMultiSelectAdapter homeMultiSelectAdapter;
    LinearLayout toolLinear, toolLL;
    ImageView imgBack, imgSelect, imgPlay, imgRename, imgDelete, imgRefresh;
    Animation animation;
    TextView txtTitle;
    ArrayList<String> imageList = new ArrayList<>();
    ArrayList<String> folderThumb = new ArrayList<>();
    private Cursor csr;
    private String TAG = HomeActivity.class.getSimpleName();
    private long exitTime;

    private static void removeMedia(Context c, File f) {
        ContentResolver resolver = c.getContentResolver();
        resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + "=?", new String[]{f.getAbsolutePath()});
    }

    public static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
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

    public void refreshAdapter() {
        homeMultiSelectAdapter.multiselectList = multiselectList;
        homeMultiSelectAdapter.folderPath = folderPath;
        homeMultiSelectAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_home);

        if (getIntent() != null) {
            Log.e(TAG, "check  :  " + getIntent().getStringExtra("key"));
        } else {
            Log.e(TAG, "false  :  ");
        }

        AdView mAdView = findViewById(R.id.adview);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //Utils.mainPlayList = new ArrayList();
        //mInterstitialAd = new InterstitialAd(this);
        //mInterstitialAd.setAdUnitId(getResources().getString(R.string.Interstitial));
        //mInterstitialAd.loadAd(new AdRequest.Builder().build());
        //mInterstitialAd.setAdListener(new AdListener() {
          //  @Override
            //public void onAdClosed() {
                //mInterstitialAd.loadAd(new AdRequest.Builder().build());
            //}
        //});

      /*  if (!Utils.isAd) {
            if (SplashScreenActivity.mInterstitialAd.isLoaded()) {
                SplashScreenActivity.mInterstitialAd.show();
                Utils.isAd = true;
            }
        }*/

        alertDialogHelper = new AlertDialogHelper(HomeActivity.this);
        toolLL = findViewById(R.id.toolLL);
        toolLinear = findViewById(R.id.toolLinear);
        imgBack = findViewById(R.id.imgBack);
        imgSelect = findViewById(R.id.imgSelect);
        imgPlay = findViewById(R.id.imgPlay);
        imgRename = findViewById(R.id.imgRename);
        imgDelete = findViewById(R.id.imgDelete);
        imgRefresh = findViewById(R.id.imgRefresh);

        txtTitle = findViewById(R.id.txtTitle);
        mSwipeRefreshLayout = findViewById(R.id.swipe);
        RviewFolders = findViewById(R.id.recycle);
        cardView = findViewById(R.id.card_view);
        cardView1 = findViewById(R.id.card_view1);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        RviewFolders.setLayoutManager(layoutManager);
        folderPath = new ArrayList<>();
        folderName = new ArrayList<>();
        folderp = new ArrayList<>();
        preferences = getSharedPreferences("my", MODE_PRIVATE);
        fab = findViewById(R.id.fab);
        vidurl = preferences.getString("videourl", "nourl");
        if (!vidurl.equals("nourl"))
            folder = new File(vidurl).getParent();

        getdata();

        if (vidurl.equals("nourl")) {
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AllVideoActivity.class);
                intent.putExtra("video", false);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
        cardView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* if (mInterstitialAd.isLoaded() && adCounter % 4 == 0) {
                   // mInterstitialAd.show();
                } else {*/
                //mInterstitialAd.loadAd(new AdRequest.Builder().build());
                startActivity(new Intent(HomeActivity.this, WhatsappStatusActivity.class));
                finish();
                overridePendingTransition(R.anim.enter, R.anim.exit);
              /*  }
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        //mInterstitialAd.loadAd(new AdRequest.Builder().build());
                        startActivity(new Intent(HomeActivity.this, WhatsappStatusActivity.class));
                        finish();
                        overridePendingTransition(R.anim.enter, R.anim.exit);
                    }*/
            }
                });
               // adCounter++;
            //}
       // });
        animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageBack();
            }
        });
        imgRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgRefresh.startAnimation(animation);
                /*folderName.clear();
                folderPath.clear();
                videoCount.clear();
                allfolderpath.clear();
                getdata();*/
                homeMultiSelectAdapter.notifyDataSetChanged();
            }
        });
        imgSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                all_select();
            }
        });
        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.isWhatsapp = 2;
                videoList = new ArrayList<>();
                if (multiselectList.size() > 0) {
                    for (int i = 0; i < multiselectList.size(); i++) {
                        FetchAndPlay(multiselectList.get(i));
                    }
                    Utils.mainPlayList = videoList;
                    Utils.listSize = videoList.size();
                    Utils.playPosition = 0;
                    Intent in = new Intent(HomeActivity.this, VideoPlayActivity.class);
                    in.putExtra("playid", "three");
                    startActivity(in);
                    finish();
                }
            }
        });
        imgRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(HomeActivity.this, multiselectList.get(0));
            }
        });
        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogHelper.showAlertDialog("", "Do you want to Delete Folder ?", "DELETE", "CANCEL", 1, false);
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Utils.mainPlayList = imageList;
                int pos = Utils.mainPlayList.indexOf(vidurl);
                Utils.playPosition = pos;
                Utils.listSize = imageList.size();

                Intent in = new Intent(HomeActivity.this, VideoPlayActivity.class);
                in.putExtra("playid", "three");
                startActivity(in);
                finish();

                Log.d("videolist", String.valueOf(Utils.mainPlayList));

            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                /*folderName.clear();
                folderPath.clear();
                videoCount.clear();
                allfolderpath.clear();
                getdata();
                //adapter.notifyDataSetChanged();*/
                homeMultiSelectAdapter.notifyDataSetChanged();
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


        homeMultiSelectAdapter = new HomeMultiSelectAdapter(this, folderName, folderPath, videoCount, multiselectList, folderThumb);
        //adapter = new HomeAdapter(this, folderName, folderPath, videoCount);

        Log.d("Count :", String.valueOf(homeMultiSelectAdapter.getItemCount()));
        RviewFolders.addOnItemTouchListener(new RecyclerItemClickListener(this, RviewFolders, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
               /* if (isMultiSelect)
                    multi_select(position);
                else {
                    if (!Utils.isAd) {
                        //if (mInterstitialAd.isLoaded()) {
                           // mInterstitialAd.show();
                           // Utils.isAd = true;
                        } else {
               */             //mInterstitialAd.loadAd(new AdRequest.Builder().build());
                            Intent in = new Intent(HomeActivity.this, AllVideoActivity.class);
                            in.putExtra("video", true);
                            in.putExtra("foldername", folderPath.get(position));
                            startActivity(in);
                            finish();
                            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                            Log.d(TAG, "FolderPath : " + folderPath.get(position));
                       // }
                       // mInterstitialAd.setAdListener(new AdListener() {
                         //   @Override
                           // public void onAdClosed() {
                               // mInterstitialAd.loadAd(new AdRequest.Builder().build());
                              /*  Intent in = new Intent(HomeActivity.this, AllVideoActivity.class);
                                in.putExtra("video", true);
                                in.putExtra("foldername", folderPath.get(position));
                                startActivity(in);
                                finish();
                                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                                Log.d(TAG, "FolderPath : " + folderPath.get(position));
                            }*/
                       // });
                   // } else {
                      /*  Intent in = new Intent(HomeActivity.this, AllVideoActivity.class);
                        in.putExtra("video", true);
                        in.putExtra("foldername", folderPath.get(position));
                        startActivity(in);
                        finish();
                        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                        Log.d(TAG, "FolderPath : " + folderPath.get(position));*/
                    }
               // }

          //  }

            @Override
            public void onItemLongClick(View view, int position) {
                if (!isMultiSelect) {
                    multiselectList = new ArrayList<>();
                    isMultiSelect = true;

                    if (!isToolVisible) {
                        isToolVisible = true;

                        toolLinear.setVisibility(View.VISIBLE);
                        toolLL.setBackgroundColor(getResources().getColor(R.color.textColor));
                        imgBack.setVisibility(View.VISIBLE);
                        fab.setVisibility(View.GONE);
                        txtTitle.setPadding(0, 0, 0, 0);
                        refreshAdapter();
                    }
                }
                multi_select(position);
            }
        }));

      /*  AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(this)
                .setLimitOfAds(10)
                .setFirstAdIndex(2)
                //.setSingleAdSize(AdSize.LARGE_BANNER)
                .setNoOfDataBetweenAds(5)
                .setAdapter(homeMultiSelectAdapter)
                //.setSingleAdUnitId(getString(R.string.adUnitID))
                .setAdViewWrappingStrategy(new BannerAdViewWrappingStrategyBase() {*/
                    /*@NonNull
                    @Override
                    protected ViewGroup getAdViewWrapper(ViewGroup parent) {
                        return (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_ad,
                                parent, false);
                    }

                    @Override
                    protected void recycleAdViewWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        for (int i = 0; i < container.getChildCount(); i++) {
                            View v = container.getChildAt(i);
                            if (v instanceof AdView) {
                                //container.removeViewAt(i);
                                break;
                            }
                        }
                    }

                    @Override
                    protected void addAdViewToWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        //container.addView(ad);
                    }
                })
                .build();

        Log.e(TAG , "Here");*/
//        if (isNetworkConnected()) {
//            RviewFolders.setAdapter(adapterWrapper);
//        } else {
            RviewFolders.setAdapter(homeMultiSelectAdapter);
        //}

    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public void multi_select(int position) {
        if (isToolVisible) {
            if (multiselectList.contains(folderPath.get(position)))
                multiselectList.remove(folderPath.get(position));
            else
                multiselectList.add(folderPath.get(position));

            Log.d("multiselectList :", String.valueOf(multiselectList));
            toolsEnability();
            if (multiselectList.size() > 0)
                txtTitle.setText("Selected " + multiselectList.size());
            else
                txtTitle.setText("Selected 0");

            refreshAdapter();

        }
    }

    public void all_select() {
        if (isToolVisible) {
            if (multiselectList.size() != folderPath.size()) {
                multiselectList = new ArrayList<>();
                for (int i = 0; i < folderPath.size(); i++) {
                    multiselectList.add(folderPath.get(i));
                }
            } else {
                multiselectList = new ArrayList<>();
            }
            Log.d("multiselectList :", String.valueOf(multiselectList));
            toolsEnability();
            if (multiselectList.size() > 0)
                txtTitle.setText("Selected " + multiselectList.size());
            else
                txtTitle.setText("Selected 0");

            refreshAdapter();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void toolsEnability() {
        if (multiselectList.size() == 0) {
            imgDelete.setImageResource(R.drawable.ic_delete_disable);
            imgDelete.setEnabled(false);
            imgPlay.setImageResource(R.drawable.ic_play_disable);
            imgPlay.setEnabled(false);
            imgRename.setImageResource(R.drawable.ic_rename_disable);
            imgRename.setEnabled(false);
        } else if (multiselectList.size() == 1) {
            imgDelete.setImageResource(R.drawable.ic_delete);
            imgDelete.setEnabled(true);
            imgPlay.setImageResource(R.drawable.ic_play_button);
            imgPlay.setEnabled(true);
            imgRename.setImageResource(R.drawable.ic_rename);
            imgRename.setEnabled(true);
        } else {
            imgDelete.setImageResource(R.drawable.ic_delete);
            imgDelete.setEnabled(true);
            imgPlay.setImageResource(R.drawable.ic_play_button);
            imgPlay.setEnabled(true);
            imgRename.setImageResource(R.drawable.ic_rename_disable);
            imgRename.setEnabled(false);
        }
    }

    public void showDialog(final Activity activity, final String path) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_rename);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_bg);
        TextView title = (TextView) dialog.findViewById(R.id.title);
        title.setTextColor(Color.LTGRAY);
        title.setText("Rename Folder");


        final EditText vidname = dialog.findViewById(R.id.vidname);
        vidname.setHint("Folder Rename To");
        vidname.setHintTextColor(Color.LTGRAY);
        vidname.setText(new File(path).getName());
        vidname.setSelectAllOnFocus(true);
        Button rename = (Button) dialog.findViewById(R.id.rename);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!vidname.getText().toString().isEmpty()) {
                    Log.d(TAG, "FetchFolder : " + FetchFolder(new File(path).getParent()));
                    if (!FetchFolder(new File(path).getParent()).contains(vidname.getText().toString())) {
                        File mFile = new File(new File(path).getParent() + "/" + vidname.getText().toString());
                        if (!mFile.exists()) {
                            mFile.mkdirs();
                        }

                        for (int i = 0; i < FetchFolderFile(path).size(); i++) {
                            File from = new File(FetchFolderFile(path).get(i));
                            File to = new File(new File(path).getParent() + "/" + vidname.getText().toString() + "/" + new File(FetchFolderFile(path).get(i)).getName());
                            try {
                                copyFile(from, to);
                                addMedia(activity, to);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        FetchVideos(path);
                        File fileOrDirectory = new File(path);
                        deleteRecursive(fileOrDirectory);
                        imageBack();
                        dialog.dismiss();
                        folderName.clear();
                        folderPath.clear();
                        videoCount.clear();
                        allfolderpath.clear();
                        getdata();
                        homeMultiSelectAdapter.notifyDataSetChanged();
                    } else {
                        vidname.setError("Name Already Exist");
                        vidname.requestFocus();
                    }
                    /*RenameVideos(path,vidname.getText().toString());
                    File oldFolder = new File(new File(path).getParent(), new File(path).getName());
                    File newFolder = new File(new File(path).getParent(), vidname.getText().toString());
                    boolean success = oldFolder.renameTo(newFolder);
                    if (success) {
                        Toast.makeText(HomeActivity.this, "Rename to " + vidname.getText().toString(), Toast.LENGTH_SHORT).show();
                    }
                    imageBack();
                    dialog.dismiss();
                    folderName.clear();
                    folderPath.clear();
                    videoCount.clear();
                    allfolderpath.clear();
                    getdata();
                    allVideoAdapter.notifyDataSetChanged();*/

                } else {
                    vidname.setError("Please Enter Folder Name !");
                    vidname.requestFocus();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                imageBack();
                dialog.dismiss();
                folderName.clear();
                folderPath.clear();
                videoCount.clear();
                allfolderpath.clear();
                getdata();
                homeMultiSelectAdapter.notifyDataSetChanged();
            }
        });

        dialog.show();
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    private void imageBack() {
        txtTitle.setText("Folder");
        final float scale = getResources().getDisplayMetrics().density;
        int left = (int) (15 * scale + 0.5f);
        txtTitle.setPadding(left, 0, 0, 0);
        imgBack.setVisibility(View.GONE);
        toolLinear.setVisibility(View.GONE);
        toolLL.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        fab.setVisibility(View.VISIBLE);
        isToolVisible = false;
        isMultiSelect = false;
        multiselectList = new ArrayList<>();
        refreshAdapter();
    }

    @Override
    public void onBackPressed() {
        if (imgBack.getVisibility() == View.VISIBLE) {
            imageBack();
        } else {
            Intent svc = new Intent(this, BackgroundSoundService.class);
            stopService(svc);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(this, "Press again to Exit", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
        }
    }

    void getdata() {
        allvidFile = getAllVideoPath(this);

        for (int i = 0; i < allvidFile.length; i++) {
            allfolderpath.add(allvidFile[i]);
        }

        String[] proj = new String[]{
                MediaStore.Video.Media.DATA
        };
        csr = this.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);

        while (csr.moveToNext()) {
            int ind = csr.getColumnIndex(MediaStore.Video.Media.DATA);
            String path = csr.getString(ind);
            Log.d("pathofpath", path);

            imageList.add(path);
            String fpath = new File(path).getParent();
            String fname = new File(fpath).getName();
            allfolderpath.add(fpath);
            Collections.sort(allfolderpath);

            /*if (!folderName.contains(fname)) {
                folderName.add(fname);
            }*/

            if (!folderPath.contains(fpath)) {
                folderPath.add(fpath);
            }
        }

        for (int i = 0; i < folderPath.size(); i++) {
            for (int j = 0; j < imageList.size(); j++) {
                if (folderPath.get(i).equals(new File(imageList.get(j)).getParent())) {
                    if (!folderThumb.contains(imageList.get(j)))
                        folderThumb.add(imageList.get(j));
                    break;
                }
            }
        }
        for (int i = 0; i < folderPath.size(); i++) {
            folderName.add(new File(folderPath.get(i)).getName());
        }
        // Collections.sort(folderName);

        for (int i = 0; i < folderName.size(); i++) {
            for (int j = 0; j < folderPath.size(); j++) {
                if (folderName.get(i).equals(new File(folderPath.get(j)).getName())) {
                    folderp.add(i, folderPath.get(j));
                    break;
                }
            }
        }
        Log.d("folderThumb", String.valueOf(folderThumb));
        Log.d("pathfolder", String.valueOf(folderp));

        Log.d("pathpathname", String.valueOf(folderName));
        Log.d("allfolderpath", String.valueOf(allfolderpath));

        for (int i = 0; i < folderPath.size(); i++) {
            int occurrences = Collections.frequency(allfolderpath, folderPath.get(i));
            videoCount.add(occurrences);
        }
        Log.d("videoCount", String.valueOf(videoCount));

        Log.d("UniquePath", String.valueOf(folderPath));
    }

    private String[] getAllVideoPath(Context context) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.VideoColumns.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        ArrayList<String> pathArrList = new ArrayList<String>();
        //int vidsCount = 0;
        if (cursor != null) {
            //vidsCount = cursor.getCount();
            //Log.d(TAG, "Total count of videos: " + vidsCount);
            while (cursor.moveToNext()) {
                pathArrList.add(cursor.getString(0));
                //Log.d(TAG, cursor.getString(0));
            }
            cursor.close();
        }

        return pathArrList.toArray(new String[pathArrList.size()]);
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
    public void onPositiveClick(int from) {
        if (from == 1) {
            if (multiselectList.size() > 0) {
                for (int i = 0; i < multiselectList.size(); i++) {
                    FetchVideos(multiselectList.get(i));
                }
                folderName.clear();
                folderPath.clear();
                videoCount.clear();
                allfolderpath.clear();
                multiselectList.clear();
                getdata();
                homeMultiSelectAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Videos Deleted", Toast.LENGTH_SHORT).show();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }

        } else if (from == 2) {
            if (mActionMode != null) {
                mActionMode.finish();
            }

            folderName.clear();
            folderPath.clear();
            videoCount.clear();
            allfolderpath.clear();
            multiselectList.clear();
            homeMultiSelectAdapter.notifyDataSetChanged();

        }

        toolLinear.setVisibility(View.VISIBLE);
        toolLL.setBackgroundColor(getResources().getColor(R.color.textColor));
        imgBack.setVisibility(View.VISIBLE);
        fab.setVisibility(View.GONE);
        txtTitle.setPadding(0, 0, 0, 0);
        /*final float scale = getResources().getDisplayMetrics().density;
        int left = (int) (10 * scale + 0.5f);
        int right = (int) (10 * scale + 0.5f);
        int top = (int) (60 * scale + 0.5f);
        int bottom = (int) (10 * scale + 0.5f);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(left, top, right, bottom);
        cardView.setLayoutParams(params);*/
        imageBack();
    }

    @Override
    public void onNegativeClick(int from) {
        toolLinear.setVisibility(View.VISIBLE);
        toolLL.setBackgroundColor(getResources().getColor(R.color.textColor));
        imgBack.setVisibility(View.VISIBLE);
        fab.setVisibility(View.GONE);
        txtTitle.setPadding(0, 0, 0, 0);
        /*final float scale = getResources().getDisplayMetrics().density;
        int left = (int) (10 * scale + 0.5f);
        int right = (int) (10 * scale + 0.5f);
        int top = (int) (60 * scale + 0.5f);
        int bottom = (int) (10 * scale + 0.5f);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(left, top, right, bottom);
        cardView.setLayoutParams(params);*/
    }

    @Override
    public void onNeutralClick(int from) {

    }

    private void FetchVideos(String path) {
        File directory = new File(path);
        if (directory.exists()) {
            File[] files = directory.listFiles();

            for (int i = 0; i < files.length; i++) {
                String file_name = String.valueOf(files[i]);
                if (isVideoFile(file_name)) {

                    File file = new File(file_name);
                    file.delete();
                    removeMedia(HomeActivity.this, file);

                    if (file.exists()) {
                        try {
                            file.getCanonicalFile().delete();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (file.exists()) {
                            getApplicationContext().deleteFile(file.getName());
                        }
                    }
                }
            }
        }
    }

    private void FetchAndPlay(String path) {
        File directory = new File(path);

        if (directory.exists()) {
            File[] files = directory.listFiles();

            for (int i = 0; i < files.length; i++) {
                String file_name = String.valueOf(files[i]);
                if (isVideoFile(file_name)) {
                    videoList.add(file_name);
                }
            }
        }
    }

    private ArrayList<String> FetchFolder(String path) {

        ArrayList<String> filenames = new ArrayList<String>();

        File directory = new File(path);
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {

            String file_name = files[i].getName();
            // you can store name to arraylist and use it later
            filenames.add(file_name);
        }
        return filenames;
    }

    private ArrayList<String> FetchFolderFile(String path) {

        ArrayList<String> filenames = new ArrayList<String>();

        File directory = new File(path);
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {

            String file_name = String.valueOf(files[i]);
            // you can store name to arraylist and use it later
            filenames.add(file_name);
        }
        return filenames;
    }
}
