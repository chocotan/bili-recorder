package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.exception.LiveRecordException;
import moe.chikalar.bili.repo.RecordRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Optional;

@Component
public class RetryInterceptor implements RecordInterceptor {
    @Autowired
    private LinkedList<Long> recordQueue;
    @Autowired
    private RecordRoomRepository recordRoomRepository;

    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult) {
        Exception exception = recordResult.getException();
        if (exception instanceof LiveRecordException) {
            if (!recordQueue.contains(recordRoom.getId())) {
                recordQueue.offer(recordRoom.getId());
            }
        }
        return recordResult;
    }
}
