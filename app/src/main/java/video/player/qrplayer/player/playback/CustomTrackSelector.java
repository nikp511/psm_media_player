package video.player.qrplayer.player.playback;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Assertions;

public class CustomTrackSelector extends DefaultTrackSelector {
    private static final int WITHIN_RENDERER_CAPABILITIES_BONUS = 1000;

    private String preferredTextLanguage;

    public CustomTrackSelector(TrackSelection.Factory adaptiveTrackSelectionFactory) {
        super(adaptiveTrackSelectionFactory);
    }


    public void setPreferredTextLanguage(@NonNull final String label) {
        Assertions.checkNotNull(label);
        if (!label.equals(preferredTextLanguage)) {
            preferredTextLanguage = label;
            invalidate();
        }
    }

    protected static boolean formatHasLanguage(Format format, String language) {
        return language != null && TextUtils.equals(language, format.language);
    }

    protected static boolean formatHasNoLanguage(Format format) {
        return TextUtils.isEmpty(format.language) || formatHasLanguage(format, C.LANGUAGE_UNDETERMINED);
    }

    @Override
    protected Pair<TrackSelection, Integer> selectTextTrack(TrackGroupArray groups, int[][] formatSupport,
                                                            Parameters params) {
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex],
                        params.exceedRendererCapabilitiesIfNecessary)) {
                    Format format = trackGroup.getFormat(trackIndex);
                    int maskedSelectionFlags =
                            format.selectionFlags & ~params.disabledTextTrackSelectionFlags;
                    boolean isDefault = (maskedSelectionFlags & C.SELECTION_FLAG_DEFAULT) != 0;
                    boolean isForced = (maskedSelectionFlags & C.SELECTION_FLAG_FORCED) != 0;
                    int trackScore;
                    boolean preferredLanguageFound = formatHasLanguage(format, preferredTextLanguage);
                    if (preferredLanguageFound
                            || (params.selectUndeterminedTextLanguage && formatHasNoLanguage(format))) {
                        if (isDefault) {
                            trackScore = 8;
                        } else if (!isForced) {
                            trackScore = 6;
                        } else {
                            trackScore = 4;
                        }
                        trackScore += preferredLanguageFound ? 1 : 0;
                    } else if (isDefault) {
                        trackScore = 3;
                    } else if (isForced) {
                        if (formatHasLanguage(format, params.preferredAudioLanguage)) {
                            trackScore = 2;
                        } else {
                            trackScore = 1;
                        }
                    } else {
                        // Track should not be selected.
                        continue;
                    }
                    if (isSupported(trackFormatSupport[trackIndex], false)) {
                        trackScore += WITHIN_RENDERER_CAPABILITIES_BONUS;
                    }
                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        return selectedGroup == null
                ? null
                : Pair.create(
                new FixedTrackSelection(selectedGroup, selectedTrackIndex), selectedTrackScore);
    }
}