package video.player.qrplayer.videoplayer.player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;

import java.lang.reflect.Constructor;
import java.util.Timer;
import java.util.TimerTask;

import video.player.qrplayer.R;
//import video.player.qrplayer.activity.HomeActivity;


public abstract class VideoLayout extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final String TAG = "WEB";
    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;

    public static final int SCREEN_WINDOW_NORMAL = 0;
    public static final int SCREEN_WINDOW_LIST = 1;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;

    public static final int CURRENT_STATE_IDLE = -1;
    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PREPARING_CHANGING_URL = 2;
    public static final int CURRENT_STATE_PLAYING = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;//default
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = true;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static int VIDEO_IMAGE_DISPLAY_TYPE = 0;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;
    public static long lastAutoFullscreenTime = 0;
    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        VideoLayout player = VideoMgr.getCurrentJzvd();
                        if (player != null && player.currentState == VideoLayout.CURRENT_STATE_PLAYING) {
                            player.startButton.performClick();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };
    protected static UserActionInterface JZ_USER_EVENT;
    public int currentState = -1;
    public int currentScreen = -1;
    public long seekToInAdvance = 0;
    public ImageView startButton, next, previous, orientation, lockscreen, unlockscreen;
    public AdView mAdView;
    public ImageView imgclose, imgPopup;
    public int lockcode = 0;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public VideoSource VideoSource;
    public int positionInList = -1;
    public int videoRotation = 0;
    public int seekToManulPosition = -1;
    protected Timer UPDATE_PROGRESS_TIMER;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;
    boolean tmp_test_back = false;

    public VideoLayout(Context context) {
        super(context);
        init(context);
    }


    public VideoLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public static void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            Log.d(TAG, "releaseAllVideos");
            VideoMgr.completeAll();
            MediaManager.instance().positionInList = -1;
            MediaManager.instance().releaseMediaPlayer();
        }
    }


    public static boolean backPress() {
        Log.i(TAG, "backPress");
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;

        if (VideoMgr.getSecondFloor() != null) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            if (VideoMgr.getFirstFloor().VideoSource.containsTheUrl(MediaManager.getDataSource().getCurrentUrl())) {
                VideoLayout jzvd = VideoMgr.getSecondFloor();
                jzvd.onEvent(jzvd.currentScreen == SCREEN_WINDOW_FULLSCREEN ?
                        UserActionInterface.ON_QUIT_FULLSCREEN :
                        UserActionInterface.ON_QUIT_TINYSCREEN);
                VideoMgr.getFirstFloor().playOnThisJzvd();
            } else {
                quitFullscreenOrTinyWindow();
            }
            return true;
        } else if (VideoMgr.getFirstFloor() != null &&
                (VideoMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN ||
                        VideoMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_TINY)) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            quitFullscreenOrTinyWindow();
            return true;
        }
        return false;
    }

    public static void quitFullscreenOrTinyWindow() {

        VideoMgr.getFirstFloor().clearFloatScreen();
        MediaManager.instance().releaseMediaPlayer();
        VideoMgr.completeAll();
    }

    @SuppressLint("RestrictedApi")
    public static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST && VideoUtils.getAppCompActivity(context) != null) {
            ActionBar ab = VideoUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            VideoUtils.getWindow(context).clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @SuppressLint("RestrictedApi")
    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST && VideoUtils.getAppCompActivity(context) != null) {
            ActionBar ab = VideoUtils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            VideoUtils.getWindow(context).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void setJzUserAction(UserActionInterface jzUserEvent) {
        JZ_USER_EVENT = jzUserEvent;
    }

    public Object getCurrentUrl() {
        return VideoSource.getCurrentUrl();
    }

    public abstract int getLayoutId();


    public void init(Context context) {
        View.inflate(context, getLayoutId(), this);
        startButton = findViewById(R.id.start);
        fullscreenButton = findViewById(R.id.fullscreen);
        progressBar = findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = findViewById(R.id.current);
        totalTimeTextView = findViewById(R.id.total);
        bottomContainer = findViewById(R.id.layout_bottom);
        textureViewContainer = findViewById(R.id.surface_container);
        topContainer = findViewById(R.id.layout_top);
        next = findViewById(R.id.next);
        mAdView = findViewById(R.id.adView);
        imgclose = findViewById(R.id.close);
        imgPopup = findViewById(R.id.popup);
   //     AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
      //  mAdView.setAdListener(new AdListener() {
        //    @Override
        //    public void onAdClosed() {
        //        AdRequest adRequest = new AdRequest.Builder().build();
          //      mAdView.loadAd(adRequest);
        //    }
       // });

        imgclose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdView.setVisibility(INVISIBLE);
                imgclose.setVisibility(INVISIBLE);
            }
        });

        previous = findViewById(R.id.previous);
        orientation = findViewById(R.id.orientation);
        lockscreen = findViewById(R.id.lockscreen);
        unlockscreen = findViewById(R.id.unlockscreen);

        unlockscreen.setOnClickListener(this);
        lockscreen.setOnClickListener(this);
        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        try {
            if (isCurrentPlay()) {
                NORMAL_ORIENTATION = ((AppCompatActivity) context).getRequestedOrientation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUp(String url, String title, int screen) {
        setUp(new VideoSource(url, title), screen);
    }

    public void setUp(VideoSource VideoSource, int screen) {
        if (this.VideoSource != null && VideoSource.getCurrentUrl() != null &&
                this.VideoSource.containsTheUrl(VideoSource.getCurrentUrl())) {
            return;
        }
        if (isCurrentJZVD() && VideoSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            long position = 0;
            try {
                position = MediaManager.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            if (position != 0) {
                VideoUtils.saveProgress(getContext(), MediaManager.getCurrentUrl(), position);
            }
            MediaManager.instance().releaseMediaPlayer();
        } else if (isCurrentJZVD() && !VideoSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            startWindowTiny();
        } else if (!isCurrentJZVD() && VideoSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            if (VideoMgr.getCurrentJzvd() != null &&
                    VideoMgr.getCurrentJzvd().currentScreen == VideoLayout.SCREEN_WINDOW_TINY) {

                tmp_test_back = true;
            }
        } else if (!isCurrentJZVD() && !VideoSource.containsTheUrl(MediaManager.getCurrentUrl())) {
        }
        this.VideoSource = VideoSource;
        this.currentScreen = screen;
        onStateNormal();

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
            if (VideoSource.urlsMap.isEmpty() || VideoSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!VideoSource.getCurrentUrl().toString().startsWith("file") && !
                        VideoSource.getCurrentUrl().toString().startsWith("/") &&
                        !VideoUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startVideo();
                onEvent(UserActionInterface.ON_CLICK_START_ICON);
            } else if (currentState == CURRENT_STATE_PLAYING) {
                onEvent(UserActionInterface.ON_CLICK_PAUSE);
                Log.d(TAG, "pauseVideo [" + this.hashCode() + "] ");
                MediaManager.pause();
                //if (isNetworkConnected()) {
                  //  mAdView.setVisibility(VISIBLE);
                   // imgclose.setVisibility(VISIBLE);
                //}
                onStatePause();
            } else if (currentState == CURRENT_STATE_PAUSE) {
                onEvent(UserActionInterface.ON_CLICK_RESUME);
                MediaManager.start();
               // mAdView.setVisibility(INVISIBLE);
                //if (HomeActivity.mInterstitialAd.isLoaded())
                 //   imgclose.setVisibility(INVISIBLE);
                onStatePlaying();
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onEvent(UserActionInterface.ON_CLICK_START_AUTO_COMPLETE);
                startVideo();
            }
        } else if (i == R.id.fullscreen) {
            Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            } else {
                Log.d(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");
                onEvent(UserActionInterface.ON_ENTER_FULLSCREEN);
                startWindowFullscreen();
            }
        } else if (i == R.id.lockscreen) {
            lockcode = 1;
            bottomContainer.setVisibility(View.INVISIBLE);
            orientation.setVisibility(View.INVISIBLE);
            topContainer.setVisibility(INVISIBLE);
        } else if (i == R.id.unlockscreen) {
            lockcode = 0;
            unlockscreen.setVisibility(View.INVISIBLE);
            bottomContainer.setVisibility(View.VISIBLE);
            orientation.setVisibility(View.VISIBLE);
            topContainer.setVisibility(VISIBLE);
        }
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (lockcode == 0) {
                        Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                        float deltaX = x - mDownX;
                        float deltaY = y - mDownY;
                        float absDeltaX = Math.abs(deltaX);
                        float absDeltaY = Math.abs(deltaY);
                        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                            if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                                if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                    cancelProgressTimer();
                                    if (absDeltaX >= THRESHOLD) {

                                        if (currentState != CURRENT_STATE_ERROR) {
                                            mChangePosition = true;
                                            mGestureDownPosition = getCurrentPositionWhenPlaying();
                                        }
                                    } else {

                                        if (mDownX < mScreenWidth * 0.5f) {
                                            mChangeBrightness = true;
                                            WindowManager.LayoutParams lp = VideoUtils.getWindow(getContext()).getAttributes();
                                            if (lp.screenBrightness < 0) {
                                                try {
                                                    mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                                    Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                                } catch (Settings.SettingNotFoundException e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                mGestureDownBrightness = lp.screenBrightness * 255;
                                                Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                            }
                                        } else {
                                            mChangeVolume = true;
                                            mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                        }
                                    }
                                }
                            }
                        }
                        if (mChangePosition) {
                            long totalTimeDuration = getDuration();
                            mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                            if (mSeekTimePosition > totalTimeDuration)
                                mSeekTimePosition = totalTimeDuration;
                            String seekTime = VideoUtils.stringForTime(mSeekTimePosition);
                            String totalTime = VideoUtils.stringForTime(totalTimeDuration);

                            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                        }
                        if (mChangeVolume) {
                            deltaY = -deltaY;
                            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                            //dialog
                            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                            showVolumeDialog(-deltaY, volumePercent);
                        }

                        if (mChangeBrightness) {
                            deltaY = -deltaY;
                            int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                            WindowManager.LayoutParams params = VideoUtils.getWindow(getContext()).getAttributes();
                            if (((mGestureDownBrightness + deltaV) / 255) >= 1) {
                                params.screenBrightness = 1;
                            } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                                params.screenBrightness = 0.01f;
                            } else {
                                params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                            }
                            VideoUtils.getWindow(getContext()).setAttributes(params);
                            //dialog
                            int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                            showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
                        }
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    if (lockcode == 0) {
                        Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                        mTouchingProgressBar = false;
                        dismissProgressDialog();
                        dismissVolumeDialog();
                        dismissBrightnessDialog();
                        if (mChangePosition) {
                            onEvent(UserActionInterface.ON_TOUCH_SCREEN_SEEK_POSITION);
                            MediaManager.seekTo(mSeekTimePosition);
                            long duration = getDuration();
                            int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                            progressBar.setProgress(progress);
                        }
                        if (mChangeVolume) {
                            onEvent(UserActionInterface.ON_TOUCH_SCREEN_SEEK_VOLUME);
                        }
                        startProgressTimer();
                    }
                    break;
            }
        }
        return false;
    }

    public void startVideo() {
        VideoMgr.completeAll();
        Log.d(TAG, "startVideo [" + this.hashCode() + "] ");
        initTextureView();
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        VideoUtils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MediaManager.setDataSource(VideoSource);
        MediaManager.instance().positionInList = positionInList;
        onStatePreparing();
        VideoMgr.setFirstFloor(this);
    }

    public void onPrepared() {
        Log.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");
        onStatePrepared();
        onStatePlaying();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                onStateNormal();
                break;
            case CURRENT_STATE_PREPARING:
                onStatePreparing();
                break;
            case CURRENT_STATE_PREPARING_CHANGING_URL:
                changeUrl(urlMapIndex, seekToInAdvance);
                break;
            case CURRENT_STATE_PLAYING:
                onStatePlaying();
                break;
            case CURRENT_STATE_PAUSE:
                onStatePause();
                break;
            case CURRENT_STATE_ERROR:
                onStateError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                onStateAutoComplete();
                break;
        }
    }

    public void onStateNormal() {
        Log.i(TAG, "onStateNormal " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_NORMAL;
        cancelProgressTimer();
    }

    public void onStatePreparing() {
        Log.i(TAG, "onStatePreparing " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PREPARING;
        resetProgressAndTime();
    }

    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        VideoSource.currentUrlIndex = urlMapIndex;
        MediaManager.setDataSource(VideoSource);
        MediaManager.instance().prepare();
    }

    public void changeUrl(VideoSource VideoSource, long seekToInAdvance) {
        currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.VideoSource = VideoSource;
        if (VideoMgr.getSecondFloor() != null && VideoMgr.getFirstFloor() != null) {
            VideoMgr.getFirstFloor().VideoSource = VideoSource;
        }
        MediaManager.setDataSource(VideoSource);
        MediaManager.instance().prepare();
    }

    public void onStatePrepared() {
        if (seekToInAdvance != 0) {
            MediaManager.seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            //long position = VideoUtils.getSavedProgress(getContext(), VideoSource.getCurrentUrl());
            long position = VideoUtils.getSavedProgress(getContext(), "");
            if (position != 0) {
                MediaManager.seekTo(position);
            }
        }
    }

    public void onStatePlaying() {
        Log.i(TAG, "onStatePlaying " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PLAYING;
        startProgressTimer();
    }

    public void onStatePause() {
        Log.i(TAG, "onStatePause " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_PAUSE;
        startProgressTimer();
    }

    public void onStateError() {
        Log.i(TAG, "onStateError " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_ERROR;
        cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        Log.i(TAG, "onStateAutoComplete " + " [" + this.hashCode() + "] ");
        currentState = CURRENT_STATE_AUTO_COMPLETE;
        cancelProgressTimer();
        progressBar.setProgress(100);
        currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
        Log.d(TAG, "onInfo what - " + what + " extra - " + extra);
    }

    public void onError(int what, int extra) {
        Log.e(TAG, "onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            if (isCurrentPlay()) {
                MediaManager.instance().releaseMediaPlayer();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN || currentScreen == SCREEN_WINDOW_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void onAutoCompletion() {
        Runtime.getRuntime().gc();
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        onEvent(UserActionInterface.ON_AUTO_COMPLETE);
        dismissVolumeDialog();
        dismissProgressDialog();
        dismissBrightnessDialog();
        MediaManager.instance().releaseMediaPlayer();
        VideoUtils.saveProgress(getContext(), VideoSource.getCurrentUrl(), 0);
    }

    public void onCompletion() {
        Log.i(TAG, "onCompletion " + " [" + this.hashCode() + "] ");
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            VideoUtils.saveProgress(getContext(), VideoSource.getCurrentUrl(), position);
        }
        cancelProgressTimer();
        dismissBrightnessDialog();
        dismissProgressDialog();
        dismissVolumeDialog();
        onStateNormal();
        textureViewContainer.removeView(MediaManager.textureView);
        MediaManager.instance().currentVideoWidth = 0;
        MediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        VideoUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();
        VideoUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);

        if (MediaManager.surface != null) MediaManager.surface.release();
        if (MediaManager.savedSurfaceTexture != null)
            MediaManager.savedSurfaceTexture.release();
        MediaManager.textureView = null;
        MediaManager.savedSurfaceTexture = null;
    }

    public void release() {
        if (VideoSource.getCurrentUrl().equals(MediaManager.getCurrentUrl()) &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {

            if (VideoMgr.getSecondFloor() != null &&
                    VideoMgr.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            } else if (VideoMgr.getSecondFloor() == null && VideoMgr.getFirstFloor() != null &&
                    VideoMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            } else {
                Log.d(TAG, "releaseMediaPlayer [" + this.hashCode() + "]");
                releaseAllVideos();
            }
        }
    }

    public void initTextureView() {
        removeTextureView();
        MediaManager.textureView = new VideoTextureView(getContext());
        MediaManager.textureView.setSurfaceTextureListener(MediaManager.instance());
    }

    public void addTextureView() {
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        textureViewContainer.addView(MediaManager.textureView, layoutParams);
    }

    public void removeTextureView() {
        MediaManager.savedSurfaceTexture = null;
        if (MediaManager.textureView != null && MediaManager.textureView.getParent() != null) {
            ((ViewGroup) MediaManager.textureView.getParent()).removeView(MediaManager.textureView);
        }
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (VideoUtils.scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(R.id.wv_fullscreen_id);
        View oldT = vp.findViewById(R.id.wv_tiny_id);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    public void clearFloatScreen() {
        VideoUtils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        showSupportActionBar(getContext());
        ViewGroup vp = (VideoUtils.scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        VideoLayout fullJzvd = vp.findViewById(R.id.wv_fullscreen_id);
        VideoLayout tinyJzvd = vp.findViewById(R.id.wv_tiny_id);

        if (fullJzvd != null) {
            vp.removeView(fullJzvd);
            if (fullJzvd.textureViewContainer != null)
                fullJzvd.textureViewContainer.removeView(MediaManager.textureView);
        }
        if (tinyJzvd != null) {
            vp.removeView(tinyJzvd);
            if (tinyJzvd.textureViewContainer != null)
                tinyJzvd.textureViewContainer.removeView(MediaManager.textureView);
        }
        VideoMgr.setSecondFloor(null);
    }

    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (MediaManager.textureView != null) {
            if (videoRotation != 0) {
                MediaManager.textureView.setRotation(videoRotation);
            }
            MediaManager.textureView.setVideoSize(MediaManager.instance().currentVideoWidth, MediaManager.instance().currentVideoHeight);
        }
    }

    public void startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
        if (!mTouchingProgressBar) {
            if (seekToManulPosition != -1) {
                if (seekToManulPosition > progress) {
                    return;
                } else {
                    seekToManulPosition = -1;
                }
            } else {
                if (progress != 0) progressBar.setProgress(progress);
            }
        }
        if (position != 0) currentTimeTextView.setText(VideoUtils.stringForTime(position));
        totalTimeTextView.setText(VideoUtils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(VideoUtils.stringForTime(0));
        totalTimeTextView.setText(VideoUtils.stringForTime(0));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;

        if (currentState == CURRENT_STATE_PLAYING ||
                currentState == CURRENT_STATE_PAUSE) {
            try {
                position = MediaManager.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0;
        try {
            duration = MediaManager.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        onEvent(UserActionInterface.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        MediaManager.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {

            long duration = getDuration();
            currentTimeTextView.setText(VideoUtils.stringForTime(progress * duration / 100));
        }
    }

    public void startWindowFullscreen() {
        Log.i(TAG, "startWindowFullscreen " + " [" + this.hashCode() + "] ");
        hideSupportActionBar(getContext());

        ViewGroup vp = (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.wv_fullscreen_id);
        if (old != null) {
            vp.removeView(old);
        }
        textureViewContainer.removeView(MediaManager.textureView);
        try {
            Constructor<VideoLayout> constructor = (Constructor<VideoLayout>) VideoLayout.this.getClass().getConstructor(Context.class);
            VideoLayout jzvd = constructor.newInstance(getContext());
            jzvd.setId(R.id.wv_fullscreen_id);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jzvd, lp);
            jzvd.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
            jzvd.setUp(VideoSource, SCREEN_WINDOW_FULLSCREEN);
            jzvd.setState(currentState);
            jzvd.addTextureView();
            VideoMgr.setSecondFloor(jzvd);
            VideoUtils.setRequestedOrientation(getContext(), FULLSCREEN_ORIENTATION);

            onStateNormal();
            jzvd.progressBar.setSecondaryProgress(progressBar.getSecondaryProgress());
            jzvd.startProgressTimer();
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startWindowTiny() {
        Log.i(TAG, "startWindowTiny " + " [" + this.hashCode() + "] ");
        onEvent(UserActionInterface.ON_ENTER_TINYSCREEN);
        if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR || currentState == CURRENT_STATE_AUTO_COMPLETE)
            return;
        ViewGroup vp = (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.wv_tiny_id);
        if (old != null) {
            vp.removeView(old);
        }
        textureViewContainer.removeView(MediaManager.textureView);

        try {
            Constructor<VideoLayout> constructor = (Constructor<VideoLayout>) VideoLayout.this.getClass().getConstructor(Context.class);
            VideoLayout jzvd = constructor.newInstance(getContext());
            jzvd.setId(R.id.wv_tiny_id);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            vp.addView(jzvd, lp);
            jzvd.setUp(VideoSource, SCREEN_WINDOW_TINY);
            jzvd.setState(currentState);
            jzvd.addTextureView();
            VideoMgr.setSecondFloor(jzvd);
            onStateNormal();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCurrentPlay() {
        return isCurrentJZVD()
                && VideoSource.containsTheUrl(MediaManager.getCurrentUrl());
    }

    public boolean isCurrentJZVD() {
        return VideoMgr.getCurrentJzvd() != null
                && VideoMgr.getCurrentJzvd() == this;
    }

    public void playOnThisJzvd() {
        Log.i(TAG, "playOnThisJzvd " + " [" + this.hashCode() + "] ");

        currentState = VideoMgr.getSecondFloor().currentState;
        clearFloatScreen();

        setState(currentState);
        addTextureView();
    }


    public void onEvent(int type) {
        if (JZ_USER_EVENT != null && isCurrentPlay() && !VideoSource.urlsMap.isEmpty()) {
            JZ_USER_EVENT.onEvent(type, VideoSource.getCurrentUrl(), currentScreen);
        }
    }


    public void onSeekComplete() {

    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }



    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        long position = getCurrentPositionWhenPlaying();
                        long duration = getDuration();
                        int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                        onProgress(progress, position, duration);
                    }
                });
            }
        }
    }

}
