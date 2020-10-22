package video.player.qrplayer.util;

import android.content.Context;
import android.preference.PreferenceManager;
import androidx.annotation.StyleRes;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import video.player.qrplayer.R;

public class ThemeHelper {

    public static void setTheme(Context context) {
        setTheme(context, -1);
    }

    public static void setTheme(Context context, int serviceId) {
        context.setTheme(getThemeForService(context, serviceId));
    }

    public static boolean isLightThemeSelected(Context context) {
        return getSelectedThemeString(context).equals(context.getResources().getString(R.string.light_theme_key));
    }

    @StyleRes
    public static int getThemeForService(Context context, int serviceId) {
        String lightTheme = context.getResources().getString(R.string.light_theme_key);
        String darkTheme = context.getResources().getString(R.string.dark_theme_key);
        String blackTheme = context.getResources().getString(R.string.black_theme_key);

        String selectedTheme = getSelectedThemeString(context);

        int defaultTheme = R.style.DarkTheme;
        if (selectedTheme.equals(lightTheme)) defaultTheme = R.style.LightTheme;
        else if (selectedTheme.equals(blackTheme)) defaultTheme = R.style.BlackTheme;
        else if (selectedTheme.equals(darkTheme)) defaultTheme = R.style.DarkTheme;

        if (serviceId <= -1) {
            return defaultTheme;
        }

        final StreamingService service;
        try {
            service = NewPipe.getService(serviceId);
        } catch (ExtractionException ignored) {
            return defaultTheme;
        }

        String themeName = "DarkTheme";
        if (selectedTheme.equals(lightTheme)) themeName = "LightTheme";
        else if (selectedTheme.equals(blackTheme)) themeName = "BlackTheme";
        else if (selectedTheme.equals(darkTheme)) themeName = "DarkTheme";

        themeName += "." + service.getServiceInfo().getName();
        int resourceId = context
                .getResources()
                .getIdentifier(themeName, "style", context.getPackageName());

        if (resourceId > 0) {
            return resourceId;
        }

        return defaultTheme;
    }

    private static String getSelectedThemeString(Context context) {
        String themeKey = context.getString(R.string.theme_key);
        String defaultTheme = context.getResources().getString(R.string.default_theme_value);
        return PreferenceManager.getDefaultSharedPreferences(context).getString(themeKey, defaultTheme);
    }
}
