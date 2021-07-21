package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordContext;
import moe.chikalar.bili.entity.RecordHistory;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.repo.RecordHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CreateRecordHistoryListener implements RecordListener {
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    @Override
    public void beforeRecord(RecordContext context, RecordConfig config) {
        // create record history
        RecordHistory recordHistory = new RecordHistory();
        recordHistory.setRecordRoom(context.getRecordRoom());
        recordHistory.setStartTime(new Date());
        recordHistory.setFileLength(0L);
        recordHistory.setFilePath(context.getPath());
        recordHistory.setFileSize(0L);
        recordHistory.setStatus("ing");
        recordHistoryRepository.save(recordHistory);
        context.addAttribute("history", recordHistory);
    }
}
