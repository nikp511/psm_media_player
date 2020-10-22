package video.player.qrplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import video.player.qrplayer.R;


public class Localization {

    public final static String DOT_SEPARATOR = " • ";

    private Localization() {
    }

    @NonNull
    public static String concatenateStrings(final String... strings) {
        return concatenateStrings(Arrays.asList(strings));
    }

    @NonNull
    public static String concatenateStrings(final List<String> strings) {
        if (strings.isEmpty()) return "";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strings.get(0));

        for (int i = 1; i < strings.size(); i++) {
            final String string = strings.get(i);
            if (!TextUtils.isEmpty(string)) {
                stringBuilder.append(DOT_SEPARATOR).append(strings.get(i));
            }
        }

        return stringBuilder.toString();
    }

    public static org.schabi.newpipe.extractor.utils.Localization getPreferredExtractorLocal(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String languageCode = sp.getString(context.getString(R.string.content_language_key),
                context.getString(R.string.default_language_value));

        String countryCode = sp.getString(context.getString(R.string.content_country_key),
                context.getString(R.string.default_country_value));

        return new org.schabi.newpipe.extractor.utils.Localization(countryCode, languageCode);
    }


    public static String getDurationString(long duration) {
        if (duration < 0) {
            duration = 0;
        }
        String output;
        long days = duration / (24 * 60 * 60L); /* greater than a day */
        duration %= (24 * 60 * 60L);
        long hours = duration / (60 * 60L); /* greater than an hour */
        duration %= (60 * 60L);
        long minutes = duration / 60L;
        long seconds = duration % 60L;

        //handle days
        if (days > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            output = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            output = String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
        return output;
    }
}
