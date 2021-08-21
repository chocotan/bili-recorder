package moe.chikalar.recorder.interceptor;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import org.apache.commons.lang3.StringUtils;
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
        String title = context.getAttribute("title");
        recordHistory.setTitle(StringUtils.isBlank(title) ? context.getRecordRoom().getTitle() : title);
        recordHistoryRepository.save(recordHistory);
        context.addAttribute("history", recordHistory);
    }
}
