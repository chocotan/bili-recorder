package moe.chikalar.recorder.interceptor;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordResult;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

@Component
public class UpdateRecordHistoryListener implements RecordListener {
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    @Override
    public RecordResult afterRecord(RecordResult recordResult, RecordConfig config) {
        // create record history
        RecordHistory history = recordResult.getContext().getAttribute("history");
        if (history != null && recordResult.getException() == null) {
            Date endTime = new Date();
            history.setEndTime(endTime);
            history.setFileLength(endTime.getTime() - history.getStartTime().getTime());
            history.setStatus("done");
            history.setFilePath(recordResult.getContext().getPath());
            try {
                history.setFileSize(new File(recordResult.getContext().getPath()).length());
            } catch (Exception e) {
                // ignored
            }
            recordHistoryRepository.save(history);
        } else {
            if (history != null) {
                history.setStatus("error");
                recordHistoryRepository.save(history);
            }
        }
        return recordResult;
    }

    public int getOrder() {
        return 10;
    }
}
