package video.player.qrplayer.player.playqueue;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.Collections;

public final class SinglePlayQueue extends PlayQueue {
    public SinglePlayQueue(final StreamInfoItem item) {
        super(0, Collections.singletonList(new PlayQueueItem(item)));
    }

    public SinglePlayQueue(final StreamInfo info) {
        super(0, Collections.singletonList(new PlayQueueItem(info)));
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void fetch() {}
}
