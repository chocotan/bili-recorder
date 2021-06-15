package moe.chikalar.bili.utils;

import moe.chikalar.bili.dto.ProgressDto;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class FileUtil {
    public static void record(String url, String filePath,
                              ProgressDto progressDto) throws IOException {
        progressDto.setStartTime(System.currentTimeMillis());
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("referer", "https://live.bilibili.com/");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");


        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(filePath);) {
            // 10k
            byte[] buffer = new byte[10240];
            int read;
            progressDto.setBytes(0L);
            while ((read = in.read(buffer)) != -1 && !progressDto.getStopStatus().get()) {
                out.write(buffer, 0, read);
                progressDto.setBytes(progressDto.getBytes() + read);
            }
        }
    }


}
