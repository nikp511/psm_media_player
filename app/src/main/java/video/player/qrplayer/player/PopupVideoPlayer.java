package video.player.qrplayer.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import video.player.qrplayer.BuildConfig;
import video.player.qrplayer.R;
import video.player.qrplayer.activity.VideoPlayActivity;
import video.player.qrplayer.player.event.PlayerEventListener;
import video.player.qrplayer.player.helper.LockManager;
import video.player.qrplayer.player.helper.PlayerHelper;
import video.player.qrplayer.player.resolver.MediaSourceTag;
import video.player.qrplayer.player.resolver.VideoPlaybackResolver;
import video.player.qrplayer.util.ListHelper;
import video.player.qrplayer.util.ThemeHelper;
import video.player.qrplayer.utils.Utils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.nostra13.universalimageloader.core.assist.FailReason;

import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.File;
import java.util.List;

import video.player.qrplayer.util.AnimationUtils;

import static video.player.qrplayer.util.AnimationUtils.animateView;

public final class PopupVideoPlayer extends Service {
    public static final String ACTION_CLOSE = "PopupVideoPlayer.CLOSE";
    public static final String ACTION_PLAY_PAUSE = "PopupVideoPlayer.PLAY_PAUSE";
    public static final String ACTION_REPEAT = "PopupVideoPlayer.REPEAT";
    private static final String TAG = ".PopupVideoPlayer";
    private static final boolean DEBUG = BasePlayer.DEBUG;
    private static final int NOTIFICATION_ID = 40028922;
    private static final String POPUP_SAVED_WIDTH = "popup_saved_width";
    private static final String POPUP_SAVED_X = "popup_saved_x";
    private static final String POPUP_SAVED_Y = "popup_saved_y";

    private static final int MINIMUM_SHOW_EXTRA_WIDTH_DP = 300;

    private static final int IDLE_WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
    private static final int ONGOING_PLAYBACK_WINDOW_FLAGS = IDLE_WINDOW_FLAGS |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    int codeOreo = Build.VERSION_CODES.O;
    int codeCurrent = Build.VERSION.SDK_INT;
    private WindowManager windowManager;
    private WindowManager.LayoutParams popupLayoutParams;
    private GestureDetector popupGestureDetector;
    private View closeOverlayView;
    private FloatingActionButton closeOverlayButton;
    private int tossFlingVelocity;
    private float screenWidth, screenHeight;
    private float popupWidth, popupHeight;
    private float minimumWidth, minimumHeight;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notBuilder;
    private RemoteViews notRemoteView;
    private float maximumWidth, maximumHeight;
    private VideoPlayerImpl playerImpl;
    private LockManager lockManager;
    private boolean isPopupClosing = false;
    private PlayerEventListener activityListener;
    private IBinder mBinder;

    @Override
    public void onCreate() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (codeCurrent < codeOreo)
            notificationManager = ((NotificationManager) getSystemService(NOTIFICATION_SERVICE));
        lockManager = new LockManager(this);
        playerImpl = new VideoPlayerImpl(this);
        ThemeHelper.setTheme(this);

        mBinder = new PlayerServiceBinder(playerImpl);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (DEBUG)
            Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        if (playerImpl.getPlayer() == null) {
            initPopup();
            initPopupCloseOverlay();
        }
        if (!playerImpl.isPlaying()) playerImpl.getPlayer().setPlayWhenReady(true);

