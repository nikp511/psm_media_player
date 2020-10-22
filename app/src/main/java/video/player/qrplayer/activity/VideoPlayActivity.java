package video.player.qrplayer.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ui.PlaybackControlView;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import video.player.qrplayer.R;
import video.player.qrplayer.database.DatabaseHelper;
import video.player.qrplayer.fragment.RecentStatusFragment;
import video.player.qrplayer.player.PopupVideoPlayer;
import video.player.qrplayer.player.playqueue.PlayQueue;
import video.player.qrplayer.player.playqueue.SinglePlayQueue;
import video.player.qrplayer.services.BackgroundSoundService;
import video.player.qrplayer.util.NavigationHelper;
import video.player.qrplayer.util.PermissionHelper;
import video.player.qrplayer.utils.PassKey;
import video.player.qrplayer.utils.Utils;
import video.player.qrplayer.videoplayer.player.MediaManager;
import video.player.qrplayer.videoplayer.player.MediaSystem;
import video.player.qrplayer.videoplayer.player.UserActionInterface;
import video.player.qrplayer.videoplayer.player.UserActionInterfaceStd;
import video.player.qrplayer.videoplayer.player.VideoLayout;
import video.player.qrplayer.videoplayer.player.VideoPLayerVideoview;
import video.player.qrplayer.videoplayer.player.VideoStd;

import static org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM;


public class VideoPlayActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener {


    public static String TAG = VideoPlayActivity.class.getSimpleName();
    public static long curpos = 0;
    public static String vidurl;
    public boolean isplaybg = false;
    VideoPLayerVideoview videoView;
    ImageView iv_poster, next, previous, playPause, imgOrientation, fullScreen, more,normalscreen;
    String url, vname;
    VideoStd videoStd;
    int pos = Utils.playPosition;
    FrameLayout frameLayout;
    TextView txtVideoTitle;
    String vidLength, vidFile, vidLocation, vidDate, vidResolution, vidSize;
    int a = 0;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    String folder;
    String playid;
    Intent svc;
    ViewDialog alert = new ViewDialog();

    //Database
    DatabaseHelper myDb;
    String id;
    String dataUrl = null;
    long dataLength = 0;
    private int currentApiVersion;
    private boolean isDismiss;

