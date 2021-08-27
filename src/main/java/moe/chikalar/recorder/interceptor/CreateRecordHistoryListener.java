package moe.chikalar.recorder.interceptor;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class CreateRecordHistoryListener implements RecordListener {
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    @Override
    public void beforeRecord(RecordContext context, RecordConfig config) {
        // create record history
        RecordHistory recordHistory = new RecordHistory();
        recordHistory.setRecordRoom(context.getRecordRoom());
        Date startTime = new Date();
        recordHistory.setStartTime(startTime);
        recordHistory.setRealStartTime(startTime.getTime());
        // 检查上一次直播的结束时间，如果相差不到五分钟，则将realStartTime设置为上一次直播的开始时间
        Optional<RecordHistory> lastHistoryOpt = recordHistoryRepository.findTop1ByRecordRoomIdOrderByStartTimeDesc(context.getRecordRoom().getId());

        lastHistoryOpt.ifPresent(l -> {
            if (l.getEndTime() != null) {
                long endTimeInMillis = l.getEndTime().getTime();
                long currentTimeInMillis = startTime.getTime();
                if (currentTimeInMillis - endTimeInMillis < 10 * 60 * 1000) {
                    if (l.getRealStartTime() != null)
                        recordHistory.setRealStartTime(l.getRealStartTime());
                    else {
                        recordHistory.setRealStartTime(l.getStartTime().getTime());
                    }
                }
            }
        });


        recordHistory.setFileLength(0L);
        recordHistory.setFilePath(context.getPath());
        recordHistory.setFileSize(0L);
        recordHistory.setStatus("ing");
        recordHistory.setUpdateTime(startTime);
        String title = context.getAttribute("title");
        recordHistory.setTitle(StringUtils.isBlank(title) ? context.getRecordRoom().getTitle() : title);
        recordHistoryRepository.save(recordHistory);
        context.addAttribute("history", recordHistory);
    }
}