        playerImpl.handleIntent(intent);

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged() called with: newConfig = [" + newConfig + "]");
        updateScreenSize();
        updatePopupSize(popupLayoutParams.width, -1);
        checkPopupPositionBounds();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy() called");
        closePopup();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AudioServiceLeakFix.preventLeakOf(base));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @SuppressLint("RtlHardcoded")
    private void initPopup() {
        if (DEBUG) Log.d(TAG, "initPopup() called");
        View rootView = View.inflate(this, R.layout.player_popup, null);
        playerImpl.setup(rootView);

        tossFlingVelocity = PlayerHelper.getTossFlingVelocity(this);

        updateScreenSize();

        final boolean popupRememberSizeAndPos = PlayerHelper.isRememberingPopupDimensions(this);
        final float defaultSize = getResources().getDimension(R.dimen.popup_default_width);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        popupWidth = popupRememberSizeAndPos ? sharedPreferences.getFloat(POPUP_SAVED_WIDTH, defaultSize) : defaultSize;

        final int layoutParamType = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_PHONE :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        popupLayoutParams = new WindowManager.LayoutParams(
                (int) popupWidth, (int) getMinimumVideoHeight(popupWidth),
                layoutParamType,
                IDLE_WINDOW_FLAGS,
                PixelFormat.TRANSLUCENT);
        popupLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        popupLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        int centerX = (int) (screenWidth / 2f - popupWidth / 2f);
        int centerY = (int) (screenHeight / 2f - popupHeight / 2f);
        popupLayoutParams.x = popupRememberSizeAndPos ? sharedPreferences.getInt(POPUP_SAVED_X, centerX) : centerX;
        popupLayoutParams.y = popupRememberSizeAndPos ? sharedPreferences.getInt(POPUP_SAVED_Y, centerY) : centerY;

        checkPopupPositionBounds();

        PopupWindowGestureListener listener = new PopupWindowGestureListener();
        popupGestureDetector = new GestureDetector(this, listener);
        rootView.setOnTouchListener(listener);

        playerImpl.getLoadingPanel().setMinimumWidth(popupLayoutParams.width);
        playerImpl.getLoadingPanel().setMinimumHeight(popupLayoutParams.height);
        windowManager.addView(rootView, popupLayoutParams);
    }

    @SuppressLint({"RtlHardcoded", "RestrictedApi"})
    private void initPopupCloseOverlay() {
        if (DEBUG) Log.d(TAG, "initPopupCloseOverlay() called");
        closeOverlayView = View.inflate(this, R.layout.player_popup_close_overlay, null);
        closeOverlayButton = closeOverlayView.findViewById(R.id.closeButton);

        final int layoutParamType = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_PHONE :
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        final int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

        WindowManager.LayoutParams closeOverlayLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                layoutParamType,
                flags,
                PixelFormat.TRANSLUCENT);
        closeOverlayLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        closeOverlayLayoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        closeOverlayButton.setVisibility(View.GONE);
        windowManager.addView(closeOverlayView, closeOverlayLayoutParams);
    }

    private void resetNotification() {
        if (codeCurrent < codeOreo)
            notBuilder = createNotification();
    }

    private NotificationCompat.Builder createNotification() {
        notRemoteView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.player_popup_notification);

        notRemoteView.setTextViewText(R.id.notificationSongName, new File(VideoPlayActivity.vidurl).getName());
        String folderName = new File(VideoPlayActivity.vidurl).getParent();
        notRemoteView.setTextViewText(R.id.notificationArtist, new File(folderName).getName());
        Glide.with(this)
                .asBitmap()
                .load(VideoPlayActivity.vidurl)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        notRemoteView.setImageViewBitmap(R.id.notificationCover, resource);
                        notificationManager.notify(NOTIFICATION_ID, notBuilder.build());
                    }
                });


        notRemoteView.setOnClickPendingIntent(R.id.notificationPlayPause,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_PLAY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT));
        notRemoteView.setOnClickPendingIntent(R.id.notificationStop,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_CLOSE), PendingIntent.FLAG_UPDATE_CURRENT));
        notRemoteView.setOnClickPendingIntent(R.id.notificationRepeat,
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, new Intent(ACTION_REPEAT), PendingIntent.FLAG_UPDATE_CURRENT));

        // Starts popup player activity -- attempts to unlock lockscreen
