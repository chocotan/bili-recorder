package moe.chikalar.bili.recorder;

import com.alibaba.fastjson.JSON;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.exception.LiveRecordException;
import moe.chikalar.bili.exception.LiveStatusException;
import moe.chikalar.bili.interceptor.RecordInterceptor;
import moe.chikalar.bili.repo.RecordRoomRepository;
import moe.chikalar.bili.utils.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NewRecordHelper {

    private final Map<Long, ProgressDto> ctx = new HashMap<>();

    private static final ExecutorService recordPool = Executors.newFixedThreadPool(100);

    @Autowired
    private RecordRoomRepository recordRoomRepository;

    @Autowired
    private BiliRecorderProperties properties;

    @Autowired
    private RecorderFactory recorderFactory;
    @Autowired
    private LinkedList<Long> recordQueue;

    @Autowired
    private List<RecordInterceptor> interceptors;


    public void recordAndErrorHandle(RecordRoom recordRoom) {
        log.info("[{}] 接收到录制任务", recordRoom.getRoomId());
        String data = recordRoom.getData();
        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
        if (!recorderOpt.isPresent())
            return;
        Recorder recorder = recorderOpt.get();
        // 将状态设置为ing
        recordRoom.setStatus("3");
        recordRoomRepository.save(recordRoom);
        recordPool.submit(() -> {
            RecordConfig config = JSON.parseObject(data, RecordConfig.class);
            RecordResult recordResult = null;
            try {
                String path = checkStatusAndRecord(recordRoom, recorder);
                recordResult = RecordResult.success(path);
            } catch (Exception e) {
                recordResult = RecordResult.error(e);
            }
            List<RecordInterceptor> list = interceptors.stream().sorted(Comparator.comparingInt(RecordInterceptor::getOrder)).collect(Collectors.toList());
            for (RecordInterceptor interceptor : list) {
                recordResult = interceptor.afterRecord(recordRoom, recordResult, config);
            }
        });

    }


    public String checkStatusAndRecord(RecordRoom recordRoom, Recorder recorder) throws IOException, InterruptedException {
        log.info("[{}] 准备检查房间是否在直播", recordRoom.getRoomId());
        Tuple2<Boolean, String> check = recorder.check(recordRoom);
        if (!check._1) {
            throw new LiveStatusException("[{}] 当前房间未在直播" + recordRoom.getRoomId());
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
            throw new LiveStatusException("该房间未在直播，无法获取播放地址 " + recordRoom.getId());
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
        String path = folder + File.separator + fileName;
        log.info("[{}] 开始录制，保存文件至 {}", recordRoom.getRoomId(), path);
        put(recordRoom.getId(), new ProgressDto(false));
        ProgressDto progressDto = ctx.get(recordRoom.getId());
        try {
            FileUtil.record(playUrl1, path, progressDto);
        } catch (Exception e) {
            throw new LiveRecordException(e);
        }
        return path;
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
