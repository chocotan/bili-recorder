package moe.chikalar.bili.recorder;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.repo.RecordRoomRepository;
import moe.chikalar.bili.utils.FileUtil;
import moe.chikalar.bili.utils.FlvCheckerWithBuffer;
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
        if (!recorder.check(recordRoom)._1) {
            log.info("[{}] 该房间未在直播 ", recordRoom.getRoomId());
            return;
        }
        RecordConfig config = JSON.parseObject(recordRoom.getData(), RecordConfig.class);
        String defaultFolder = System.getProperty("user.home");
        String folder = StringUtils.isBlank(config.getSaveFolder())? defaultFolder: config.getSaveFolder();
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String playUrl1 = recorder.getPlayUrl(recordRoom.getRoomId()).get(0);
        String pathname = folder + File.separator + ".bili" + File.separator + recordRoom.getRoomId() + "-" + time + ".flv";
        log.info("[{}] 开始录制，保存文件至 {}", recordRoom.getRoomId(), pathname);
        ProgressDto progressDto = ctx.get(recordRoom.getId());
        try {
            FileUtil.record(playUrl1, pathname, progressDto);
        } finally {
            new FlvCheckerWithBuffer().check(pathname, true);
        }
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
