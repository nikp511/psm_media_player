package video.player.qrplayer.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.clockbyte.admobadapter.bannerads.AdmobBannerRecyclerAdapterWrapper;
import com.clockbyte.admobadapter.bannerads.BannerAdViewWrappingStrategyBase;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;

import video.player.qrplayer.R;
import video.player.qrplayer.adapter.AllVideoAdapter;
import video.player.qrplayer.multiselect.AlertDialogHelper;
import video.player.qrplayer.multiselect.RecyclerItemClickListener;
import video.player.qrplayer.utils.Utils;

//import androidx.core.widget.SwipeRefreshLayout;

@SuppressLint("RestrictedApi")
public class AllVideoActivity extends AppCompatActivity implements AlertDialogHelper.AlertDialogListener {
    public static String folder;
    ArrayList<String> videoList = new ArrayList<>();
    RecyclerView RviewAllVideo;

    ArrayList<String> multiselectList = new ArrayList<>();

    SharedPreferences preferences;
    FloatingActionButton fab;
    String vidurl;
    SwipeRefreshLayout mSwipeRefreshLayout;
    CardView cardView, cardView1;
    boolean isMultiSelect = false;
    boolean isToolVisible = false;
    ActionMode mActionMode;
    AlertDialogHelper alertDialogHelper;
    AllVideoAdapter allVideoAdapter;
    LinearLayout toolLinear, toolLL, llTools;
    ImageView imgBack, imgSelect, imgPlay, imgRename, imgDelete, imgRefresh;
    Animation animation;
    TextView txtTitle;
    boolean isFromFolder;
    boolean isKeyboardOpen = false;
    SearchView searchView;
    private Cursor csr;
    private String TAG = getClass().getSimpleName();
    private LinearLayoutManager layoutManager;

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
        allVideoAdapter.multiselectList = multiselectList;
        allVideoAdapter.videoList = videoList;
        allVideoAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_video);

        AdView mAdView = findViewById(R.id.adview);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (getIntent() != null) {
            Log.e(TAG, "check  :  " + getIntent().getStringExtra("key"));
        } else {
            Log.e(TAG, "false  :  ");
        }
        isFromFolder = getIntent().getBooleanExtra("video", false);

        Utils.mainPlayList = new ArrayList();
        alertDialogHelper = new AlertDialogHelper(AllVideoActivity.this);
        toolLL = findViewById(R.id.toolLL);
        llTools = findViewById(R.id.llTools);
        fab = findViewById(R.id.fab);
        toolLinear = findViewById(R.id.toolLinear);
        imgBack = findViewById(R.id.imgBack);
        imgSelect = findViewById(R.id.imgSelect);
        imgPlay = findViewById(R.id.imgPlay);
        imgRename = findViewById(R.id.imgRename);
        imgDelete = findViewById(R.id.imgDelete);
        imgRefresh = findViewById(R.id.imgRefresh);


        txtTitle = findViewById(R.id.txtTitle);
        mSwipeRefreshLayout = findViewById(R.id.swipe);
        RviewAllVideo = findViewById(R.id.recycle);
        cardView = findViewById(R.id.card_view);
        cardView1 = findViewById(R.id.card_view1);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        RviewAllVideo.setLayoutManager(layoutManager);

        if (isFromFolder) {
            llTools.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            imgBack.setVisibility(View.VISIBLE);
            folder = getIntent().getStringExtra("foldername");
            txtTitle.setText(new File(getIntent().getStringExtra("foldername")).getName());
        } else {
            llTools.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
            imgBack.setVisibility(View.GONE);
        }

        preferences = getSharedPreferences("my", MODE_PRIVATE);

        vidurl = preferences.getString("videourl", "nourl");

        searchView = findViewById(R.id.search);
        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        ImageView searchClose = (ImageView) searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        searchClose.setImageResource(R.drawable.search_close);
        searchAutoComplete.setHintTextColor(Color.WHITE);
        searchAutoComplete.setTextColor(Color.WHITE);
        int searchImgId = androidx.appcompat.R.id.search_button;
        ImageView v = (ImageView) searchView.findViewById(searchImgId);
        v.setImageResource(R.drawable.ic_search_black_24dp);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtTitle.setVisibility(View.GONE);
                isKeyboardOpen = true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                txtTitle.setVisibility(View.VISIBLE);
                return isKeyboardOpen = false;
            }
        });


        getdata();

        if (vidurl.equals("nourl")) {
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AllVideoActivity.this, HomeActivity.class));
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
        cardView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AllVideoActivity.this, WhatsappStatusActivity.class));
                finish();
                overridePendingTransition(R.anim.enter, R.anim.exit);
            }
        });
        animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isKeyboardOpen) {
                    Log.e(TAG, "1");
                    searchView.setIconified(true);
                    isKeyboardOpen = false;
                } else {
                    Log.e(TAG, "2");
                    onBackPressed();
                }
            }
        });
        imgRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgRefresh.startAnimation(animation);
                videoList.clear();
                getdata();
                allVideoAdapter.notifyDataSetChanged();
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
                    Utils.mainPlayList = multiselectList;
                    Utils.playPosition = 0;
                    Utils.listSize = multiselectList.size();
                    Intent in = new Intent(AllVideoActivity.this, VideoPlayActivity.class);
                    in.putExtra("playid", "four");
                    startActivity(in);
                    finish();
                }
            }
        });
        imgRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(AllVideoActivity.this, multiselectList.get(0));
            }
        });
        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialogHelper.showAlertDialog("", "Do you want to Delete File ?", "DELETE", "CANCEL", 1, false);
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Utils.mainPlayList = videoList;
                int pos = Utils.mainPlayList.indexOf(vidurl);
                Utils.playPosition = pos;
                Utils.listSize = videoList.size();

                Intent in = new Intent(AllVideoActivity.this, VideoPlayActivity.class);
                in.putExtra("playid", "four");
                startActivity(in);
                finish();

                Log.d("videolist", String.valueOf(Utils.mainPlayList));
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                videoList.clear();

                getdata();
                //adapter.notifyDataSetChanged();
                allVideoAdapter.notifyDataSetChanged();
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

        allVideoAdapter = new AllVideoAdapter(this, videoList, multiselectList);
        //adapter = new HomeAdapter(this, folderName, folderPath, videoCount);
        //RviewAllVideo.setAdapter(allVideoAdapter);
        Log.d("Count :", String.valueOf(allVideoAdapter.getItemCount()));
        RviewAllVideo.addOnItemTouchListener(new RecyclerItemClickListener(this, RviewAllVideo, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (isMultiSelect)
                    multi_select(position);
                else {
                    Utils.mainPlayList = allVideoAdapter.mFilteredList;
                    String path = allVideoAdapter.mFilteredList.get(position);
                    Utils.listSize = Utils.mainPlayList.size();
                    Utils.playPosition = Utils.mainPlayList.indexOf(path);

                    Intent in = new Intent(AllVideoActivity.this, VideoPlayActivity.class);
                    if (isFromFolder) {
                        in.putExtra("playid", "one");
                    } else {
                        in.putExtra("playid", "four");
                    }

                    startActivity(in);
                    finish();
                }

            }

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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                allVideoAdapter.getFilter().filter(newText);
                return true;
            }
        });

        AdmobBannerRecyclerAdapterWrapper adapterWrapper = AdmobBannerRecyclerAdapterWrapper.builder(this)
                .setLimitOfAds(10)
                .setFirstAdIndex(2)
                .setSingleAdSize(AdSize.LARGE_BANNER)
                .setNoOfDataBetweenAds(5)
                .setAdapter(allVideoAdapter)
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
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        for (int i = 0; i < container.getChildCount(); i++) {
                            View v = container.getChildAt(i);
                            if (v instanceof AdView) {
                                container.removeViewAt(i);
                                break;
                            }
                        }
                    }

                    @Override
                    protected void addAdViewToWrapper(@NonNull ViewGroup wrapper, @NonNull AdView ad) {
                        ViewGroup container = (ViewGroup) wrapper.findViewById(R.id.ad_container);
                        container.addView(ad);
                    }
                })
                .build();

        Log.e(TAG, "Here");
        //if (isNetworkConnected()) {
        //    RviewAllVideo.setAdapter(adapterWrapper);
        //} else {
        RviewAllVideo.setAdapter(allVideoAdapter);
        //}


    }

    void getdata() {
        String[] proj = new String[]{
                MediaStore.Video.Media.DATA
        };
        csr = this.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);
        while (csr.moveToNext()) {
            int ind = csr.getColumnIndex(MediaStore.Video.Media.DATA);
            String path = csr.getString(ind);
            Log.d("pathofpath", path);
            if (isFromFolder) {
                if (new File(path).getParent().equals(getIntent().getStringExtra("foldername"))) {
                    if (!videoList.contains(path)) {
                        videoList.add(path);
                    }
                }
            } else {
                videoList.add(path);
            }
        }
        Collections.sort(videoList);


    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public void multi_select(int position) {
        if (isToolVisible) {
            if (multiselectList.contains(videoList.get(position)))
                multiselectList.remove(videoList.get(position));
            else
                multiselectList.add(videoList.get(position));

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
            if (multiselectList.size() != videoList.size()) {
                multiselectList = new ArrayList<>();
                for (int i = 0; i < videoList.size(); i++) {
                    multiselectList.add(videoList.get(i));
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
        if(Utils.mainPlayList.isEmpty()){
            fab.setVisibility(View.GONE);
        }
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
                        File from = new File(new File(path).getParent(), new File(path).getName());
                        File to = new File(new File(path).getParent(), vidname.getText().toString() + ".mp4");
                        if (from.exists())
                            from.renameTo(to);
                        removeMedia(activity, from);
                        addMedia(activity, to);
                        imageBack();
                        dialog.dismiss();
                        videoList.clear();
                        getdata();
                        allVideoAdapter.notifyDataSetChanged();
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
                videoList.clear();

                getdata();
                allVideoAdapter.notifyDataSetChanged();
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
        if (isFromFolder) {
            txtTitle.setText(new File(getIntent().getStringExtra("foldername")).getName());
        } else {
            txtTitle.setText("All Videos");
            imgBack.setVisibility(View.GONE);
        }

        final float scale = getResources().getDisplayMetrics().density;
        int left = (int) (15 * scale + 0.5f);
        txtTitle.setPadding(left, 0, 0, 0);
        toolLinear.setVisibility(View.GONE);
        toolLL.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        if (vidurl.equals("nourl")) {
            fab.setVisibility(View.INVISIBLE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
        isToolVisible = false;
        isMultiSelect = false;
        multiselectList = new ArrayList<>();
        refreshAdapter();
    }

    @Override
    public void onBackPressed() {
        if (isToolVisible) {
            imageBack();
        } else {
            Intent intent = new Intent(AllVideoActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.enter, R.anim.exit);
        }
    }

    @Override
    public void onPositiveClick(int from) {
        if (from == 1) {
            if (multiselectList.size() > 0) {
                for (int i = 0; i < multiselectList.size(); i++) {
                    if (isVideoFile(multiselectList.get(i))) {
                        File file = new File(multiselectList.get(i));
                        file.delete();
                        removeMedia(AllVideoActivity.this, file);
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
                videoList.clear();
                multiselectList.clear();
                getdata();
                allVideoAdapter.notifyDataSetChanged();
                Log.e(TAG, "Selected : " + multiselectList);

                Toast.makeText(this, "Videos Deleted", Toast.LENGTH_SHORT).show();
                if (mActionMode != null) {
                    mActionMode.finish();
                }
            }

        } else if (from == 2) {
            if (mActionMode != null) {
                mActionMode.finish();
            }

            videoList.clear();
            multiselectList.clear();
            allVideoAdapter.notifyDataSetChanged();

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
                    removeMedia(AllVideoActivity.this, file);

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
