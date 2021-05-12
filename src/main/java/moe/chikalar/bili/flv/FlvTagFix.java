package moe.chikalar.bili.flv;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class FlvTagFix {
    public File fix(String flvPath, Boolean delete) throws IOException {
        String newFlvPath = flvPath.replaceAll("(.*)\\.flv", "$1-fixed.flv");
        final AtomicLong duration = new AtomicLong();
        FFmpegResult execute = FFmpeg.atPath()
                .addInput(UrlInput.fromUrl(flvPath))
                .setLogLevel(LogLevel.INFO)
                .setOverwriteOutput(true)
                .addArguments("-c:a", "copy")
                .addArguments("-c:v", "copy")
                .addOutput(UrlOutput.toUrl(newFlvPath))
                .setProgressListener(new ProgressListener() {
                    @Override
                    public void onProgress(FFmpegProgress progress) {
                        double percents = 100. * progress.getTimeMillis() / duration.get();
                        log.info("正在处理文件 [{}] {}", percents, flvPath);
                    }
                })
                .execute();
        if (delete) {
            new File(flvPath).delete();
        }
//        new FlvCheckerWithBufferEx().check(flvPath, delete);
        return new File(newFlvPath);
    }
}
