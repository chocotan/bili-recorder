package moe.chikalar.bili.flv;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class FlvTagFix {
    public File fix(String flvPath, Boolean delete) throws IOException {
        String newMp4Path = flvPath.replaceAll("(.*)\\.flv", "$1-fixed.mp4");
        FFmpeg.atPath()
                .addInput(UrlInput.fromUrl(flvPath))
                .setLogLevel(LogLevel.INFO)
                .setOverwriteOutput(true)
                .addArguments("-c:a", "copy")
                .addArguments("-c:v", "copy")
                .addOutput(UrlOutput.toUrl(newMp4Path))
                .execute();
        if (delete) {
            new File(flvPath).delete();
        }
        return new File(newMp4Path);
    }
}
