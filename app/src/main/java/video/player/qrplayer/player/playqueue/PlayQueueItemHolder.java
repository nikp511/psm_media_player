package video.player.qrplayer.player.playqueue;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import video.player.qrplayer.R;

public class PlayQueueItemHolder extends RecyclerView.ViewHolder {

    public final TextView itemVideoTitleView, itemDurationView, itemAdditionalDetailsView;
    public final ImageView itemSelected, itemThumbnailView, itemHandle;

    public final View itemRoot;

    public PlayQueueItemHolder(View v) {
        super(v);
        itemRoot = v.findViewById(R.id.itemRoot);
        itemVideoTitleView = v.findViewById(R.id.itemVideoTitleView);
        itemDurationView = v.findViewById(R.id.itemDurationView);
        itemAdditionalDetailsView = v.findViewById(R.id.itemAdditionalDetails);
        itemSelected = v.findViewById(R.id.itemSelected);
        itemThumbnailView = v.findViewById(R.id.itemThumbnailView);
        itemHandle = v.findViewById(R.id.itemHandle);
    }
}
