package video.player.qrplayer.util;

import android.util.Log;

import video.player.qrplayer.activity.HomeActivity;

import org.schabi.newpipe.extractor.Info;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.SuggestionExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.comments.CommentsInfo;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

public final class ExtractorHelper {
    private static final String TAG = ExtractorHelper.class.getSimpleName();
    private static final InfoCache cache = InfoCache.getInstance();

    private ExtractorHelper() {
        //no instance
    }

    private static void checkServiceId(int serviceId) {
        if (serviceId == Constants.NO_SERVICE_ID) {
            throw new IllegalArgumentException("serviceId is NO_SERVICE_ID");
        }
    }

    public static Single<SearchInfo> searchFor(final int serviceId,
                                               final String searchString,
                                               final List<String> contentFilter,
                                               final String sortFilter) {
        checkServiceId(serviceId);
        return Single.fromCallable(() ->
                SearchInfo.getInfo(NewPipe.getService(serviceId),
                        NewPipe.getService(serviceId)
                                .getSearchQHFactory()
                                .fromQuery(searchString, contentFilter, sortFilter)));
    }

    public static Single<InfoItemsPage> getMoreSearchItems(final int serviceId,
                                                           final String searchString,
                                                           final List<String> contentFilter,
                                                           final String sortFilter,
                                                           final String pageUrl) {
        checkServiceId(serviceId);
        return Single.fromCallable(() ->
                SearchInfo.getMoreItems(NewPipe.getService(serviceId),
                        NewPipe.getService(serviceId)
                                .getSearchQHFactory()
                                .fromQuery(searchString, contentFilter, sortFilter),
                        pageUrl));

    }

    public static Single<List<String>> suggestionsFor(final int serviceId,
                                                      final String query) {
        checkServiceId(serviceId);
        return Single.fromCallable(() -> {
            SuggestionExtractor extractor = NewPipe.getService(serviceId)
                    .getSuggestionExtractor();
            return extractor != null
                    ? extractor.suggestionList(query)
                    : Collections.emptyList();
        });
    }

    public static Single<StreamInfo> getStreamInfo(final int serviceId,
                                                   final String url,
                                                   boolean forceLoad) {
        checkServiceId(serviceId);
        return checkCache(forceLoad, serviceId, url, InfoItem.InfoType.STREAM, Single.fromCallable(() ->
                StreamInfo.getInfo(NewPipe.getService(serviceId), url)));
    }

    public static Single<ChannelInfo> getChannelInfo(final int serviceId,
                                                     final String url,
                                                     boolean forceLoad) {
        checkServiceId(serviceId);
        return checkCache(forceLoad, serviceId, url, InfoItem.InfoType.CHANNEL, Single.fromCallable(() ->
                ChannelInfo.getInfo(NewPipe.getService(serviceId), url)));
    }

    public static Single<InfoItemsPage> getMoreChannelItems(final int serviceId,
                                                            final String url,
                                                            final String nextStreamsUrl) {
        checkServiceId(serviceId);
        return Single.fromCallable(() ->
                ChannelInfo.getMoreItems(NewPipe.getService(serviceId), url, nextStreamsUrl));
    }

    public static Single<CommentsInfo> getCommentsInfo(final int serviceId,
                                                       final String url,
                                                       boolean forceLoad) {
        checkServiceId(serviceId);
        return checkCache(forceLoad, serviceId, url, InfoItem.InfoType.COMMENT, Single.fromCallable(() ->
                CommentsInfo.getInfo(NewPipe.getService(serviceId), url)));
    }

    public static Single<InfoItemsPage> getMoreCommentItems(final int serviceId,
                                                            final CommentsInfo info,
                                                            final String nextPageUrl) {
        checkServiceId(serviceId);
        return Single.fromCallable(() ->
                CommentsInfo.getMoreItems(NewPipe.getService(serviceId), info, nextPageUrl));
    }

    public static Single<PlaylistInfo> getPlaylistInfo(final int serviceId,
                                                       final String url,
                                                       boolean forceLoad) {
        checkServiceId(serviceId);
        return checkCache(forceLoad, serviceId, url, InfoItem.InfoType.PLAYLIST, Single.fromCallable(() ->
                PlaylistInfo.getInfo(NewPipe.getService(serviceId), url)));
    }