//        final Intent intent = NavigationHelper.getPopupPlayerActivityIntent(this);
//        notRemoteView.setOnClickPendingIntent(R.id.notificationContent,
//                PendingIntent.getActivity(this, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        setRepeatModeRemote(notRemoteView, playerImpl.getRepeatMode());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setOngoing(true)
                .setSmallIcon(R.drawable.notfication_play)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContent(notRemoteView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
        }
        return builder;
    }

    private void updateNotification(int drawableId) {

        if (codeCurrent < codeOreo) {
            if (DEBUG)
                Log.d(TAG, "updateNotification() called with: drawableId = [" + drawableId + "]");
            if (notBuilder == null || notRemoteView == null) return;
            if (drawableId != -1)
                notRemoteView.setImageViewResource(R.id.notificationPlayPause, drawableId);
            notificationManager.notify(NOTIFICATION_ID, notBuilder.build());
        }

    }

    public void closePopup() {
        if (DEBUG) Log.d(TAG, "closePopup() called, isPopupClosing = " + isPopupClosing);
        if (isPopupClosing) return;
        isPopupClosing = true;
        Utils.isPopup = false;

        if (playerImpl != null) {
            if (playerImpl.getRootView() != null) {
                windowManager.removeView(playerImpl.getRootView());
            }
            playerImpl.setRootView(null);
            playerImpl.stopActivityBinding();
            playerImpl.destroy();
            playerImpl = null;
        }

        mBinder = null;
        if (lockManager != null) lockManager.releaseWifiAndCpu();
        if (codeCurrent < codeOreo)
            if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);

        animateOverlayAndFinishService();
    }

    private void animateOverlayAndFinishService() {
        final int targetTranslationY = (int) (closeOverlayButton.getRootView().getHeight() - closeOverlayButton.getY());

        closeOverlayButton.animate().setListener(null).cancel();
        closeOverlayButton.animate()
                .setInterpolator(new AnticipateInterpolator())
                .translationY(targetTranslationY)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        end();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        end();
                    }

                    private void end() {
                        windowManager.removeView(closeOverlayView);

                        stopForeground(true);
                        stopSelf();
                    }
                }).start();
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean checkPopupPositionBounds() {
        return checkPopupPositionBounds(screenWidth, screenHeight);
    }

    private boolean checkPopupPositionBounds(final float boundaryWidth, final float boundaryHeight) {
        if (DEBUG) {
            Log.d(TAG, "checkPopupPositionBounds() called with: boundaryWidth = [" + boundaryWidth + "], boundaryHeight = [" + boundaryHeight + "]");
        }

        if (popupLayoutParams.x < 0) {
            popupLayoutParams.x = 0;
            return true;
        } else if (popupLayoutParams.x > boundaryWidth - popupLayoutParams.width) {
            popupLayoutParams.x = (int) (boundaryWidth - popupLayoutParams.width);
            return true;
        }

        if (popupLayoutParams.y < 0) {
            popupLayoutParams.y = 0;
            return true;
        } else if (popupLayoutParams.y > boundaryHeight - popupLayoutParams.height) {
            popupLayoutParams.y = (int) (boundaryHeight - popupLayoutParams.height);
            return true;
        }

        return false;
    }

    private void savePositionAndSize() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PopupVideoPlayer.this);
        sharedPreferences.edit().putInt(POPUP_SAVED_X, popupLayoutParams.x).apply();
        sharedPreferences.edit().putInt(POPUP_SAVED_Y, popupLayoutParams.y).apply();
        sharedPreferences.edit().putFloat(POPUP_SAVED_WIDTH, popupLayoutParams.width).apply();
    }

    private float getMinimumVideoHeight(float width) {
        //if (DEBUG) Log.d(TAG, "getMinimumVideoHeight() called with: width = [" + width + "], returned: " + height);
        return width / (16.0f / 9.0f); // Respect the 16:9 ratio that most videos have
    }

    private void updateScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        if (DEBUG)
            Log.d(TAG, "updateScreenSize() called > screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);

        popupWidth = getResources().getDimension(R.dimen.popup_default_width);
        popupHeight = getMinimumVideoHeight(popupWidth);

        minimumWidth = getResources().getDimension(R.dimen.popup_minimum_width);
        minimumHeight = getMinimumVideoHeight(minimumWidth);

        maximumWidth = screenWidth;
        maximumHeight = screenHeight;
    }

    private void updatePopupSize(int width, int height) {
        if (playerImpl == null) return;
        if (DEBUG)
            Log.d(TAG, "updatePopupSize() called with: width = [" + width + "], height = [" + height + "]");

        width = (int) (width > maximumWidth ? maximumWidth : width < minimumWidth ? minimumWidth : width);

        if (height == -1) height = (int) getMinimumVideoHeight(width);
        else
            height = (int) (height > maximumHeight ? maximumHeight : height < minimumHeight ? minimumHeight : height);

        popupLayoutParams.width = width;
        popupLayoutParams.height = height;
        popupWidth = width;
        popupHeight = height;

        if (DEBUG)
            Log.d(TAG, "updatePopupSize() updated values:  width = [" + width + "], height = [" + height + "]");
        windowManager.updateViewLayout(playerImpl.getRootView(), popupLayoutParams);
    }

    protected void setRepeatModeRemote(final RemoteViews remoteViews, final int repeatMode) {
        final String methodName = "setImageResource";

        if (remoteViews == null) return;

        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                remoteViews.setInt(R.id.notificationRepeat, methodName, R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                remoteViews.setInt(R.id.notificationRepeat, methodName, R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                remoteViews.setInt(R.id.notificationRepeat, methodName, R.drawable.exo_controls_repeat_all);
                break;
        }
    }

    private void updateWindowFlags(final int flags) {
        if (popupLayoutParams == null || windowManager == null || playerImpl == null) return;

        popupLayoutParams.flags = flags;
        windowManager.updateViewLayout(playerImpl.getRootView(), popupLayoutParams);
    }
    ///////////////////////////////////////////////////////////////////////////

    protected class VideoPlayerImpl extends VideoPlayer implements View.OnLayoutChangeListener {
        private TextView resizingIndicator;
        private ImageButton fullScreenButton;
        private ImageView videoPlayPause;

        private View extraOptionsView;
        private View closingOverlayView;

        VideoPlayerImpl(final Context context) {
            super("VideoPlayerImpl" + PopupVideoPlayer.TAG, context);
        }

        @Override
        public void handleIntent(Intent intent) {
            super.handleIntent(intent);

            resetNotification();
            if (codeCurrent < codeOreo)
                startForeground(NOTIFICATION_ID, notBuilder.build());
        }

        @Override
        public void initViews(View rootView) {
            super.initViews(rootView);
            resizingIndicator = rootView.findViewById(R.id.resizing_indicator);
            fullScreenButton = rootView.findViewById(R.id.fullScreenButton);
            fullScreenButton.setOnClickListener(v -> onFullScreenButtonClicked());
            videoPlayPause = rootView.findViewById(R.id.videoPlayPause);

            extraOptionsView = rootView.findViewById(R.id.extraOptionsView);
            closingOverlayView = rootView.findViewById(R.id.closingOverlay);
            rootView.addOnLayoutChangeListener(this);
        }

        @Override
        public void initListeners() {
            super.initListeners();
            videoPlayPause.setOnClickListener(v -> onPlayPause());
        }

        @Override
        protected void setupSubtitleView(@NonNull SubtitleView view,
                                         final float captionScale,
                                         @NonNull final CaptionStyleCompat captionStyle) {
            float captionRatio = (captionScale - 1f) / 5f + 1f;
            view.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * captionRatio);
            view.setApplyEmbeddedStyles(captionStyle.equals(CaptionStyleCompat.DEFAULT));
            view.setStyle(captionStyle);
        }

        @Override
        public void onLayoutChange(final View view, int left, int top, int right, int bottom,
                                   int oldLeft, int oldTop, int oldRight, int oldBottom) {
            float widthDp = Math.abs(right - left) / getResources().getDisplayMetrics().density;
            final int visibility = widthDp > MINIMUM_SHOW_EXTRA_WIDTH_DP ? View.VISIBLE : View.GONE;
            extraOptionsView.setVisibility(visibility);
        }

        @Override
        public void destroy() {
            if (codeCurrent < codeOreo) {
                if (notRemoteView != null)
                    notRemoteView.setImageViewBitmap(R.id.notificationCover, null);
            }

            super.destroy();
        }

        @Override
        public void onFullScreenButtonClicked() {
            super.onFullScreenButtonClicked();
/*
            if (DEBUG) Log.d(TAG, "onFullScreenButtonClicked() called");

            setRecovery();
            final Intent intent = NavigationHelper.getPlayerIntent(
                    context,
                    MainVideoPlayer.class,
                    this.getPlayQueue(),
                    this.getRepeatMode(),
                    this.getPlaybackSpeed(),
                    this.getPlaybackPitch(),
                    this.getPlaybackSkipSilence(),
                    this.getPlaybackQuality()
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);*/
            Utils.mainPlayList = Utils.popupList;
            Log.e(TAG, "List Size : " + Utils.mainPlayList.size());
            Log.e(TAG, "List Size : " + Utils.mainPlayList);
            Log.e(TAG, "List Size : " + Utils.playPosition);
            Intent intent = new Intent(context, VideoPlayActivity.class);
            intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("playid", "four");
            context.startActivity(intent);
            closePopup();
        }

        @Override
        public void onDismiss(PopupMenu menu) {
            super.onDismiss(menu);
            if (isPlaying()) hideControls(500, 0);
        }

        @Override
        protected int nextResizeMode(int resizeMode) {
            if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                return AspectRatioFrameLayout.RESIZE_MODE_FIT;
            } else {
                return AspectRatioFrameLayout.RESIZE_MODE_FILL;
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            super.onStopTrackingTouch(seekBar);
            if (wasPlaying()) {
                hideControls(100, 0);
            }
        }

        @Override
        public void onShuffleClicked() {
            super.onShuffleClicked();
            updatePlayback();
        }

        @Override
        public void onUpdateProgress(int currentProgress, int duration, int bufferPercent) {
            updateProgress(currentProgress, duration, bufferPercent);
            super.onUpdateProgress(currentProgress, duration, bufferPercent);
        }

        @Override
        protected VideoPlaybackResolver.QualityResolver getQualityResolver() {
            return new VideoPlaybackResolver.QualityResolver() {
                @Override
                public int getDefaultResolutionIndex(List<VideoStream> sortedVideos) {
                    return ListHelper.getPopupDefaultResolutionIndex(context, sortedVideos);
                }

                @Override
                public int getOverrideResolutionIndex(List<VideoStream> sortedVideos,
                                                      String playbackQuality) {
                    return ListHelper.getPopupResolutionIndex(context, sortedVideos,
                            playbackQuality);
                }
            };
        }


        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            super.onLoadingComplete(imageUri, view, loadedImage);
            if (playerImpl == null) return;
            // rebuild notification here since remote view does not release bitmaps,
            // causing memory leaks
            resetNotification();
            updateNotification(-1);
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            super.onLoadingFailed(imageUri, view, failReason);
            resetNotification();
            updateNotification(-1);
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            super.onLoadingCancelled(imageUri, view);
            resetNotification();
            updateNotification(-1);
        }

        void setActivityListener(PlayerEventListener listener) {
            activityListener = listener;
            updateMetadata();
            updatePlayback();
            triggerProgressUpdate();
        }

        void removeActivityListener(PlayerEventListener listener) {
            if (activityListener == listener) {
                activityListener = null;
            }
        }

        private void updateMetadata() {
            if (activityListener != null && getCurrentMetadata() != null) {
                activityListener.onMetadataUpdate(getCurrentMetadata().getMetadata());
            }
        }

        private void updatePlayback() {
            if (activityListener != null && simpleExoPlayer != null && playQueue != null) {
                activityListener.onPlaybackUpdate(currentState, getRepeatMode(),
                        playQueue.isShuffled(), simpleExoPlayer.getPlaybackParameters());
            }
        }

        private void updateProgress(int currentProgress, int duration, int bufferPercent) {
            if (activityListener != null) {
                activityListener.onProgressUpdate(currentProgress, duration, bufferPercent);
            }
        }

        private void stopActivityBinding() {
            if (activityListener != null) {
                activityListener.onServiceStopped();
                activityListener = null;
            }
        }

        @Override
        public void onRepeatModeChanged(int i) {
            super.onRepeatModeChanged(i);
            if (codeCurrent < codeOreo)
                setRepeatModeRemote(notRemoteView, i);
            updatePlayback();
            resetNotification();
            updateNotification(-1);
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            super.onPlaybackParametersChanged(playbackParameters);
            updatePlayback();
        }


        protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {
            super.onMetadataChanged(tag);
            resetNotification();
            updateNotification(-1);
            updateMetadata();
        }

        @Override
        public void onPlaybackShutdown() {
            super.onPlaybackShutdown();
            closePopup();
        }


        @Override
        protected void setupBroadcastReceiver(IntentFilter intentFilter) {
            super.setupBroadcastReceiver(intentFilter);
            if (DEBUG)
                Log.d(TAG, "setupBroadcastReceiver() called with: intentFilter = [" + intentFilter + "]");
            intentFilter.addAction(ACTION_CLOSE);
            intentFilter.addAction(ACTION_PLAY_PAUSE);
            intentFilter.addAction(ACTION_REPEAT);

            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        }

        @Override
        public void onBroadcastReceived(Intent intent) {
            super.onBroadcastReceived(intent);
            if (intent == null || intent.getAction() == null) return;
            if (DEBUG) Log.d(TAG, "onBroadcastReceived() called with: intent = [" + intent + "]");
            switch (intent.getAction()) {
                case ACTION_CLOSE:
                    closePopup();
                    break;
                case ACTION_PLAY_PAUSE:
                    onPlayPause();
                    break;
                case ACTION_REPEAT:
                    onRepeatClicked();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    enableVideoRenderer(true);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    enableVideoRenderer(false);
                    break;
            }
        }

        @Override
        public void changeState(int state) {
            super.changeState(state);
            updatePlayback();
        }

        @Override
        public void onBlocked() {
            super.onBlocked();
            resetNotification();
            updateNotification(R.drawable.ic_play_arrow_white);
        }

        @Override
        public void onPlaying() {
            super.onPlaying();

            updateWindowFlags(ONGOING_PLAYBACK_WINDOW_FLAGS);

            resetNotification();
            updateNotification(R.drawable.ic_pause_white);

            videoPlayPause.setBackgroundResource(R.drawable.ic_pause_white);
            hideControls(DEFAULT_CONTROLS_DURATION, DEFAULT_CONTROLS_HIDE_TIME);
            if (codeCurrent < codeOreo)
                startForeground(NOTIFICATION_ID, notBuilder.build());
            lockManager.acquireWifiAndCpu();
        }

        @Override
        public void onBuffering() {
            super.onBuffering();
            resetNotification();
            updateNotification(R.drawable.ic_play_arrow_white);
        }

        @Override
        public void onPaused() {
            super.onPaused();

            updateWindowFlags(IDLE_WINDOW_FLAGS);

            resetNotification();
            updateNotification(R.drawable.ic_play_arrow_white);

            videoPlayPause.setBackgroundResource(R.drawable.ic_play_arrow_white);
            lockManager.releaseWifiAndCpu();

            stopForeground(false);
        }

        @Override
        public void onPausedSeek() {
            super.onPausedSeek();
            resetNotification();
            updateNotification(R.drawable.ic_play_arrow_white);
            Log.e(TAG, "Pause : ");
            videoPlayPause.setBackgroundResource(R.drawable.ic_pause_white);
        }

        @Override
        public void onCompleted() {
            super.onCompleted();

            updateWindowFlags(IDLE_WINDOW_FLAGS);

            resetNotification();
            updateNotification(R.drawable.ic_replay_white);

            videoPlayPause.setBackgroundResource(R.drawable.ic_replay_white);
            lockManager.releaseWifiAndCpu();

            stopForeground(false);
        }

        @Override
        public void showControlsThenHide() {
            videoPlayPause.setVisibility(View.VISIBLE);
            super.showControlsThenHide();
        }

        public void showControls(long duration) {
            videoPlayPause.setVisibility(View.VISIBLE);
            super.showControls(duration);
        }

        public void hideControls(final long duration, long delay) {
            super.hideControlsAndButton(duration, delay, videoPlayPause);
        }


        void enableVideoRenderer(final boolean enable) {
            final int videoRendererIndex = getRendererIndex(C.TRACK_TYPE_VIDEO);
            if (videoRendererIndex != RENDERER_UNAVAILABLE) {
                trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setRendererDisabled(videoRendererIndex, !enable));
            }
        }

        @SuppressWarnings("WeakerAccess")
        public TextView getResizingIndicator() {
            return resizingIndicator;
        }

        public View getClosingOverlayView() {
            return closingOverlayView;
        }
    }

    private class PopupWindowGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
        private int initialPopupX, initialPopupY;
        private boolean isMoving;
        private boolean isResizing;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (DEBUG)
                Log.d(TAG, "onDoubleTap() called with: e = [" + e + "]" + "rawXy = " + e.getRawX() + ", " + e.getRawY() + ", xy = " + e.getX() + ", " + e.getY());
            if (playerImpl == null || !playerImpl.isPlaying()) return false;

            playerImpl.hideControls(0, 0);

            if (e.getX() > popupWidth / 2) {
                playerImpl.onFastForward();
            } else {
                playerImpl.onFastRewind();
            }

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onSingleTapConfirmed() called with: e = [" + e + "]");
            if (playerImpl == null || playerImpl.getPlayer() == null) return false;
            if (playerImpl.isControlsVisible()) {
                playerImpl.hideControls(100, 100);
            } else {
                playerImpl.showControlsThenHide();

            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onDown() called with: e = [" + e + "]");

            // Fix popup position when the user touch it, it may have the wrong one
            // because the soft input is visible (the draggable area is currently resized).
            checkPopupPositionBounds(closeOverlayView.getWidth(), closeOverlayView.getHeight());

            initialPopupX = popupLayoutParams.x;
            initialPopupY = popupLayoutParams.y;
            popupWidth = popupLayoutParams.width;
            popupHeight = popupLayoutParams.height;
            return super.onDown(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (DEBUG) Log.d(TAG, "onLongPress() called with: e = [" + e + "]");
            updateScreenSize();
            checkPopupPositionBounds();
            updatePopupSize((int) screenWidth, -1);
        }

        @Override
        public boolean onScroll(MotionEvent initialEvent, MotionEvent movingEvent, float distanceX, float distanceY) {
            if (isResizing || playerImpl == null)
                return super.onScroll(initialEvent, movingEvent, distanceX, distanceY);

            if (!isMoving) {
                AnimationUtils.animateView(closeOverlayButton, true, 200);
            }

            isMoving = true;

            float diffX = (int) (movingEvent.getRawX() - initialEvent.getRawX()), posX = (int) (initialPopupX + diffX);
            float diffY = (int) (movingEvent.getRawY() - initialEvent.getRawY()), posY = (int) (initialPopupY + diffY);

            if (posX > (screenWidth - popupWidth)) posX = (int) (screenWidth - popupWidth);
            else if (posX < 0) posX = 0;

            if (posY > (screenHeight - popupHeight)) posY = (int) (screenHeight - popupHeight);
            else if (posY < 0) posY = 0;

            popupLayoutParams.x = (int) posX;
            popupLayoutParams.y = (int) posY;

            final View closingOverlayView = playerImpl.getClosingOverlayView();
            if (isInsideClosingRadius(movingEvent)) {
                if (closingOverlayView.getVisibility() == View.GONE) {
                    AnimationUtils.animateView(closingOverlayView, true, 250);
                }
            } else {
                if (closingOverlayView.getVisibility() == View.VISIBLE) {
                    AnimationUtils.animateView(closingOverlayView, false, 0);
                }
            }

            //noinspection PointlessBooleanExpression
            if (DEBUG && false) {
                Log.d(TAG, "PopupVideoPlayer.onScroll = " +
                        ", e1.getRaw = [" + initialEvent.getRawX() + ", " + initialEvent.getRawY() + "]" + ", e1.getX,Y = [" + initialEvent.getX() + ", " + initialEvent.getY() + "]" +
                        ", e2.getRaw = [" + movingEvent.getRawX() + ", " + movingEvent.getRawY() + "]" + ", e2.getX,Y = [" + movingEvent.getX() + ", " + movingEvent.getY() + "]" +
                        ", distanceX,Y = [" + distanceX + ", " + distanceY + "]" +
                        ", posX,Y = [" + posX + ", " + posY + "]" +
                        ", popupW,H = [" + popupWidth + " x " + popupHeight + "]");
            }
            windowManager.updateViewLayout(playerImpl.getRootView(), popupLayoutParams);
            return true;
        }

        private void onScrollEnd(MotionEvent event) {
            if (DEBUG) Log.d(TAG, "onScrollEnd() called");
            if (playerImpl == null) return;
            if (playerImpl.isControlsVisible() && playerImpl.getCurrentState() == BasePlayer.STATE_PLAYING) {
                playerImpl.hideControls(VideoPlayer.DEFAULT_CONTROLS_DURATION, VideoPlayer.DEFAULT_CONTROLS_HIDE_TIME);
            }

            if (isInsideClosingRadius(event)) {
                closePopup();
            } else {
                AnimationUtils.animateView(playerImpl.getClosingOverlayView(), false, 0);

                if (!isPopupClosing) {
                    AnimationUtils.animateView(closeOverlayButton, false, 200);
                }
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DEBUG) Log.d(TAG, "Fling velocity: dX=[" + velocityX + "], dY=[" + velocityY + "]");
            if (playerImpl == null) return false;

            final float absVelocityX = Math.abs(velocityX);
            final float absVelocityY = Math.abs(velocityY);
            if (Math.max(absVelocityX, absVelocityY) > tossFlingVelocity) {
                if (absVelocityX > tossFlingVelocity) popupLayoutParams.x = (int) velocityX;
                if (absVelocityY > tossFlingVelocity) popupLayoutParams.y = (int) velocityY;
                checkPopupPositionBounds();
                windowManager.updateViewLayout(playerImpl.getRootView(), popupLayoutParams);
                return true;
            }
            return false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            popupGestureDetector.onTouchEvent(event);
            if (playerImpl == null) return false;
            if (event.getPointerCount() == 2 && !isResizing) {
                if (DEBUG) Log.d(TAG, "onTouch() 2 finger pointer detected, enabling resizing.");
                playerImpl.showAndAnimateControl(-1, true);
                playerImpl.getLoadingPanel().setVisibility(View.GONE);

                playerImpl.hideControls(0, 0);
                AnimationUtils.animateView(playerImpl.getCurrentDisplaySeek(), false, 0, 0);
                AnimationUtils.animateView(playerImpl.getResizingIndicator(), true, 200, 0);
                isResizing = true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE && !isMoving && isResizing) {
                if (DEBUG)
                    Log.d(TAG, "onTouch() ACTION_MOVE > v = [" + v + "],  e1.getRaw = [" + event.getRawX() + ", " + event.getRawY() + "]");
                return handleMultiDrag(event);
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (DEBUG)
                    Log.d(TAG, "onTouch() ACTION_UP > v = [" + v + "],  e1.getRaw = [" + event.getRawX() + ", " + event.getRawY() + "]");
                if (isMoving) {
                    isMoving = false;
                    onScrollEnd(event);
                }

                if (isResizing) {
                    isResizing = false;
                    AnimationUtils.animateView(playerImpl.getResizingIndicator(), false, 100, 0);
                    playerImpl.changeState(playerImpl.getCurrentState());
                }

                if (!isPopupClosing) {
                    savePositionAndSize();
                }
            }

            v.performClick();
            return true;
        }

        private boolean handleMultiDrag(final MotionEvent event) {
            if (event.getPointerCount() != 2) return false;

            final float firstPointerX = event.getX(0);
            final float secondPointerX = event.getX(1);

            final float diff = Math.abs(firstPointerX - secondPointerX);
            if (firstPointerX > secondPointerX) {
                // second pointer is the anchor (the leftmost pointer)
                popupLayoutParams.x = (int) (event.getRawX() - diff);
            } else {
                // first pointer is the anchor
                popupLayoutParams.x = (int) event.getRawX();
            }

            checkPopupPositionBounds();
            updateScreenSize();

            final int width = (int) Math.min(screenWidth, diff);
            updatePopupSize(width, -1);

            return true;
        }

        private int distanceFromCloseButton(MotionEvent popupMotionEvent) {
            final int closeOverlayButtonX = closeOverlayButton.getLeft() + closeOverlayButton.getWidth() / 2;
            final int closeOverlayButtonY = closeOverlayButton.getTop() + closeOverlayButton.getHeight() / 2;

            float fingerX = popupLayoutParams.x + popupMotionEvent.getX();
            float fingerY = popupLayoutParams.y + popupMotionEvent.getY();

            return (int) Math.sqrt(Math.pow(closeOverlayButtonX - fingerX, 2) + Math.pow(closeOverlayButtonY - fingerY, 2));
        }

        private float getClosingRadius() {
            final int buttonRadius = closeOverlayButton.getWidth() / 2;
            // 20% wider than the button itself
            return buttonRadius * 1.2f;
        }

        private boolean isInsideClosingRadius(MotionEvent popupMotionEvent) {
            return distanceFromCloseButton(popupMotionEvent) <= getClosingRadius();
        }
    }
}