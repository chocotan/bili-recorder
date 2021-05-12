package moe.chikalar.bili.recorder;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.flv.FlvTagFix;
import moe.chikalar.bili.repo.RecordRoomRepository;
import moe.chikalar.bili.utils.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
public class RecordHelper {

    private final Map<Long, ProgressDto> ctx = new HashMap<>();

    private static final ExecutorService tagPool = Executors.newFixedThreadPool(100);

    @Autowired
    private RecordRoomRepository recordRoomRepository;

    @Autowired
    private BiliRecorderProperties properties;

    @Autowired
    private RecorderFactory recorderFactory;
    @Autowired
    private LinkedList<Long> recordQueue;

    @Autowired
    private FlvTagFix fix;

    public void recordAndErrorHandle(RecordRoom recordRoom) {
        log.info("[{}] 接收到录制任务", recordRoom.getRoomId());
        String data = recordRoom.getData();
        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
        if (recorderOpt.isPresent()) {
            Recorder recorder = recorderOpt.get();
            Future<?> submit = tagPool.submit(() -> {
                RecordConfig config = JSON.parseObject(data, RecordConfig.class);
                try {
                    checkStatusAndRecord(recordRoom, recorder);
                } catch (Exception e) {
                    // 异常时将状态设置为1, 记录异常日志
                    recordRoom.setLastError(ExceptionUtils.getStackTrace(e));
                    log.info("[{}] 录制发生异常 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
                } finally {
                    log.info("[{}] 录制结束", recordRoom.getRoomId());
                    remove(recordRoom.getId());
                    recordRoom.setStatus("1");
                    recordRoom.setLastError("");
                    recordRoomRepository.save(recordRoom);
                    // 临时注释掉，结束后需要检查当前直播状态，如果仍然是正在直播，那么需要将其加入录制队列
                    // checkLiveStatusAfterRecord(recordRoom, recorder, config);
                }
            });
            put(recordRoom.getId(), new ProgressDto(false));
            // 将状态设置为ing
            recordRoom.setStatus("3");
            recordRoomRepository.save(recordRoom);
        } else {
            log.info("未知类型 {}", recordRoom.getType());
        }
    }

    private void checkLiveStatusAfterRecord(RecordRoom recordRoom, Recorder recorder, RecordConfig config) {
        Tuple2<Boolean, String> check = null;
        try {
            Thread.sleep(config.getRetryInterval() * 1000);
            if (!recordQueue.contains(recordRoom.getId())) {
                check = recorder.check(recordRoom);
                if (check._1) {
                    // 加入录制队列
                    log.info("[{}] 用户仍然在直播，疑似网络波动导致，将其加入录制队列",
                            recordRoom.getRoomId());
                    // 再判断录制状态，只有没有在录制的才加入录制队列
                    Optional<RecordRoom> roomOpt = recordRoomRepository.findById(recordRoom.getId());
                    roomOpt.ifPresent(o -> {
                        if ("1".equals(o.getStatus()))
                            recordQueue.offer(recordRoom.getId());
                    });

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkStatusAndRecord(RecordRoom recordRoom, Recorder recorder) throws IOException, InterruptedException {
        log.info("[{}] 准备检查房间是否在直播", recordRoom.getRoomId());
        Tuple2<Boolean, String> check = recorder.check(recordRoom);
        if (!check._1) {
            log.info("[{}] 该房间未在直播 ", recordRoom.getRoomId());
            return;
        }
        String title = check._2;
        if (StringUtils.isBlank(title)) {
            title = "直播";
        }
        recordRoom.setTitle(title);
        recordRoomRepository.save(recordRoom);
        RecordConfig config = JSON.parseObject(recordRoom.getData(), RecordConfig.class);
        String defaultFolder = properties.getWorkPath();
        List<String> playUrl = recorder.getPlayUrl(recordRoom.getRoomId());
        if (playUrl == null || playUrl.size() == 0) {
            log.info("[{}] 该房间未在直播，无法获取播放地址 ", recordRoom.getRoomId());
            return;
        }
        String playUrl1 = playUrl.get(0);
        String uname = recordRoom.getUname();
        String folder = defaultFolder + File.separator + uname;
        folder = StringUtils.isNotBlank(config.getSaveFolder()) ? config.getSaveFolder() : folder;
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        String fileName = generateFileName(recordRoom);
        String pathname = folder + File.separator + fileName;
        log.info("[{}] 开始录制，保存文件至 {}", recordRoom.getRoomId(), pathname);
        ProgressDto progressDto = ctx.get(recordRoom.getId());
        try {
            FileUtil.record(playUrl1, pathname, progressDto);
        } finally {
            if (new File(pathname).exists()) {
                tagPool.submit(() -> {
                    try {
                        File newFile = new File(pathname);
                        if (config.isFixTag()) {
                            try {
                                newFile = fix.fix(pathname, !config.isDebug());
                                log.info("[{}] 成功修复时间戳 {}", recordRoom.getRoomId(), pathname);
                            } catch (Exception e) {
                                log.info("[{}] 修复时间戳失败 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
                            }
                        }
                        if (StringUtils.isNotBlank(config.getMoveFolder())) {
                            File moveParentFolder = new File(config.getMoveFolder());
                            if (!moveParentFolder.exists()) {
                                moveParentFolder.mkdirs();
                            }
                            File moveFile = new File(moveParentFolder, newFile.getName());
                            Thread.sleep(1000);
                            Files.move(newFile, moveFile);
                        }
                    } catch (Exception e) {
                        log.info(ExceptionUtils.getStackTrace(e));
                    }
                });

            }
        }
    }

    private String generateFileName(RecordRoom recordRoom) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return recordRoom.getRoomId() + "-" + recordRoom.getUname() + "-" + time + "-" + recordRoom.getTitle() + ".flv";
    }

    public ProgressDto get(Long id) {
        return ctx.get(id);
    }


    public void remove(Long id) {
        ctx.remove(id);
    }

    public void put(Long id, ProgressDto progressDto) {
        ctx.put(id, progressDto);
    }
}
