package moe.chikalar.recorder.job;

import com.alibaba.fastjson.JSON;
import com.hiczp.bilibili.api.member.model.AddResponse;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.Recorder;
import moe.chikalar.recorder.recorder.RecorderFactory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import moe.chikalar.recorder.uploader.BiliVideoUploader;
import moe.chikalar.recorder.uploader.VideoUploader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UploadJob {

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecorderFactory recorderFactory;

    // 定时查询直播历史，如果下一次直播开始时间和上一次结束时间小于5min，视为同一次直播
    @Scheduled(fixedDelay = 600000, initialDelay = 6000)
    public void uploadCheck() {
        // 只查询当天的，状态为done和error的，且uploadStatus为待上传
        Calendar cal = Calendar.getInstance();
        // 检查是否正在直播，如果正在直播，那么不处理
        List<RecordHistory> histories = historyRepository
                .findByStatusInAndUploadStatus(Arrays.asList("done", "error"), "1");
        // 检查是否在直播，把未在直播的上传了
        histories = histories.stream()
                .filter(d -> {
                    return d.getUploadRetryCount() == null || d.getUploadRetryCount() < 5;
                })
                .filter(d -> {
                    try {
                        Thread.sleep(6000);
                        RecordRoom recordRoom = d.getRecordRoom();
                        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
                        if (recorderOpt.isPresent()) {
                            Recorder recorder = recorderOpt.get();
                            return !recorder.check(recordRoom)._1;
                        }
                    } catch (Exception e) {
                        // ignored;
                    }
                    return true;
                }).collect(Collectors.toList());

        // 按照日期时间排序分成不同的投稿
        histories.sort((a, b) -> (int) (a.getStartTime().getTime()
                - b.getStartTime().getTime()));
        List<List<RecordHistory>> upload = new ArrayList<>();

        long lastEndTime = 0;
        List<RecordHistory> tmp = new ArrayList<>();
        for (int i = 0; i < histories.size(); i++) {
            RecordHistory h = histories.get(0);
            if (lastEndTime == 0) {
                tmp.add(h);
                upload.add(tmp);
                lastEndTime = h.getStartTime().getTime();
                continue;
            }
            if ((h.getStartTime().getTime() - lastEndTime) < 5 * 60 * 1000) {
                tmp.add(h);
            } else {
                tmp = new ArrayList<>();
                tmp.add(h);
                upload.add(tmp);
            }
            lastEndTime = h.getStartTime().getTime();
        }

        upload.forEach(list -> {
            VideoUploader uploader = new BiliVideoUploader();
            RecordHistory recordHistory = list.get(0);
            RecordRoom recordRoom = recordHistory.getRecordRoom();
            String data = recordRoom.getData();
            RecordConfig recordConfig = JSON.parseObject(data, RecordConfig.class);
            if(!recordConfig.getUploadToBili()){
                return;
            }
            // 上传只支持mp4
            String files = recordHistory.getExtraFilePaths();
            if (StringUtils.isBlank(files)) {
                files = recordHistory.getFilePath();
            }

            List<String> fileList = Arrays.asList(files.split(","));
            try {
                // 准备上传文件
                log.info("[{}] 准备上传录播，时间={}，文件列表={}",
                        recordRoom.getId(),
                        recordHistory.getStartTime(),
                        fileList);
                AddResponse upload1 = uploader.upload(recordConfig, recordHistory, fileList);
                if (upload1 != null) {
                    recordHistory.setUploadStatus("3");
                }

            } catch (Exception e) {
                Integer retryCount = recordHistory.getUploadRetryCount();
                if (retryCount == null) {
                    retryCount = 0;
                }
                recordHistory.setUploadRetryCount(++retryCount);
                log.info("[{}] 上传录播异常，等待下次重试，error={}", recordRoom.getId()
                        , ExceptionUtils.getStackTrace(e));
            } finally {
                historyRepository.save(recordHistory);
            }
        });


    }
}
