package video.player.qrplayer.player.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import video.player.qrplayer.player.playqueue.PlayQueueItem;

import org.schabi.newpipe.extractor.stream.StreamInfo;

public interface PlaybackListener {

    boolean isApproachingPlaybackEdge(final long timeToEndMillis);

    void onPlaybackBlock();

    void onPlaybackUnblock(final MediaSource mediaSource);

    void onPlaybackSynchronize(@NonNull final PlayQueueItem item);

    @Nullable
    MediaSource sourceOf(final PlayQueueItem item, final StreamInfo info);

    void onPlaybackShutdown();
}
