package moe.chikalar.bili.recorder;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.repo.RecordRoomRepository;
import moe.chikalar.bili.utils.FileUtil;
import moe.chikalar.bili.utils.FlvCheckerWithBuffer;
import moe.chikalar.bili.utils.FlvCheckerWithBufferEx;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
public class RecordHelper {

    private final Map<Long, ProgressDto> ctx = new HashMap<>();

    private static final ExecutorService recordPool = Executors.newFixedThreadPool(100);

    @Autowired
    private RecordRoomRepository recordRoomRepository;

    @Autowired
    private BiliRecorderProperties properties;

    @Autowired
    private RecorderFactory recorderFactory;

    public void recordAndErrorHandle(RecordRoom recordRoom) {
        String data = recordRoom.getData();
        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
        if (recorderOpt.isPresent()) {
            Recorder recorder = recorderOpt.get();
            Future<?> submit = recordPool.submit(() -> {
                RecordConfig config = JSON.parseObject(data, RecordConfig.class);
                try {
                    // doRecord
                    // 检查房间是否正在直播
                    checkStatusAndRecord(recordRoom, recorder);
                    remove(recordRoom.getId());
                } catch (Exception e) {
                    // 异常时将状态设置为1, 记录异常日志
                    recordRoom.setLastError(ExceptionUtils.getStackTrace(e));
                    log.info("[{}] 录制发生异常 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
                } finally {
                    recordRoom.setStatus("1");
                    recordRoom.setLastError("");
                    recordRoomRepository.save(recordRoom);
                    remove(recordRoom.getId());
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

    public void checkStatusAndRecord(RecordRoom recordRoom, Recorder recorder) throws IOException, InterruptedException {
        Tuple2<Boolean, String> check = recorder.check(recordRoom);
        if (!check._1) {
            log.info("[{}] 该房间未在直播 ", recordRoom.getRoomId());
            return;
        }
        String title = check._2;
        recordRoom.setTitle(title);
        recordRoomRepository.save(recordRoom);
        RecordConfig config = JSON.parseObject(recordRoom.getData(), RecordConfig.class);
        String defaultFolder = properties.getWorkPath();
        String playUrl1 = recorder.getPlayUrl(recordRoom.getRoomId()).get(0);
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
                File newFile = new FlvCheckerWithBufferEx().check(pathname, !config.isDebug());
                if (StringUtils.isNotBlank(config.getMoveFolder())) {
                    File moveParentFolder = new File(config.getMoveFolder());
                    if(!moveParentFolder.exists()){
                        moveParentFolder.mkdirs();
                    }
                    File moveFile = new File(moveParentFolder, newFile.getName());
                    Thread.sleep(1000);
                    Files.move(newFile, moveFile);
                }
            }
        }
    }

    private String generateFileName(RecordRoom recordRoom) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return recordRoom.getRoomId() + "-" + recordRoom.getUname() + "-" + recordRoom.getTitle() + "-" + time + ".flv";
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
