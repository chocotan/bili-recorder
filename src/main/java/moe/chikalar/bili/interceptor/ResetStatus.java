package moe.chikalar.bili.interceptor;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.exception.LiveStatusException;
import moe.chikalar.bili.repo.RecordRoomRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResetStatus implements RecordListener {
    private final RecordRoomRepository recordRoomRepository;

    public ResetStatus(RecordRoomRepository recordRoomRepository) {
        this.recordRoomRepository = recordRoomRepository;
    }

    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config) {
        Exception exception = recordResult.getException();
        if (exception instanceof LiveStatusException) {
            log.info("[{}] 当前房间未在直播", recordRoom.getRoomId());
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        } else if (exception == null) {
            log.info("[{}] 录制结束，无异常", recordRoom.getRoomId());
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        } else{
            log.info("[{}] 录制异常 {} ", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(exception));
            recordRoom.setStatus("1");
            recordRoomRepository.save(recordRoom);
        }
        return recordResult;
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
