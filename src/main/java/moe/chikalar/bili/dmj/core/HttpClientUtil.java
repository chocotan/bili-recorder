package moe.chikalar.bili.dmj.core;

import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by chocotan on 2017/8/20.
 */
public class HttpClientUtil {
    private final OkHttpClient client;

    public HttpClientUtil(Boolean useProxy, String proxyHost, Integer proxyPort) {
        if (useProxy) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort));
            client = new OkHttpClient().newBuilder()
                    .proxy(proxy)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .writeTimeout(8, TimeUnit.SECONDS)
                    .build();

        } else {
            client = new OkHttpClient().newBuilder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .writeTimeout(8, TimeUnit.SECONDS)
                    .build();
        }
    }

    public String post(String url, Map<String, String> headers, String json) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request build = new Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(requestBody)
                .build();
        Response response = client.newCall(build).execute();
        return response.body().string();
    }

    public String get(String url, Map<String, String> headers) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .get()
                .build()
        ).execute();
        return response.body().string();
    }


    public String get(String url) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .get()
                .build()
        ).execute();
        return response.body().string();
    }


    public OkHttpClient getClient() {
        return client;
    }

}
