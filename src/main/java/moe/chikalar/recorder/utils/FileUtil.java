package moe.chikalar.recorder.utils;

import moe.chikalar.recorder.dto.ProgressDto;
import moe.chikalar.recorder.dto.RecordContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class FileUtil {
    public static void record(String url, RecordContext ctx,
                              ProgressDto progressDto) throws IOException {
        String path = ctx.getPath();
        progressDto.setStartTime(System.currentTimeMillis());
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("referer", "https://live.bilibili.com/");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");

        File file = new File(path);
        file.getParentFile().mkdirs();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(path)) {
            // 10k
            byte[] buffer = new byte[10240];
            int read;
            progressDto.setBytes(0L);
            AtomicLong atomicLong = ctx.getAttribute("lastWriteTime");
            while ((read = in.read(buffer)) != -1 && !progressDto.getStopStatus().get()) {
                if (atomicLong != null) {
                    atomicLong.set(System.currentTimeMillis());
                }
                out.write(buffer, 0, read);
                progressDto.setBytes(progressDto.getBytes() + read);
            }
        }

    }

    public void recordStatusCheck() {

    }


}
