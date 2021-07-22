package moe.chikalar.recorder.interceptor;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class Convert2Mp4 implements RecordListener {

    public RecordResult afterRecord(RecordResult recordResult, RecordConfig config) {
        if (!config.getConvertToMp4()) {
            return recordResult;
        }

        String filePath = recordResult.getContext().getPath();
        if(StringUtils.isBlank(filePath)){
            return recordResult;
        }
        File file = new File(filePath);
        if (file.exists()) {
            try {
                String newMp4Path = filePath.replaceAll("(.*)\\.flv", "$1-fixed.mp4");
                FFmpeg.atPath()
                        .addInput(UrlInput.fromUrl(filePath))
                        .setLogLevel(LogLevel.INFO)
                        .setOverwriteOutput(true)
                        .addArguments("-c:a", "copy")
                        .addArguments("-c:v", "copy")
                        .addOutput(UrlOutput.toUrl(newMp4Path))
                        .execute();
                File newFile = new File(newMp4Path);
                // 1m=1024*1024
                if (newFile.exists() && newFile.length() > 10 * 1024 * 1024) {
                    recordResult.getContext().setPath(newMp4Path);
                    if(config.getConvertToMp4Delete()){
                        file.delete();
                    }
                }
            } catch (Exception e) {
                log.info("[{}] ffmpeg执行报错 {}",
                        recordResult.getContext().getRecordRoom().getRoomId(),
                        ExceptionUtils.getStackTrace(e));
            }
        }
        return recordResult;
    }
}
