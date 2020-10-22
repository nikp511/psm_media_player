package video.player.qrplayer;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import video.player.qrplayer.R;

public class App extends Application {
    protected static final String TAG = App.class.toString();
    @SuppressWarnings("unchecked")
    private static App app;


    public static App getApp() {
        return app;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        initNotificationChannel();
    }


    public void initNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        final String id = getString(R.string.notification_channel_id);
        final CharSequence name = getString(R.string.notification_channel_name);
        final String description = getString(R.string.notification_channel_description);

        final int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(mChannel);

        setUpUpdateNotificationChannel(importance);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void setUpUpdateNotificationChannel(int importance) {

        final String appUpdateId
                = getString(R.string.app_update_notification_channel_id);
        final CharSequence appUpdateName
                = getString(R.string.app_update_notification_channel_name);
        final String appUpdateDescription
                = getString(R.string.app_update_notification_channel_description);

        NotificationChannel appUpdateChannel
                = new NotificationChannel(appUpdateId, appUpdateName, importance);
        appUpdateChannel.setDescription(appUpdateDescription);

        NotificationManager appUpdateNotificationManager
                = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        appUpdateNotificationManager.createNotificationChannel(appUpdateChannel);
    }
}
