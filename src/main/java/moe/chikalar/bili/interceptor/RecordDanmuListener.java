package moe.chikalar.bili.interceptor;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.recorder.DanmuRecorder;
import moe.chikalar.bili.recorder.RecorderFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RecordDanmuListener implements RecordListener {
    @Autowired
    private RecorderFactory factory;

    private ThreadLocal<DanmuRecorder> threadLocal = new ThreadLocal<>();

    public String beforeRecord(RecordRoom recordRoom, RecordConfig config, String path) {
        if (config.getDanmuRecord()) {
            String danmuFileName = path.replaceAll("(.*)\\.flv", "$1.txt");
            DanmuRecorder danmuRecorder = factory.getDanmuRecorder(recordRoom.getType());
            threadLocal.set(danmuRecorder);
            try {

                danmuRecorder.startRecord(recordRoom.getRoomId(), danmuFileName);
                log.info("[{}] 弹幕保存至 {}", recordRoom.getRoomId(), danmuFileName);
            } catch (Exception e) {
                log.error("[{}] 弹幕录制异常 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
            }
        }

        return path;
    }

    @Override
    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config) {
        DanmuRecorder danmuRecorder = threadLocal.get();
        if (danmuRecorder != null) {
            try {
                danmuRecorder.stop();
            } catch (Exception e) {
                // ignored
            }
        }
        return recordResult;
    }
}
