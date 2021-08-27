package moe.chikalar.recorder.interceptor;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordResult;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.exception.LiveStatusException;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import moe.chikalar.recorder.repo.RecordRoomRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class ResetStatus implements RecordListener {
    private final RecordRoomRepository recordRoomRepository;

    private final RecordHistoryRepository recordHistoryRepository;

    public ResetStatus(RecordRoomRepository recordRoomRepository, RecordHistoryRepository recordHistoryRepository) {
        this.recordRoomRepository = recordRoomRepository;
        this.recordHistoryRepository = recordHistoryRepository;
    }

    public RecordResult afterRecord(RecordResult recordResult, RecordConfig config) {
        RecordRoom recordRoom = recordResult.getContext().getRecordRoom();
        Throwable exception = recordResult.getException();
        if (exception instanceof LiveStatusException) {
            log.info("[{}] 当前房间未在直播", recordRoom.getRoomId());
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        } else if (exception == null) {
            log.info("[{}] 录制结束，无异常", recordRoom.getRoomId());
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        } else {
            log.info("[{}] 录制异常 {} ", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(exception));
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        }
        RecordHistory history = recordResult.getContext().getAttribute("history");

        if (history != null && recordResult.getException() == null) {
            Date endTime = new Date();
            history.setUpdateTime(new Date());
            history.setEndTime(endTime);
            recordHistoryRepository.save(history);
        }
        return recordResult;
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
