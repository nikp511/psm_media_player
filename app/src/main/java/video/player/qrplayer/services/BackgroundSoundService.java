package video.player.qrplayer.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;

import video.player.qrplayer.R;
import video.player.qrplayer.activity.HomeActivity;
import video.player.qrplayer.activity.VideoPlayActivity;
import video.player.qrplayer.utils.Utils;

public class BackgroundSoundService<webmazumder> extends Service {
    public static final int PRIORITY_MAX = 2;
    private static final String TAG = null;
    public static MediaPlayer player;
    public static Notification customNotification;
    public static long notpos;
    public static Bitmap bitVideoThumb;
    public static int pos = Utils.playPosition;
    public static NotificationManager notificationmanager;
    private static RemoteViews notificationLayout;
    private static int a = 0;
    Intent intent;
    private String CHANNEL_ID = "no";
    private boolean released;

    public IBinder onBind(Intent arg0) {

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();
        player = MediaPlayer.create(this, Uri.parse(VideoPlayActivity.vidurl));
        player.setVolume(100, 100);
        player.seekTo((int) VideoPlayActivity.curpos);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (pos == Utils.listSize - 1) {
                    player.stop();
                } else {
                    pos++;
                    Log.d("position", "pos" + pos);
                    Log.d("position", "size" + Utils.listSize);
                    String vidurl = (String) Utils.mainPlayList.get(pos);
                    VideoPlayActivity.vidurl = vidurl;
                    String vidname = new File(vidurl).getName();
                    player = MediaPlayer.create(getApplicationContext(), Uri.parse(VideoPlayActivity.vidurl));
                    player.setVolume(100, 100);
                    player.start();
                    notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
                    bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
                    notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
                    notificationmanager.notify(0, customNotification);
                }
            }
        });

        //sendNotification("hello","test");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            /*Intent notificationIntent = new Intent(this, VideoListActivity.class);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);


        */
            notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
            notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
            customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notfication_play)
                    /*.setStyle(new NotificationCompat.DecoratedCustomViewStyle())*/
                    .setCustomContentView(notificationLayout)
                    .setPriority(PRIORITY_MAX)
                    .build();
            customNotification.flags = Notification.FLAG_ONGOING_EVENT;

            customNotification.contentView = notificationLayout;
            // customNotification.contentIntent = pendingNotificationIntent;
            customNotification.flags |= Notification.FLAG_NO_CLEAR;
            Intent playIntent = new Intent(this, playPauseButtonListener.class);
            PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0,
                    playIntent, 0);
            Intent forwardIntent = new Intent(this, forwardButtonListener.class);
            PendingIntent pendingForwardIntent = PendingIntent.getBroadcast(this, 0,
                    forwardIntent, 0);
            Intent backwardIntent = new Intent(this, backwardButtonListener.class);
            PendingIntent pendingBackwardIntent = PendingIntent.getBroadcast(this, 0,
                    backwardIntent, 0);
            Intent closeIntent = new Intent(this, closeButtonListener.class);
            PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0,
                    closeIntent, 0);
            notificationLayout.setOnClickPendingIntent(R.id.imageView3, pendingPlayIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView4, pendingForwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView2, pendingBackwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView5, pendingCloseIntent);

            notificationLayout.setImageViewResource(R.id.imageView2, R.drawable.backward);
            notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.pause);
            notificationLayout.setImageViewResource(R.id.imageView4, R.drawable.forward);
            notificationLayout.setImageViewResource(R.id.imageView5, R.drawable.not_close);
            notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
            notificationLayout.setTextColor(R.id.title, Color.WHITE);
            Log.e("bitmap", String.valueOf(bitVideoThumb));
            notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
            notificationmanager.notify(0, customNotification);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
            notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
            customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notfication_play)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationLayout)
                    .setPriority(PRIORITY_MAX)
                    .build();
            customNotification.flags = Notification.FLAG_ONGOING_EVENT;
            Intent notificationIntent = new Intent(this, HomeActivity.class);
            notificationIntent.putExtra("key", "hello");
            PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);
            customNotification.contentView = notificationLayout;
            // customNotification.contentIntent = pendingNotificationIntent;
            customNotification.flags |= Notification.FLAG_NO_CLEAR;
            Intent playIntent = new Intent(this, playPauseButtonListener.class);
            PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0,
                    playIntent, 0);
            Intent forwardIntent = new Intent(this, forwardButtonListener.class);
            PendingIntent pendingForwardIntent = PendingIntent.getBroadcast(this, 0,
                    forwardIntent, 0);
            Intent backwardIntent = new Intent(this, backwardButtonListener.class);
            PendingIntent pendingBackwardIntent = PendingIntent.getBroadcast(this, 0,
                    backwardIntent, 0);
            Intent closeIntent = new Intent(this, closeButtonListener.class);
            PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0,
                    closeIntent, 0);
            notificationLayout.setOnClickPendingIntent(R.id.imageView3, pendingPlayIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView4, pendingForwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView2, pendingBackwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView5, pendingCloseIntent);

            notificationLayout.setImageViewResource(R.id.imageView2, R.drawable.not_backward);
            notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_pause);
            notificationLayout.setImageViewResource(R.id.imageView4, R.drawable.not_forward);
            notificationLayout.setImageViewResource(R.id.imageView5, R.drawable.not_close);
            notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
            notificationLayout.setTextColor(R.id.title, Color.BLACK);
            Log.e("bitmap", String.valueOf(bitVideoThumb));
            notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
            notificationmanager.notify(0, customNotification);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            notificationmanager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
            notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
            customNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notfication_play)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationLayout)
                    .setPriority(PRIORITY_MAX)
                    .setChannelId(CHANNEL_ID)
                    .build();
            customNotification.flags = Notification.FLAG_ONGOING_EVENT;

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "hello", importance);
            customNotification.contentView = notificationLayout;
            // customNotification.contentIntent = pendingNotificationIntent;
            customNotification.flags |= Notification.FLAG_NO_CLEAR;
            Intent playIntent = new Intent(this, playPauseButtonListener.class);
            PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0,
                    playIntent, 0);
            Intent forwardIntent = new Intent(this, forwardButtonListener.class);
            PendingIntent pendingForwardIntent = PendingIntent.getBroadcast(this, 0,
                    forwardIntent, 0);
            Intent backwardIntent = new Intent(this, backwardButtonListener.class);
            PendingIntent pendingBackwardIntent = PendingIntent.getBroadcast(this, 0,
                    backwardIntent, 0);
            Intent closeIntent = new Intent(this, closeButtonListener.class);
            PendingIntent pendingCloseIntent = PendingIntent.getBroadcast(this, 0,
                    closeIntent, 0);
            notificationLayout.setOnClickPendingIntent(R.id.imageView3, pendingPlayIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView4, pendingForwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView2, pendingBackwardIntent);
            notificationLayout.setOnClickPendingIntent(R.id.imageView5, pendingCloseIntent);

            notificationLayout.setImageViewResource(R.id.imageView2, R.drawable.not_backward);
            notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_pause);
            notificationLayout.setImageViewResource(R.id.imageView4, R.drawable.not_forward);
            notificationLayout.setImageViewResource(R.id.imageView5, R.drawable.not_close);
            notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
            notificationLayout.setTextColor(R.id.title, Color.BLACK);
            Log.e("bitmap", String.valueOf(bitVideoThumb));
            notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationmanager.createNotificationChannel(mChannel);
            }
            notificationmanager.notify(0, customNotification);


        }


    }

    private void sendNotification(String title, String messageBody) {

       /* Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 *//* Request code *//*, intent,
                PendingIntent.FLAG_ONE_SHOT);*/

        String channelId = "channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setColor(Color.parseColor("#393838"));
        } else {
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    public Bitmap getBitmapFromPath(String path) {
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path,
                MediaStore.Images.Thumbnails.MINI_KIND);
        return thumb;
    }

    @SuppressLint("WrongConstant")
    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        return 1;
    }

   /* private final static AtomicInteger c = new AtomicInteger(0);

    public static int getID() {
        return c.incrementAndGet();
    }*/

    public void onStart(Intent intent, int startId) {

    }

    public IBinder onUnBind(Intent arg0) {
        // TO DO Auto-generated method
        return null;
    }

    public void onStop() {

    }

    public void onPause() {

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        Intent intent = new Intent(this, BackgroundSoundService.class);
        stopService(intent);
    }

    @Override
    public void onDestroy() {
        player.stop();
        player.release();
        customNotification.flags = Notification.FLAG_ONGOING_EVENT;
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

    }


    @Override
    public void onLowMemory() {

    }

    public static class playPauseButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (a % 2 == 0) {
                try {
                    notpos = player.getCurrentPosition();
                } catch (Exception e) {
                    notpos = 0;
                }
                player.stop();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.play);
                else
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_play);
            } else {
                player.stop();
                player = MediaPlayer.create(context, Uri.parse(VideoPlayActivity.vidurl));
                player.setVolume(100, 100);
                player.seekTo((int) notpos);
                player.start();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.pause);
                else
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_pause);

            }
            a++;
            notificationmanager.notify(0, customNotification);
        }
    }

    public static class forwardButtonListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (pos < Utils.listSize - 1) {
                player.stop();
                a = 0;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.pause);
                else
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_pause);
                pos++;
                String vidurl = (String) Utils.mainPlayList.get(pos);
                VideoPlayActivity.vidurl = vidurl;
                player = MediaPlayer.create(context, Uri.parse(VideoPlayActivity.vidurl));
                player.setVolume(100, 100);
                player.start();
                notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
                bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
                notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
                notificationmanager.notify(0, customNotification);
            }
        }

        public Bitmap getBitmapFromPath(String path) {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Images.Thumbnails.MINI_KIND);
            return thumb;
        }
    }

    public static class backwardButtonListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (pos > 0) {
                player.stop();
                a = 0;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.pause);
                else
                    notificationLayout.setImageViewResource(R.id.imageView3, R.drawable.not_pause);
                pos--;
                String vidurl = (String) Utils.mainPlayList.get(pos);
                VideoPlayActivity.vidurl = vidurl;
                player = MediaPlayer.create(context, Uri.parse(VideoPlayActivity.vidurl));
                player.setVolume(100, 100);
                player.start();
                notificationLayout.setTextViewText(R.id.title, new File(VideoPlayActivity.vidurl).getName());
                bitVideoThumb = getBitmapFromPath(VideoPlayActivity.vidurl);
                notificationLayout.setImageViewBitmap(R.id.custimage, bitVideoThumb);
                notificationmanager.notify(0, customNotification);
            }
        }

        public Bitmap getBitmapFromPath(String path) {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Images.Thumbnails.MINI_KIND);
            return thumb;
        }
    }


    public static class closeButtonListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            Intent i = new Intent(context, BackgroundSoundService.class);
            context.stopService(i);
        }
    }
}