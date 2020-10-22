package video.player.qrplayer.util;

import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.schabi.newpipe.extractor.Info;
import org.schabi.newpipe.extractor.InfoItem;

import java.util.Map;

import video.player.qrplayer.activity.HomeActivity;


public final class InfoCache {
    private static final boolean DEBUG = HomeActivity.DEBUG;
    private static final InfoCache instance = new InfoCache();
    private static final int MAX_ITEMS_ON_CACHE = 60;
    /**
     * Trim the cache to this size
     */
    private static final int TRIM_CACHE_TO = 30;
    private static final LruCache<String, CacheData> lruCache = new LruCache<>(MAX_ITEMS_ON_CACHE);
    private final String TAG = getClass().getSimpleName();

    private InfoCache() {
        //no instance
    }

    public static InfoCache getInstance() {
        return instance;
    }

    @NonNull
    private static String keyOf(final int serviceId, @NonNull final String url, @NonNull InfoItem.InfoType infoType) {
        return serviceId + url + infoType.toString();
    }

    private static void removeStaleCache() {
        for (Map.Entry<String, CacheData> entry : InfoCache.lruCache.snapshot().entrySet()) {
            final CacheData data = entry.getValue();
            if (data != null && data.isExpired()) {
                InfoCache.lruCache.remove(entry.getKey());
            }
        }
    }

    @Nullable
    private static Info getInfo(@NonNull final String key) {
        final CacheData data = InfoCache.lruCache.get(key);
        if (data == null) return null;

        if (data.isExpired()) {
            InfoCache.lruCache.remove(key);
            return null;
        }

        return data.info;
    }

    @Nullable
    public Info getFromKey(int serviceId, @NonNull String url, @NonNull InfoItem.InfoType infoType) {
        if (DEBUG)
            Log.d(TAG, "getFromKey() called with: serviceId = [" + serviceId + "], url = [" + url + "]");
        synchronized (lruCache) {
            return getInfo(keyOf(serviceId, url, infoType));
        }
    }

    public void putInfo(int serviceId, @NonNull String url, @NonNull Info info, @NonNull InfoItem.InfoType infoType) {
        if (DEBUG) Log.d(TAG, "putInfo() called with: info = [" + info + "]");

        final long expirationMillis = ServiceHelper.getCacheExpirationMillis(info.getServiceId());
        synchronized (lruCache) {
            final CacheData data = new CacheData(info, expirationMillis);
            lruCache.put(keyOf(serviceId, url, infoType), data);
        }
    }

    public void removeInfo(int serviceId, @NonNull String url, @NonNull InfoItem.InfoType infoType) {
        if (DEBUG)
            Log.d(TAG, "removeInfo() called with: serviceId = [" + serviceId + "], url = [" + url + "]");
        synchronized (lruCache) {
            lruCache.remove(keyOf(serviceId, url, infoType));
        }
    }

    public void clearCache() {
        if (DEBUG) Log.d(TAG, "clearCache() called");
        synchronized (lruCache) {
            lruCache.evictAll();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void trimCache() {
        if (DEBUG) Log.d(TAG, "trimCache() called");
        synchronized (lruCache) {
            removeStaleCache();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                lruCache.trimToSize(TRIM_CACHE_TO);
            }
        }
    }

    public long getSize() {
        synchronized (lruCache) {
            return lruCache.size();
        }
    }

    final private static class CacheData {
        final private long expireTimestamp;
        final private Info info;

        private CacheData(@NonNull final Info info, final long timeoutMillis) {
            this.expireTimestamp = System.currentTimeMillis() + timeoutMillis;
            this.info = info;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expireTimestamp;
        }
    }
}