    public static Single<InfoItemsPage> getMorePlaylistItems(final int serviceId,
                                                             final String url,
                                                             final String nextStreamsUrl) {
        checkServiceId(serviceId);
        return Single.fromCallable(() ->
                PlaylistInfo.getMoreItems(NewPipe.getService(serviceId), url, nextStreamsUrl));
    }

    public static Single<KioskInfo> getKioskInfo(final int serviceId,
                                                 final String url,
                                                 boolean forceLoad) {
        return checkCache(forceLoad, serviceId, url, InfoItem.InfoType.PLAYLIST, Single.fromCallable(() ->
                KioskInfo.getInfo(NewPipe.getService(serviceId), url)));
    }

    public static Single<InfoItemsPage> getMoreKioskItems(final int serviceId,
                                                          final String url,
                                                          final String nextStreamsUrl) {
        return Single.fromCallable(() ->
                KioskInfo.getMoreItems(NewPipe.getService(serviceId),
                        url, nextStreamsUrl));
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    /**
     * Check if we can load it from the cache (forceLoad parameter), if we can't,
     * load from the network (Single loadFromNetwork)
     * and put the results in the cache.
     */
    private static <I extends Info> Single<I> checkCache(boolean forceLoad,
                                                         int serviceId,
                                                         String url,
                                                         InfoItem.InfoType infoType,
                                                         Single<I> loadFromNetwork) {
        checkServiceId(serviceId);
        loadFromNetwork = loadFromNetwork.doOnSuccess(info -> cache.putInfo(serviceId, url, info, infoType));

        Single<I> load;
        if (forceLoad) {
            cache.removeInfo(serviceId, url, infoType);
            load = loadFromNetwork;
        } else {
            load = Maybe.concat(ExtractorHelper.loadFromCache(serviceId, url, infoType),
                    loadFromNetwork.toMaybe())
                    .firstElement() //Take the first valid
                    .toSingle();
        }

        return load;
    }

    /**
     * Default implementation uses the {@link InfoCache} to get cached results
     */
    public static <I extends Info> Maybe<I> loadFromCache(final int serviceId, final String url, InfoItem.InfoType infoType) {
        checkServiceId(serviceId);
        return Maybe.defer(() -> {
            //noinspection unchecked
            I info = (I) cache.getFromKey(serviceId, url, infoType);
            if (HomeActivity.DEBUG) Log.d(TAG, "loadFromCache() called, info > " + info);

            // Only return info if it's not null (it is cached)
            if (info != null) {
                return Maybe.just(info);
            }

            return Maybe.empty();
        });
    }

    public static boolean hasAssignableCauseThrowable(Throwable throwable,
                                                      Class<?>... causesToCheck) {
        // Check if getCause is not the same as cause (the getCause is already the root),
        // as it will cause a infinite loop if it is
        Throwable cause, getCause = throwable;

        // Check if throwable is a subclass of any of the filtered classes
        final Class throwableClass = throwable.getClass();
        for (Class<?> causesEl : causesToCheck) {
            if (causesEl.isAssignableFrom(throwableClass)) {
                return true;
            }
        }

        // Iteratively checks if the root cause of the throwable is a subclass of the filtered class
        while ((cause = throwable.getCause()) != null && getCause != cause) {
            getCause = cause;
            final Class causeClass = cause.getClass();
            for (Class<?> causesEl : causesToCheck) {
                if (causesEl.isAssignableFrom(causeClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if throwable have the exact cause from one of the causes to check.
     */
    public static boolean hasExactCauseThrowable(Throwable throwable, Class<?>... causesToCheck) {
        // Check if getCause is not the same as cause (the getCause is already the root),
        // as it will cause a infinite loop if it is
        Throwable cause, getCause = throwable;

        for (Class<?> causesEl : causesToCheck) {
            if (throwable.getClass().equals(causesEl)) {
                return true;
            }
        }

        while ((cause = throwable.getCause()) != null && getCause != cause) {
            getCause = cause;
            for (Class<?> causesEl : causesToCheck) {
                if (cause.getClass().equals(causesEl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if throwable have Interrupted* exception as one of its causes.
     */
    public static boolean isInterruptedCaused(Throwable throwable) {
        return ExtractorHelper.hasExactCauseThrowable(throwable,
                InterruptedIOException.class,
                InterruptedException.class);
    }
}
