package video.player.qrplayer.player.mediasource;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;

public class ManagedMediaSourcePlaylist {
    @NonNull
    private final ConcatenatingMediaSource internalSource;

    public ManagedMediaSourcePlaylist() {
        internalSource = new ConcatenatingMediaSource(false,
                new ShuffleOrder.UnshuffledShuffleOrder(0));
    }

    public int size() {
        return internalSource.getSize();
    }

    @Nullable
    public ManagedMediaSource get(final int index) {
        return (index < 0 || index >= size()) ?
                null : (ManagedMediaSource) internalSource.getMediaSource(index);
    }

    @NonNull
    public ConcatenatingMediaSource getParentMediaSource() {
        return internalSource;
    }

    public synchronized void expand() {
        append(new PlaceholderMediaSource());
    }

    public synchronized void append(@NonNull final ManagedMediaSource source) {
        internalSource.addMediaSource(source);
    }

    public synchronized void remove(final int index) {
        if (index < 0 || index > internalSource.getSize()) return;

        internalSource.removeMediaSource(index);
    }

    public synchronized void move(final int source, final int target) {
        if (source < 0 || target < 0) return;
        if (source >= internalSource.getSize() || target >= internalSource.getSize()) return;

        internalSource.moveMediaSource(source, target);
    }

    public synchronized void invalidate(final int index,
                                        @Nullable final Handler handler,
                                        @Nullable final Runnable finalizingAction) {
        if (get(index) instanceof PlaceholderMediaSource) return;
        update(index, new PlaceholderMediaSource(), handler, finalizingAction);
    }


    public synchronized void update(final int index, @NonNull final ManagedMediaSource source) {
        update(index, source, null,null);
    }

    public synchronized void update(final int index, @NonNull final ManagedMediaSource source,
                                    @Nullable final Handler handler,
                                    @Nullable final Runnable finalizingAction) {
        if (index < 0 || index >= internalSource.getSize()) return;

        internalSource.addMediaSource(index + 1, source);

        internalSource.removeMediaSource(index, handler, finalizingAction);
    }
}
