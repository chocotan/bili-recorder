package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordContext;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordHistory;
import moe.chikalar.bili.repo.RecordHistoryRepository;
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
        if (history != null) {
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
        }
        return recordResult;
    }

    public int getOrder() {
        return 10;
    }
}
