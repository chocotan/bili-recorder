package moe.chikalar.bili.interceptor;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.exception.LiveRecordException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
@Slf4j
public class RetryListener implements RecordListener {
    @Autowired
    private LinkedList<Long> recordQueue;

    public RecordResult afterRecord(RecordResult recordResult, RecordConfig config) {
        RecordRoom recordRoom = recordResult.getContext().getRecordRoom();
        Exception exception = recordResult.getException();
        if (exception instanceof LiveRecordException) {
            log.info("[{}] 录制发生网络异常，即将重试 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(exception));
            if (!recordQueue.contains(recordRoom.getId())) {
                recordQueue.offer(recordRoom.getId());
            }
        }
        return recordResult;
    }
}
