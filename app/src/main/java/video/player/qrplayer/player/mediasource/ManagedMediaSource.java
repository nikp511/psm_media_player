package video.player.qrplayer.player.mediasource;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.source.MediaSource;
import video.player.qrplayer.player.playqueue.PlayQueueItem;

public interface ManagedMediaSource extends MediaSource {

    boolean shouldBeReplacedWith(@NonNull final PlayQueueItem newIdentity,
                                 final boolean isInterruptable);

    boolean isStreamEqual(@NonNull final PlayQueueItem stream);
}
