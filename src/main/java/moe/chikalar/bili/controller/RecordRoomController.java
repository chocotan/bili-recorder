package moe.chikalar.bili.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.recorder.Recorder;
import moe.chikalar.bili.recorder.RecorderFactory;
import moe.chikalar.bili.repo.RecordRoomRepository;
import moe.chikalar.bili.utils.FileUtil;
import moe.chikalar.bili.utils.FlvCheckerWithBuffer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
@RequestMapping("record")
@Slf4j
public class RecordRoomController {

    private final RecordRoomRepository recordRoomRepository;

    // TODO 需要替换
    private static ExecutorService recordPool = Executors.newFixedThreadPool(100);

    public RecordRoomController(RecordRoomRepository recordRoomRepository) {
        this.recordRoomRepository = recordRoomRepository;
    }

    @RequestMapping("")
    public String index(Model model) {
        List<RecordRoom> all = Lists.newArrayList(recordRoomRepository.findAll());
        all
                .forEach(r -> {
                    ProgressDto progressDto = ctx.get(r.getId());
                    if (progressDto != null) {
                        r.setDataSize(progressDto.getBytes() / 1024 / 1024);
                    }
                });
        model.addAttribute("list", all);
        return "index";
    }

    @GetMapping("add")
    public String addGet() {
        return "add";
    }


    @PostMapping("add")
    public String addPost(String roomId, String type) {
        RecordRoom recordRoom = new RecordRoom();
        recordRoom.setRoomId(roomId);
        recordRoom.setCreateTime(new Date());
        recordRoom.setUpdateTime(new Date());
        recordRoom.setType(type);
        recordRoom.setStatus("2");

        RecordConfig config = new RecordConfig();
        config.setRoomId(roomId);
        config.setRetryTimes(0);
        recordRoom.setData(JSON.toJSONString(config));

        recordRoomRepository.save(recordRoom);
        return "redirect:/record";
    }

    @Autowired
    private RecorderFactory recorderFactory;

    private Map<Long, ProgressDto> ctx = new HashMap<>();


    @GetMapping("stopRecord")
    public String stopRecord(Long id) {
        log.info("即将停止录制 {} ", id);
        ProgressDto progressDto = ctx.get(id);
        if (progressDto != null) {
            progressDto.getStopStatus().set(true);
        } else {
            log.info("该直播间尚未开始录制 {} ", id);
        }
        ctx.remove(id);
        return "redirect:/record";
    }

    @GetMapping("startRecord")
    public String startRecord(Long id) {
        Optional<RecordRoom> opt = recordRoomRepository.findById(id);
        if (opt.isPresent()) {
            RecordRoom recordRoom = opt.get();
            String data = recordRoom.getData();
            Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
            if (recorderOpt.isPresent()) {
                Recorder recorder = recorderOpt.get();
                Future<?> submit = recordPool.submit(() -> {
                    RecordConfig config = JSON.parseObject(data, RecordConfig.class);
                    try {
                        // doRecord
                        // 检查房间是否正在直播
                        for (int i = 0; i <= config.getRetryTimes(); i++) {
                            try {
                                doRecord(recordRoom, recorder);
                                ctx.remove(recordRoom.getId());
                                // 正常结束不重试
                                break;
                            } catch (IOException e) {
                                // 如果最后一次仍然异常，那么抛出异常
                                if (i == config.getRetryTimes()) {
                                    throw e;
                                }
                                log.info(recordRoom + ": " + ExceptionUtils.getStackTrace(e));
                            }
                        }
                    } catch (Exception e) {
                        // 异常时将状态设置为1, 记录异常日志
                        recordRoom.setLastError(ExceptionUtils.getStackTrace(e));
                        log.info(recordRoom.getId() + ": " + ExceptionUtils.getStackTrace(e));
                    } finally {
                        recordRoom.setStatus("1");
                        recordRoom.setLastError("");
                        recordRoomRepository.save(recordRoom);
                        ctx.remove(recordRoom.getId());
                    }
                });
                ctx.put(recordRoom.getId(), new ProgressDto(false));
                // 将状态设置为ing
                recordRoom.setStatus("3");
                recordRoomRepository.save(recordRoom);
            }
        }
        return "redirect:/record";
    }


    private void doRecord(RecordRoom recordRoom, Recorder recorder) throws IOException, InterruptedException {
        if (!recorder.check(recordRoom.getRoomId())) {
            throw new RuntimeException("该房间未在直播 " + recordRoom.getId());
        }
        Thread.sleep(1000);
        String path = System.getProperty("user.home");
        String time = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        String playUrl1 = recorder.getPlayUrl(recordRoom.getRoomId()).get(0);
        String pathname = path + File.separator + ".bili" + File.separator + recordRoom.getRoomId() + "-" + time + ".flv";
        log.info("start record, save to {}", pathname);
        ProgressDto progressDto = ctx.get(recordRoom.getId());
        try {
            FileUtil.record(playUrl1, pathname, progressDto);
        } finally {
            // 不管是成功还是失败，在文件生成之后，需要check视频长度
            new FlvCheckerWithBuffer().check(pathname, true);
        }
        if (progressDto.getStopStatus().get()) {
            log.info("成功停止 {}", recordRoom.getId());
            try {
                FileUtil.record(playUrl1, pathname, progressDto);
            } finally {
                // 不管是成功还是失败，在文件生成之后，需要check视频长度
                new FlvCheckerWithBuffer().check(pathname, true);
            }
            if (progressDto.getStopStatus().get()) {
                log.info("成功停止录制 {}", recordRoom.getId());
            }
        }
    }
}