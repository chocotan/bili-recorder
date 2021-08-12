package moe.chikalar.recorder.interceptor;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
import moe.chikalar.recorder.dto.RecordResult;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.DanmuRecorder;
import moe.chikalar.recorder.recorder.RecorderFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RecordDanmuListener implements RecordListener {
    @Autowired
    private RecorderFactory factory;

    private ThreadLocal<DanmuRecorder> threadLocal = new ThreadLocal<>();

    public void beforeRecord(RecordContext context, RecordConfig config) {
        RecordRoom recordRoom = context.getRecordRoom();
        String path = context.getPath();
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

    }

    @Override
    public RecordResult afterRecord(RecordResult recordResult, RecordConfig config) {
        DanmuRecorder danmuRecorder = threadLocal.get();
        if (danmuRecorder != null) {
            try {
                danmuRecorder.stop();
            } catch (Exception e) {
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
        return recordResult;
    }
}
