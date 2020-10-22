package video.player.qrplayer.player.mediasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import video.player.qrplayer.player.playqueue.PlayQueueItem;

public class PlaceholderMediaSource extends BaseMediaSource implements ManagedMediaSource {

    @Override
    public void maybeThrowSourceInfoRefreshError() {}
    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) { return null; }
    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {}
    @Override
    protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {}
    @Override
    protected void releaseSourceInternal() {}

    @Override
    public boolean shouldBeReplacedWith(@NonNull PlayQueueItem newIdentity,
                                        final boolean isInterruptable) {
        return true;
    }

    @Override
    public boolean isStreamEqual(@NonNull PlayQueueItem stream) {
        return false;
    }
}