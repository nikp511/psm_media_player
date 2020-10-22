package video.player.qrplayer.player.helper;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import video.player.qrplayer.player.mediasession.MediaSessionCallback;
import video.player.qrplayer.player.mediasession.PlayQueueNavigator;
import video.player.qrplayer.player.mediasession.PlayQueuePlaybackController;

public class MediaSessionManager {
    private static final String TAG = "MediaSessionManager";

    @NonNull
    private final MediaSessionCompat mediaSession;
    @NonNull
    private final MediaSessionConnector sessionConnector;

    public MediaSessionManager(@NonNull final Context context,
                               @NonNull final Player player,
                               @NonNull final MediaSessionCallback callback) {
        this.mediaSession = new MediaSessionCompat(context, TAG);
        this.mediaSession.setActive(true);

        this.sessionConnector = new MediaSessionConnector(mediaSession,
                new PlayQueuePlaybackController(callback));
        this.sessionConnector.setQueueNavigator(new PlayQueueNavigator(mediaSession, callback));
        this.sessionConnector.setPlayer(player, null);
    }

    @Nullable
    @SuppressWarnings("UnusedReturnValue")
    public KeyEvent handleMediaButtonIntent(final Intent intent) {
        return MediaButtonReceiver.handleIntent(mediaSession, intent);
    }

    /**
     * Should be called on player destruction to prevent leakage.
     * */
    public void dispose() {
        this.sessionConnector.setPlayer(null, null);
        this.sessionConnector.setQueueNavigator(null);
        this.mediaSession.setActive(false);
        this.mediaSession.release();
	}
}