    //private static final String TAG = "PlayerActivity";
    public static final int FADE_OUT = 0;
    public static final int SHOW_PROGRESS = 1;
    private SurfaceView sv;
    private MediaPlayer player;
    private TextView subtitleText;
   // private SubtitleProcessingTask subsFetchTask;
    private SeekBar mSeeker;
    AudioManager audioManager;
    private PlaybackControlView controller;
    //private MessageHandler mHandler;


    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static void addMedia(Context c, File f) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(f));
        c.sendBroadcast(intent);
    }

    private static void removeMedia(Context c, File f) {
        ContentResolver resolver = c.getContentResolver();
        resolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.DATA + "=?", new String[]{f.getAbsolutePath()});
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+

        this.getWindow().getDecorView().setSystemUiVisibility(flags);

        // Code below is to handle presses of Volume up or Volume down.
        // Without this, after pressing volume buttons, the navigation bar will
        // show up and won't hide
        final View decorView = this.getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            }
        });


        setContentView(R.layout.activity_video_display);


        if (isMyServiceRunning(PopupVideoPlayer.class)) {
            Intent myService = new Intent(VideoPlayActivity.this, PopupVideoPlayer.class);
            stopService(myService);
        }

        myDb = new DatabaseHelper(this);
        init();
        PassKey.PLAYER_SEEK = -1;
        vidurl = url;
        videoView.imgPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.playPosition = pos;
                Log.e(TAG, "List Size : " + Utils.mainPlayList.size());
                Log.e(TAG, "List Size : " + Utils.playPosition);
                openPopupPlayer();

            }
        });
    }



    private void openPopupPlayer() {
        if (!PermissionHelper.isPopupEnabled(VideoPlayActivity.this)) {
            PermissionHelper.showPopupEnablementToast(VideoPlayActivity.this);
            return;
        }
        Utils.popupList = new ArrayList<>();
        Utils.popupList = Utils.mainPlayList;
        vidurl = videoView.getCurrentUrl().toString();
        MediaManager.pause();
        StreamInfo currentInfo = new StreamInfo(12, "", "", VIDEO_STREAM, "", "", 20);
        final PlayQueue itemQueue = new SinglePlayQueue(currentInfo);
        Toast.makeText(VideoPlayActivity.this, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        final Intent intent = NavigationHelper.getPlayerIntent(
                VideoPlayActivity.this, PopupVideoPlayer.class, itemQueue, "360p");
        VideoPlayActivity.this.startService(intent);
        Utils.isPopup = true;
        onBackPressed();
    }

    private void init() {

        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }

        videoView = findViewById(R.id.videoView);
        iv_poster = findViewById(R.id.iv_poster);
        videoStd = new VideoStd(this);
        next = videoView.findViewById(R.id.next);
        previous = videoView.findViewById(R.id.previous);
        playPause = videoView.findViewById(R.id.start);
        imgOrientation = videoView.findViewById(R.id.orientation);
        fullScreen = videoView.findViewById(R.id.fullscreen);
        normalscreen = videoView.findViewById(R.id.normal_screeen);
        frameLayout = videoView.findViewById(R.id.surface_container);
        more = videoView.findViewById(R.id.more);
        txtVideoTitle = videoView.findViewById(R.id.title);
        preferences = getSharedPreferences("my", MODE_PRIVATE);
        editor = preferences.edit();
        svc = new Intent(this, BackgroundSoundService.class);

        url = Utils.mainPlayList.get(Utils.playPosition);
        Log.e(TAG, "Utils.playPosition : " + Utils.playPosition);
        vname = new File(Utils.mainPlayList.get(Utils.playPosition)).getName();
        playid = getIntent().getStringExtra("playid");


        if (playid.equals("zero")) {
            folder = HomeActivity.folder;
        } else if (playid.equals("one")) {
            folder = AllVideoActivity.folder;
        } else if (playid.equals("two")) {
            folder = RecentStatusFragment.folder;
        }
        editor.putString("folder", folder);
        editor.commit();

        Glide.with(this).load(url).into(videoView.thumbImageView);
        getData(url);
        if (dataLength != 0) {
            playResume();
        } else {
            String vidname = new File(Utils.mainPlayList.get(Utils.playPosition)).getName();
            videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);
            Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
            VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
            videoView.startVideo();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);


        /*Log.d(TAG,"id  :  "+id);
        Log.d(TAG,"url  :  "+data_url);
        Log.d(TAG,"length  :  "+data_length);*/

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pos < Utils.listSize - 1) {
                    pos++;
                    Utils.playPosition = pos;
                    myDb.updateData((String) videoView.getCurrentUrl(), MediaManager.getCurrentPosition());
                    String vidurl = (String) Utils.mainPlayList.get(pos);
                    VideoPlayActivity.vidurl = vidurl;
                    /*String vidname = new File(vidurl).getName();
                    videoView.setUp(vidurl, vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);*/
                    Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                    MediaManager.pause();
                    /*VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                    videoView.startVideo();*/
                    getData(vidurl);
                    if (dataLength != 0) {
                        playResume();
                    } else {
                        String vidname = new File(Utils.mainPlayList.get(Utils.playPosition)).getName();
                        videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);
                        Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                        VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                        videoView.startVideo();
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (pos > 0) {
                    pos--;
                    Utils.playPosition = pos;
                    myDb.updateData((String) videoView.getCurrentUrl(), MediaManager.getCurrentPosition());
                    String vidurl = (String) Utils.mainPlayList.get(pos);
                    VideoPlayActivity.vidurl = vidurl;
                   /* String vidname = new File(vidurl).getName();
                    videoView.setUp(vidurl, vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);*/
                    Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                    MediaManager.pause();
                    /*VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                    videoView.startVideo();*/
                    getData(vidurl);
                    if (dataLength != 0) {
                        playResume();
                    } else {
                        String vidname = new File(Utils.mainPlayList.get(Utils.playPosition)).getName();
                        videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);
                        Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                        VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                        videoView.startVideo();
                    }
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            }
        });


        fullScreen.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View v) {
                        fullScreen.setImageResource(R.drawable.normal_screen);
                        fullScreen.setVisibility(View.GONE);
                        normalscreen.setVisibility(View.VISIBLE);
                        //  this.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        //  this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

                final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

                // This work only for android 4.4+

                //this.getWindow().getDecorView().setSystemUiVisibility(flags);

                // Code below is to handle presses of Volume up or Volume down.
                // Without this, after pressing volume buttons, the navigation bar will
                // show up and won't hide
              /*  final View decorView = this.getWindow().getDecorView();
                decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            decorView.setSystemUiVisibility(flags);
                        }
                    }
                });
*/

                if (a % 2 == 0) {
                    fullScreen.setImageResource(R.drawable.normal_screen);
                    Display screenOrientation = getWindowManager().getDefaultDisplay();
                    int orientation = Configuration.ORIENTATION_UNDEFINED;
                    if (screenOrientation.getWidth() == screenOrientation.getHeight()) {
                        orientation = Configuration.ORIENTATION_SQUARE;
                        //Do something
                        getActionBar().hide();

                    } else {
                        if (screenOrientation.getWidth() < screenOrientation.getHeight()) {
                            orientation = Configuration.ORIENTATION_PORTRAIT;
                            //Do something
                            int width = frameLayout.getMeasuredWidth();
                            int height = frameLayout.getMeasuredHeight();
                            Log.d("sizew", String.valueOf(width));
                            Log.d("sizeh", String.valueOf(height));
                            MediaManager.textureView.setVideoSize(width, height);

                        } else {
                            orientation = Configuration.ORIENTATION_LANDSCAPE;
                            //Do something
                            int width = frameLayout.getMeasuredWidth();
                            int height = frameLayout.getMeasuredHeight();
                            Log.d("sizew", String.valueOf(width));
                            Log.d("sizeh", String.valueOf(height));
                            MediaManager.textureView.setVideoSize(width, height);

                        }
                    }
                } else {
                    MediaManager.textureView.setVideoSize(MediaManager.instance().currentVideoWidth, MediaManager.instance().currentVideoHeight);
                    fullScreen.setImageResource(R.drawable.full_screen);
                }
               // a++;
            }
        });

        normalscreen.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick(View v) {

                normalscreen.setImageResource(R.drawable.full_screen);
                normalscreen.setVisibility(View.GONE);
                fullScreen.setVisibility(View.VISIBLE);
               // Display screenOrientation = getWindowManager().getDefaultDisplay();
                MediaManager.textureView.setVideoSize(MediaManager.instance().currentVideoWidth, MediaManager.instance().currentVideoHeight);

                //Display screenOrientation = getWindowManager().getDefaultDisplay();
               // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            }
        });

        PlaybackControlView customController = (PlaybackControlView) findViewById(R.id.exo_controller);

        if (customController != null) {
            this.controller = customController;
        }

