package video.player.qrplayer;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import org.schabi.newpipe.extractor.DownloadRequest;
import org.schabi.newpipe.extractor.DownloadResponse;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Downloader implements org.schabi.newpipe.extractor.Downloader {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

    private static Downloader instance;
    private String mCookies;
    private final OkHttpClient client;

    private Downloader(OkHttpClient.Builder builder) {
        this.client = builder
                .readTimeout(30, TimeUnit.SECONDS)
                //.cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"), 16 * 1024 * 1024))
                .build();
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     */
    public static Downloader init(@Nullable OkHttpClient.Builder builder) {
        return instance = new Downloader(builder != null ? builder : new OkHttpClient.Builder());
    }


    @Override
    public String download(String siteUrl, Localization localization) throws IOException, ReCaptchaException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept-Language", localization.getLanguage());
        return download(siteUrl, requestProperties);
    }

    @Override
    public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        return getBody(siteUrl, customProperties).string();
    }

    public InputStream stream(String siteUrl) throws IOException {
        try {
            return getBody(siteUrl, Collections.emptyMap()).byteStream();
        } catch (ReCaptchaException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    private ResponseBody getBody(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        final Request.Builder requestBuilder = new Request.Builder()
                .method("GET", null).url(siteUrl);

        for (Map.Entry<String, String> header : customProperties.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (!customProperties.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final Request request = requestBuilder.build();
        final Response response = client.newCall(request).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested");
        }

        if (body == null) {
            response.close();
            return null;
        }

        return body;
    }

    @Override
    public String download(String siteUrl) throws IOException, ReCaptchaException {
        return download(siteUrl, Collections.emptyMap());
    }


    @Override
    public DownloadResponse get(String siteUrl, DownloadRequest request) throws IOException, ReCaptchaException {
        final Request.Builder requestBuilder = new Request.Builder()
                .method("GET", null).url(siteUrl);

        Map<String, List<String>> requestHeaders = request.getRequestHeaders();
        // set custom headers in request
        for (Map.Entry<String, List<String>> pair : requestHeaders.entrySet()) {
            for(String value : pair.getValue()){
                requestBuilder.addHeader(pair.getKey(), value);
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final Request okRequest = requestBuilder.build();
        final Response response = client.newCall(okRequest).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested");
        }

        if (body == null) {
            response.close();
            return null;
        }

        return new DownloadResponse(body.string(), response.headers().toMultimap());
    }

    @Override
    public DownloadResponse get(String siteUrl) throws IOException, ReCaptchaException {
        return get(siteUrl, DownloadRequest.emptyRequest);
    }

    @Override
    public DownloadResponse post(String siteUrl, DownloadRequest request) throws IOException, ReCaptchaException {

        Map<String, List<String>> requestHeaders = request.getRequestHeaders();
        if(null == requestHeaders.get("Content-Type") || requestHeaders.get("Content-Type").isEmpty()){
            // content type header is required. maybe throw an exception here
            return null;
        }

        String contentType = requestHeaders.get("Content-Type").get(0);

        RequestBody okRequestBody = null;
        if(null != request.getRequestBody()){
            okRequestBody = RequestBody.create(MediaType.parse(contentType), request.getRequestBody());
        }
        final Request.Builder requestBuilder = new Request.Builder()
                .method("POST",  okRequestBody).url(siteUrl);

        // set custom headers in request
        for (Map.Entry<String, List<String>> pair : requestHeaders.entrySet()) {
            for(String value : pair.getValue()){
                requestBuilder.addHeader(pair.getKey(), value);
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final Request okRequest = requestBuilder.build();
        final Response response = client.newCall(okRequest).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested");
        }

        if (body == null) {
            response.close();
            return null;
        }

        return new DownloadResponse(body.string(), response.headers().toMultimap());
    }
}