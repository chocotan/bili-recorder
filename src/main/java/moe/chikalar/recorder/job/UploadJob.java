package moe.chikalar.recorder.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.dto.ProgressDto;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.RecordHelper;
import moe.chikalar.recorder.recorder.RecorderFactory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import moe.chikalar.recorder.uploader.BiliVideoUploader;
import moe.chikalar.recorder.uploader.VideoUploader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
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
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void uploadCheck() {
        // 刚启动完成的时候，要处理上次上传的异常情况
        // 将所有正在上传的修改为当前的状态
        List<RecordHistory> error = historyRepository.findByUploadStatus("2");
        log.info("查询出{}个异常状态的录制记录，即将更新", error.size());
        error.forEach(h -> {
            RecordRoom recordRoom = h.getRecordRoom();
            RecordConfig config = JSON.parseObject(recordRoom.getData(), RecordConfig.class);
            Boolean uploadToBili = config.getUploadToBili();
            if (uploadToBili != null && uploadToBili) {
                h.setUploadStatus("1");
            } else {
                h.setUploadStatus("0");
            }
            historyRepository.save(h);
        });


        // 查询12小时内的，最旧的一条记录，根据这条记录的realStartTime查出来相同的记录
        // 只查询12小时以内的，状态为done和error的，且uploadStatus为待上传，且重试次数没超标
        // 检查是否正在直播，如果正在直播，那么不处理
        Date to = new Date();
        Date from = new Date(to.getTime() - 24 * 3600 * 1000);
        // order by startTime，取第1条
        List<RecordHistory> histories = historyRepository
                .findByStatusInAndUploadStatusAndUploadRetryCountLessThanAndUpdateTimeBetweenOrderByStartTimeAsc(
                        Arrays.asList("done", "error"),
                        "1", Math.toIntExact(properties.getUploadReties()), from, to);

        // 检查是否在录制，把未在录制的上传了
        histories = histories.stream()
                .filter(d -> {
                    try {
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

        if (histories.isEmpty()) {
            return;
        }
        RecordHistory toUpload = histories.get(0);
        log.info("共有{}条直播记录待上传，取最早的一条id={}，title={}", histories.size(), toUpload.getId(), toUpload.getTitle());

        if (toUpload.getRealStartTime() != null) {
            // 查询相同直播间，且realStartTime相等的直播记录，排序
            histories = historyRepository.findByStatusInAndUploadStatusAndUploadRetryCountLessThanAndRealStartTimeOrderByStartTimeAsc(Arrays.asList("done", "error"),
                    "1", Math.toIntExact(properties.getUploadReties()), toUpload.getRealStartTime());
            log.info("找到{}条相同时间的记录", histories.size());
        }

        histories.forEach(h -> {
            log.info("待上传记录id={},title={},startTime={}", h.getId(), h.getTitle(), h.getStartTime());
        });


        // 按照日期时间排序分成不同的投稿
        histories.sort((a, b) -> (int) (a.getStartTime().getTime()
                - b.getStartTime().getTime()));

        // 分隔文件
        List<String> totalFiles = histories.stream().flatMap(h -> {
            List<String> files = new ArrayList<>();

            if (StringUtils.isNotBlank(h.getExtraFiles())) {
                files.addAll(Arrays.asList(h.getExtraFiles().split(",")));
            } else {
                files.add(h.getFilePath());
            }
            return files.stream();
        }).filter(f -> {
            File file = new File(f);
            return file.exists() && file.length() > properties.getUploadFileSizeMin();
        }).collect(Collectors.toList());

        RecordHistory recordHistory = histories.get(0);
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
            histories.forEach(h -> {
                h.setUploadStatus("2");
                historyRepository.save(h);
            });
            if (!totalFiles.isEmpty()) {
                String uploadRes = uploader.upload2(recordConfig, recordHistory, totalFiles);
                String bv = JSON.parseObject(uploadRes).getJSONObject("data").getString("bvid");
                log.info("[{}] 上传成功，file={}，bv={}", recordRoom.getId(), totalFiles, bv);
            } else {
                log.info("[{}] 无文件，file={}", recordRoom.getId(), totalFiles);
            }
            // 成功了之后，将状态设为3
            histories.forEach(h -> {
                h.setUploadStatus("3");
            });
        } catch (Exception e) {
            final Integer[] retryCount = {recordHistory.getUploadRetryCount()};
            if (retryCount[0] == null) {
                retryCount[0] = 0;
            }
            histories.forEach(h -> {
                recordHistory.setUploadRetryCount(++retryCount[0]);
            });

            log.info("[{}] 上传录播异常，等待下次重试，error={}", recordRoom.getId()
                    , ExceptionUtils.getStackTrace(e));
        } finally {
            historyRepository.save(recordHistory);
            histories.forEach(h -> {
                historyRepository.save(h);
            });
        }


    }
}