else {
            this.controller = null;
        }

        imgOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Display screenOrientation = getWindowManager().getDefaultDisplay();
                int orientation = Configuration.ORIENTATION_UNDEFINED;
                if (screenOrientation.getWidth() == screenOrientation.getHeight()) {
                    orientation = Configuration.ORIENTATION_SQUARE;
                } else {
                    if (screenOrientation.getWidth() < screenOrientation.getHeight()) {
                        orientation = Configuration.ORIENTATION_PORTRAIT;
                    } else {
                        orientation = Configuration.ORIENTATION_LANDSCAPE;
                    }
                }
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
                a = 0;
                MediaManager.textureView.setVideoSize(MediaManager.instance().currentVideoWidth, MediaManager.instance().currentVideoHeight);


            }
        });
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(VideoPlayActivity.this, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.option_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(VideoPlayActivity.this);
                Menu itemSetAs = popup.getMenu();
                SubMenu s = itemSetAs.findItem(R.id.tools).getSubMenu();
                SpannableString headerTitle = new SpannableString(itemSetAs.findItem(R.id.tools).getTitle());
                headerTitle.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, headerTitle.length(), 0);
                s.setHeaderTitle(headerTitle);
                popup.show();
            }
        });

        iv_poster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VideoPlayActivity.this, VideoPlayActivity.class);
                startActivity(intent);
            }
        });

        videoView.backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        //videoView.setLayerType(View.LAYER_TYPE_HARDWARE,null);
    }

    public void playResume() {
       /* Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    MediaManager.pause();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();*/
        alert.playResumeDialog(VideoPlayActivity.this);
    }

    public void getData(String url) {
        Cursor res = myDb.getAllData(url);
        if (res.getCount() == 0) {
            dataUrl = null;
            dataLength = 0;
            id = null;
        } else {
            while (res.moveToNext()) {
                Log.d(TAG, "url  :  " + res.getString(1));
                Log.d(TAG, "length  :  " + res.getLong(2));
                id = res.getString(0);
                dataUrl = res.getString(1);
                dataLength = res.getLong(2);
            }
        }

        if (dataUrl == null) {
            myDb.insertData(url, MediaManager.getCurrentPosition());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "Time  1: " + PassKey.PLAYER_SEEK);
        Cursor res = myDb.getAllData(Utils.mainPlayList.get(Utils.playPosition));
        if (res.getCount() == 0) {
            dataUrl = null;
            dataLength = 0;
            id = null;
        } else {
            while (res.moveToNext()) {
            /*id.add(res.getInt(0));
            data_url.add(res.getString(1));
            data_length.add(res.getLong(2));*/
                Log.d(TAG, "url  :  " + res.getString(1));
                Log.d(TAG, "length  :  " + res.getLong(2));
                id = res.getString(0);
                dataUrl = res.getString(1);
                dataLength = res.getLong(2);
            }
        }

        if (isMyServiceRunning(BackgroundSoundService.class) == true) {
            if (BackgroundSoundService.player.isPlaying()) {
                long curdur = BackgroundSoundService.player.getCurrentPosition();
                videoView.setUp(vidurl, new File(vidurl).getName(), VideoStd.SCREEN_WINDOW_FULLSCREEN);
                videoView.startVideo();
                MediaManager.seekTo(curdur);
                MediaManager.start();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                if (PassKey.PLAYER_SEEK != -1) {
                    MediaManager.seekTo(PassKey.PLAYER_SEEK);
                    MediaManager.start();
                }
            }
        }
        stopService(svc);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
/*
        try {
            *//*myDb.updateData((String) videoView.getCurrentUrl(), MediaManager.getCurrentPosition());
            PassKey.PLAYER_SEEK = MediaManager.getCurrentPosition();
            Log.e(TAG, "Time  2: " + PassKey.PLAYER_SEEK);*//*
            MediaManager.start();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // VideoLayout.releaseAllVideos();
        if (!playid.equals("two") && !Utils.mainPlayList.isEmpty() && Utils.playPosition != 0) {
            Log.e(TAG, "Utils.playPosition : " + Utils.playPosition);
            Log.e(TAG, " Utils.mainPlayList : " + Utils.mainPlayList);
            editor.putString("videourl", (String) Utils.mainPlayList.get(Utils.playPosition));
            editor.commit();
        }

        if (isplaybg) {
            curpos = MediaManager.getCurrentPosition();
            svc.putExtra("url", String.valueOf(videoView.getCurrentUrl()));
            startService(svc);
            isplaybg = false;
        } else {
            stopService(svc);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
        try {
            myDb.updateData((String) videoView.getCurrentUrl(), MediaManager.getCurrentPosition());
            PassKey.PLAYER_SEEK = MediaManager.getCurrentPosition();
            Log.e(TAG, "Time  2: " + PassKey.PLAYER_SEEK);
            MediaManager.pause();
            videoView.onStatePause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        MediaManager.pause();
    }

    @Override
    public void onBackPressed() {
        if (MediaSystem.mediaPlayer != null) {
            MediaManager.pause();
            myDb.updateData((String) videoView.getCurrentUrl(), MediaManager.getCurrentPosition());
            if (!playid.equals("two")) {
                editor.putString("videourl", (String) Utils.mainPlayList.get(Utils.playPosition));
                editor.commit();
            }
            Intent intent;
            if (playid.equals("two")) {
                intent = new Intent(VideoPlayActivity.this, WhatsappStatusActivity.class);
            } else if (playid.equals("three")) {
                intent = new Intent(VideoPlayActivity.this, HomeActivity.class);
            } else if (playid.equals("four")) {
                intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                intent.putExtra("video", false);
            } else {
                folder = preferences.getString("folder", folder);
                intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                intent.putExtra("video", true);
                intent.putExtra("foldername", folder);
            }
            startActivity(intent);
            finish();
        } else {
            Intent intent;
            if (playid.equals("two")) {
                intent = new Intent(VideoPlayActivity.this, WhatsappStatusActivity.class);
            } else if (playid.equals("three")) {
                intent = new Intent(VideoPlayActivity.this, HomeActivity.class);
            } else if (playid.equals("four")) {
                intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                intent.putExtra("video", false);
            } else {
//                folder = preferences.getString("folder", folder);
                intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                intent.putExtra("video", true);
                intent.putExtra("foldername", folder);
            }
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play:
                MediaManager.pause();
                alert.playBackgroundDialog(VideoPlayActivity.this);
                return true;
            case R.id.tools:
                return true;
            case R.id.share:
                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpg");
                //  final File photoFile = new File((String) videoView.getCurrentUrl());
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse((String) videoView.getCurrentUrl()));
                startActivity(Intent.createChooser(shareIntent, "Share image using"));
                return true;
            case R.id.rename:
                MediaManager.pause();
                alert.showDialog(VideoPlayActivity.this);
                return true;
            case R.id.delete:
                Log.e(TAG, "URL  :  " + (String) videoView.getCurrentUrl());
                Utils.mainPlayList.remove(videoView.getCurrentUrl());
                Utils.listSize = Utils.mainPlayList.size();
                File file = new File((String) videoView.getCurrentUrl());
                file.delete();
                removeMedia(VideoPlayActivity.this, file);

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
                if (pos == Utils.listSize) {
                    Intent intent;
                    Log.e(TAG,"playid : "+playid);
                    if (playid.equals("two")) {
                        intent = new Intent(VideoPlayActivity.this, WhatsappStatusActivity.class);
                    } else if (playid.equals("three")) {
                        intent = new Intent(VideoPlayActivity.this, HomeActivity.class);
                    } else if (playid.equals("four")) {
                        intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                    } else {
//                folder = preferences.getString("folder", folder);
                        intent = new Intent(VideoPlayActivity.this, VideoListActivity.class);
                        intent.putExtra("foldername", folder);
                    }
                    startActivity(intent);
                    finish();
                } else {
//                    pos++;
                    String vidurl = (String) Utils.mainPlayList.get(pos);
                    String vidname = new File(vidurl).getName();
                    videoView.setUp(vidurl, vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);
                    Glide.with(VideoPlayActivity.this).load("").into(videoView.thumbImageView);
                    VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                    videoView.startVideo();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
                editor.putString("videourl", "nourl");
                editor.commit();
                return true;
            case R.id.info:
                MediaManager.pause();
                videoInfo();
                return true;
            default:
                return false;
        }
    }

    public void visibilityofad() {
       // videoView.mAdView.setVisibility(View.INVISIBLE);
        //videoView.imgclose.setVisibility(View.INVISIBLE);
    }

    public void videoInfo() {


        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource((String) videoView.getCurrentUrl());
        int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        retriever.release();
        long seconds = Integer.parseInt(time);
        vidLength = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(seconds),
                TimeUnit.MILLISECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds)),
                TimeUnit.MILLISECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds)));
        File f = new File((String) videoView.getCurrentUrl());
        long length = f.length();
        Date lastModDate = new Date(f.lastModified());
        vidDate = String.valueOf(lastModDate);
        vidSize = getFileSize(length) + " (" + length + ")";
        vidResolution = String.valueOf(width) + " x " + String.valueOf(height);
        vidFile = new File((String) videoView.getCurrentUrl()).getName();
        vidLocation = new File((String) videoView.getCurrentUrl()).getParent();
        Log.d("videoinfo", "vidLength  : " + String.valueOf(vidLength));
        Log.d("videoinfo", "vidFile  : " + String.valueOf(vidFile));
        Log.d("videoinfo", "vidLocation  : " + String.valueOf(vidLocation));
        Log.d("videoinfo", "vidDate  : " + String.valueOf(vidDate));
        Log.d("videoinfo", "vidResolution  : " + String.valueOf(vidResolution));
        Log.d("videoinfo", "vidSize  : " + String.valueOf(vidSize));
        alert.infoDialog(VideoPlayActivity.this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public class ViewDialog {

        public void showDialog(Activity activity) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_rename);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_bg);
            TextView title = (TextView) dialog.findViewById(R.id.title);
            title.setTextColor(Color.LTGRAY);

            String s = new File((String) videoView.getCurrentUrl()).getName();
            String before = s.substring(0, s.indexOf("."));

            final EditText vidname = dialog.findViewById(R.id.vidname);
            vidname.setText(before);
            vidname.setSelectAllOnFocus(true);

            Button rename = (Button) dialog.findViewById(R.id.rename);
            Button cancel = (Button) dialog.findViewById(R.id.cancel);
            rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String sdcard = new File((String) videoView.getCurrentUrl()).getParent();
                    String newName = vidname.getText().toString();
                    txtVideoTitle.setText(newName);
                    newName = newName + ".mp4";
                    String renamepath = sdcard + "/" + newName;
                    File from = new File((String) videoView.getCurrentUrl());
                    File to = new File(sdcard + "/" + newName);
                    from.renameTo(to);
                    removeMedia(VideoPlayActivity.this, from);
                    addMedia(VideoPlayActivity.this, to);
                    if (to.exists()) {
                        Log.d("renamepath", renamepath);
                    } else {
                        Log.d("renamepath", "not exist");
                    }
                    Toast.makeText(VideoPlayActivity.this, "Rename to " + newName, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    MediaManager.start();
                    visibilityofad();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    MediaManager.start();
                    visibilityofad();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    MediaManager.start();
                    visibilityofad();
                }
            });

            dialog.show();
        }

        public void playBackgroundDialog(final Activity activity) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_play_background);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_bg);
            TextView title = (TextView) dialog.findViewById(R.id.title);
            title.setTextColor(Color.LTGRAY);

            String s = new File((String) videoView.getCurrentUrl()).getName();
            String before = s.substring(0, s.indexOf("."));

            final CheckBox checkBox = dialog.findViewById(R.id.checkbox);

            Button ok = (Button) dialog.findViewById(R.id.rename);
            Button cancel = (Button) dialog.findViewById(R.id.cancel);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkBox.isChecked()) {
                        isplaybg = true;
                    } else
                        isplaybg = false;
                    dialog.dismiss();
                    MediaManager.start();
                    visibilityofad();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isplaybg = false;
                    dialog.dismiss();
                    MediaManager.start();
                    visibilityofad();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    MediaManager.start();
                    visibilityofad();
                }
            });

            dialog.show();
        }

        public void playResumeDialog(final Activity activity) {

            isDismiss = false;
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_resume);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_bg);
            TextView title = (TextView) dialog.findViewById(R.id.title);
            title.setTextColor(Color.LTGRAY);

//            String s = new File((String) videoView.getCurrentUrl()).getName();


            Button ok = (Button) dialog.findViewById(R.id.rename);
            Button cancel = (Button) dialog.findViewById(R.id.cancel);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isDismiss = true;
                    videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), new File(Utils.mainPlayList.get(Utils.playPosition)).getName(), VideoStd.SCREEN_WINDOW_FULLSCREEN);
                    VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                    videoView.startVideo();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                                MediaManager.seekTo(dataLength);
                                MediaManager.start();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
//                    MediaManager.seekTo(dataLength);
                    dialog.dismiss();
//                    MediaManager.start();
//                    visibilityofad();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isDismiss = true;
                    videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), new File(Utils.mainPlayList.get(Utils.playPosition)).getName(), VideoStd.SCREEN_WINDOW_FULLSCREEN);
                    VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                    videoView.startVideo();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//                    MediaManager.seekTo(0);
                    dialog.dismiss();
//                    MediaManager.start();
                    visibilityofad();
                }
            });
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                   /* MediaManager.start();
                    visibilityofad();*/
                    if (!isDismiss) {
                        Intent intent;
                        if (playid.equals("two")) {
                            intent = new Intent(VideoPlayActivity.this, WhatsappStatusActivity.class);
                        } else if (playid.equals("three")) {
                            intent = new Intent(VideoPlayActivity.this, HomeActivity.class);
                        } else if (playid.equals("four")) {
                            intent = new Intent(VideoPlayActivity.this, AllVideoActivity.class);
                        } else {
//                folder = preferences.getString("folder", folder);
                            intent = new Intent(VideoPlayActivity.this, VideoListActivity.class);
                            intent.putExtra("foldername", folder);
                        }
                        startActivity(intent);
                        finish();
                    }
                }
            });
            dialog.setCanceledOnTouchOutside(false);

            dialog.show();
        }

        public void infoDialog(Activity activity) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_info);
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.border_bg);
            TextView txttitle, txtfile, txtlocation, txtlength, txtsize, txtdate, txtresolution;
            txttitle = (TextView) dialog.findViewById(R.id.txttitle);
            txtfile = (TextView) dialog.findViewById(R.id.filename);
            txtlocation = (TextView) dialog.findViewById(R.id.location);
            txtlength = (TextView) dialog.findViewById(R.id.length);
            txtsize = (TextView) dialog.findViewById(R.id.size);
            txtdate = (TextView) dialog.findViewById(R.id.date);
            txtresolution = (TextView) dialog.findViewById(R.id.resolution);
            txttitle.setTextColor(Color.LTGRAY);
            txttitle.setText("Video Info");
            txtfile.setText(vidFile);
            txtlocation.setText(vidLocation);
            txtlength.setText(vidLength);
            txtsize.setText(vidSize);
            txtdate.setText(vidDate);
            txtresolution.setText(vidResolution);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    MediaManager.start();
                }
            });
            dialog.show();
        }
    }


    class MyUserActionInterfaceStd implements UserActionInterfaceStd {

        @Override
        public void onEvent(int type, Object url, int screen, Object... objects) {
            switch (type) {
                case UserActionInterface.ON_CLICK_START_ICON:
                    Log.i("USER_EVENT", "ON_CLICK_START_ICON" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_CLICK_START_ERROR:
                    Log.i("USER_EVENT", "ON_CLICK_START_ERROR" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_CLICK_START_AUTO_COMPLETE:
                    Log.i("USER_EVENT", "ON_CLICK_START_AUTO_COMPLETE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_CLICK_PAUSE:
                    Log.i("USER_EVENT", "ON_CLICK_PAUSE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_CLICK_RESUME:
                    Log.i("USER_EVENT", "ON_CLICK_RESUME" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_SEEK_POSITION:
                    Log.i("USER_EVENT", "ON_SEEK_POSITION" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_AUTO_COMPLETE:
                    Log.i("USER_EVENT", "ON_AUTO_COMPLETE" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);

                    if (pos == Utils.listSize - 1) {
                        myDb.updateData((String) url, 0);
                        onBackPressed();
                    } else {
                        pos++;
                        myDb.updateData((String) url, 0);
                        Utils.playPosition = pos;
                        Log.d("position", "pos" + pos);
                        Log.d("position", "size" + Utils.listSize);
                        String vidurl = (String) Utils.mainPlayList.get(pos);
                        VideoPlayActivity.vidurl = vidurl;
                        /*String vidname = new File(vidurl).getName();
                        videoView.setUp(vidurl, vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);*/
                        Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                        /*VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                        videoView.startVideo();*/
                        getData(vidurl);
                        if (dataLength != 0) {
                            playResume();
                        } else {
                            String vidname = new File(Utils.mainPlayList.get(Utils.playPosition)).getName();
                            videoView.setUp(Utils.mainPlayList.get(Utils.playPosition), vidname, VideoStd.SCREEN_WINDOW_FULLSCREEN);
                            Glide.with(VideoPlayActivity.this).load(Utils.mainPlayList.get(Utils.playPosition)).into(videoView.thumbImageView);
                            VideoLayout.setJzUserAction(new MyUserActionInterfaceStd());
                            videoView.startVideo();
                        }
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }

                    break;
                case UserActionInterface.ON_ENTER_FULLSCREEN:
                    Log.i("USER_EVENT", "ON_ENTER_FULLSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_QUIT_FULLSCREEN:
                    Log.i("USER_EVENT", "ON_QUIT_FULLSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_ENTER_TINYSCREEN:
                    Log.i("USER_EVENT", "ON_ENTER_TINYSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_QUIT_TINYSCREEN:
                    Log.i("USER_EVENT", "ON_QUIT_TINYSCREEN" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_TOUCH_SCREEN_SEEK_VOLUME:
                    Log.i("USER_EVENT", "ON_TOUCH_SCREEN_SEEK_VOLUME" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterface.ON_TOUCH_SCREEN_SEEK_POSITION:
                    Log.i("USER_EVENT", "ON_TOUCH_SCREEN_SEEK_POSITION" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterfaceStd.ON_CLICK_START_THUMB:
                    Log.i("USER_EVENT", "ON_CLICK_START_THUMB" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                case UserActionInterfaceStd.ON_CLICK_BLANK:
                    Log.i("USER_EVENT", "ON_CLICK_BLANK" + " title is : " + (objects.length == 0 ? "" : objects[0]) + " url is : " + url + " screen is : " + screen);
                    break;
                default:
                    Log.i("USER_EVENT", "unknow");
                    break;
            }
        }
    }

}
