package moe.chikalar.recorder.recorder;

import com.alibaba.fastjson.JSON;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.dto.ProgressDto;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
import moe.chikalar.recorder.dto.RecordResult;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.exception.LiveRecordException;
import moe.chikalar.recorder.exception.LiveStatusException;
import moe.chikalar.recorder.interceptor.RecordListener;
import moe.chikalar.recorder.repo.RecordRoomRepository;
import moe.chikalar.recorder.utils.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    @Autowired
    private List<RecordListener> interceptors;

    public void recordAndErrorHandle(RecordRoom recordRoom) {
        log.info("[{}] 接收到录制任务", recordRoom.getRoomId());
        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
        if (!recorderOpt.isPresent())
            return;
        // 将状态设置为ing
        recordRoom.setStatus("3");
        recordRoomRepository.save(recordRoom);
        Recorder recorder = recorderOpt.get();
        AtomicLong lastWriteTime = new AtomicLong(System.currentTimeMillis());
        Future<?> future = recordPool.submit(() -> {
            String data = recordRoom.getData();
            RecordConfig config = JSON.parseObject(data, RecordConfig.class);
            RecordResult recordResult = null;
            RecordContext context = new RecordContext();
            context.setRecordRoom(recordRoom);
            context.addAttribute("lastWriteTime", lastWriteTime);
            try {
                Tuple2<Boolean, String> check = checkStatus(recordRoom, recorder);
                doRecord(recordRoom, recorder, check, context);
                recordResult = RecordResult.success(context);
                lastWriteTime.set(0L);
            } catch (Throwable e) {
                recordResult = RecordResult.error(e, context);
                // 标记为0表示已经结束
                lastWriteTime.set(0L);
            } finally {
                List<RecordListener> list = interceptors.stream().sorted(Comparator.comparingInt(RecordListener::getOrder)).collect(Collectors.toList());
                for (RecordListener listener : list) {
                    try {
                        recordResult = listener.afterRecord(recordResult, config);
                    } catch (Exception e) {
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        });

        recordPool.submit(() -> {
            try {
                Thread.sleep(5000L);
                while (!future.isDone() && lastWriteTime.get() != 0) {
                    Thread.sleep(60000);
                    if (lastWriteTime.get() != 0 && System.currentTimeMillis() - lastWriteTime.get() > 60000) {
                        future.cancel(true);
                        log.info("[{}] 状态检查出现异常，即将停止录制", recordRoom.getRoomId());
                        break;
                    }
                }
            } catch (Exception e) {
                // ignored
            }
        });


    }

    public Tuple2<Boolean, String> checkStatus(RecordRoom recordRoom, Recorder recorder) throws IOException {
        log.info("[{}] 准备检查房间是否在直播", recordRoom.getRoomId());
        Tuple2<Boolean, String> check = recorder.check(recordRoom);
        if (!check._1) {
            throw new LiveStatusException("[{}] 当前房间未在直播" + recordRoom.getRoomId());
        }
        return check;
    }

    public RecordContext doRecord(RecordRoom recordRoom, Recorder recorder,
                                  Tuple2<Boolean, String> check,
                                  RecordContext context) throws IOException, InterruptedException {
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
        String fileName = generateFileName(recordRoom);
        if (fileName.contains("/")) {
            fileName = fileName.replace("/", "_");
        }
        String path = folder + File.separator + fileName;

        // before record
        List<RecordListener> list = interceptors.stream().sorted(Comparator.comparingInt(RecordListener::getOrder)).collect(Collectors.toList());
        context.setRecordRoom(recordRoom);
        context.setPath(path);
        context.addAttribute("title", title);

        for (RecordListener listener : list) {
            try {
                listener.beforeRecord(context, config);
            } catch (Exception e) {
                log.error("[{}] listener异常 {}", recordRoom.getRoomId(), ExceptionUtils.getStackTrace(e));
            }
        }

        log.info("[{}] 开始录制，保存文件至 {}", recordRoom.getRoomId(), path);
        put(recordRoom.getId(), new ProgressDto(false));
        ProgressDto progressDto = ctx.get(recordRoom.getId());
        try {
            FileUtil.record(playUrl1, context, progressDto);
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                throw new LiveStatusException("该房间未在直播 " + recordRoom.getId(), e);
            }
            throw new LiveRecordException(e);
        } finally {
            remove(recordRoom.getId());
        }
        return context;
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
