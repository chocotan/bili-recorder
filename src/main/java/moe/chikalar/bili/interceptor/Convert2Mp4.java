package moe.chikalar.bili.interceptor;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class Convert2Mp4 implements RecordListener {

    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config) {
        if (!config.isConvertToMp4()) {
            return recordResult;
        }
        String filePath = recordResult.getFilePath();
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
                if (newFile.exists() && newFile.length() > 1024 * 1024) {
                    recordResult.setFilePath(newMp4Path);
                    if(config.isConvertToMp4Delete()){
                        file.delete();
                    }
                }
            } catch (Exception e) {
                log.info("[{}] ffmpeg执行报错 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
            }
        }
        return recordResult;
    }
}
