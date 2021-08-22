package moe.chikalar.recorder.job;

import com.alibaba.fastjson.JSON;
import com.hiczp.bilibili.api.member.model.AddResponse;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.dto.ProgressDto;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.RecordHelper;
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

    @Autowired
    private BiliRecorderProperties properties;


    @Autowired
    private RecordHelper recordHelper;

    // 定时查询直播历史，如果下一次直播开始时间和上一次结束时间小于5min，视为同一次直播
    @Scheduled(fixedDelay = 600000, initialDelay = 6000)
    public void uploadCheck() {
        // 只查询12小时以内的，状态为done和error的，且uploadStatus为待上传
        // 检查是否正在直播，如果正在直播，那么不处理
        Date to = new Date();
        Date from = new Date(to.getTime() - 12 * 3600 * 1000);
        List<RecordHistory> histories = historyRepository
                .findByStatusInAndUploadStatusAndStartTimeBetween(Arrays.asList("done", "error"),
                        "1", from, to);
        // 根据用户分组，只上传第一组
        Map<Long, List<RecordHistory>> groupHistories = histories.stream().collect(Collectors.groupingBy(h -> h.getRecordRoom().getId()));
        if (groupHistories.size() <= 0) {
            return;
        }
        histories = groupHistories.entrySet().stream().findFirst().get().getValue();
        // 检查是否在录制，把未在录制的上传了
        histories = histories.stream()
                .filter(d -> d.getUploadRetryCount() == null || d.getUploadRetryCount() < properties.getUploadReties())
                .filter(d -> {
                    try {
                        Thread.sleep(6000);
                        // 判断是否正在录制
                        RecordRoom recordRoom = d.getRecordRoom();
                        ProgressDto progressDto = recordHelper.get(recordRoom.getId());
                        if (progressDto != null) {
                            return progressDto.getStopStatus().get();
                        }
                        return true;
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
            RecordHistory h = histories.get(i);
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
            List<String> totalFiles = list.stream().flatMap(h -> {
                List<String> files = new ArrayList<>();

                if (StringUtils.isNotBlank(h.getExtraFilePaths())) {
                    files.addAll(Arrays.asList(h.getExtraFilePaths().split(",")));
                } else {
                    files.add(h.getFilePath());
                }
                return files.stream();
            }).collect(Collectors.toList());

            RecordHistory recordHistory = list.get(0);
            RecordRoom recordRoom = recordHistory.getRecordRoom();
            String data = recordRoom.getData();
            RecordConfig recordConfig = JSON.parseObject(data, RecordConfig.class);
            if (!recordConfig.getUploadToBili()) {
                log.info("[{}] uploadToBili=false，不上传", recordRoom.getId());
                return;
            }
            VideoUploader uploader = new BiliVideoUploader();
            try {
                // 准备上传文件
                log.info("[{}] 准备上传录播，时间={}，文件列表={}",
                        recordRoom.getId(),
                        recordHistory.getStartTime(),
                        totalFiles);
                list.forEach(h -> {
                    h.setUploadStatus("2");
                    historyRepository.save(h);
                });
                String uploadRes = uploader.upload2(recordConfig, recordHistory, totalFiles);
                String bv = JSON.parseObject(uploadRes).getJSONObject("data").getString("bvid");
                log.info("[{}] 上传成功，file={}，bv={}", recordRoom.getId(), totalFiles, bv);
                // 成功了之后，将状态设为3
                list.forEach(h -> {
                    h.setUploadStatus("3");
                });
            } catch (Exception e) {
                final Integer[] retryCount = {recordHistory.getUploadRetryCount()};
                if (retryCount[0] == null) {
                    retryCount[0] = 0;
                }
                list.forEach(h -> {
                    recordHistory.setUploadRetryCount(++retryCount[0]);
                });

                log.info("[{}] 上传录播异常，等待下次重试，error={}", recordRoom.getId()
                        , ExceptionUtils.getStackTrace(e));
            } finally {
                historyRepository.save(recordHistory);
                list.forEach(h -> {
                    historyRepository.save(h);
                });
            }
        });


    }
}
