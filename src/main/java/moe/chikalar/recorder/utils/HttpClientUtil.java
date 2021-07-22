package moe.chikalar.recorder.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientUtil {
    private static OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .writeTimeout(8, TimeUnit.SECONDS)
            .build();

    public static String post(String url, Map<String, String> headers, String json) throws IOException {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request build = new Request.Builder()
                .headers(Headers.of(headers))
                .url(url)
                .post(requestBody)
                .build();
        Response response = client.newCall(build).execute();
        String string = response.body().string();
        log.info("url={}, header={}, param={}, resp={}", url, JSON.toJSONString(headers), json, string);
        return string;
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .get()
                .build()
        ).execute();
        String string = response.body().string();
        log.info("url={}, header={}, resp={}", url, JSON.toJSONString(headers), string);
        return string;
    }


    public static String get(String url) throws IOException {
        Response response = client.newCall(new Request.Builder()
                .url(url)
                .get()
                .build()
        ).execute();
        return response.body().string();
    }

    public static OkHttpClient getClient(){
        return client;
    }


}
