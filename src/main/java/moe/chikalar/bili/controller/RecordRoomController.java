package moe.chikalar.bili.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dto.ProgressDto;
import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.recorder.BiliRecorder;
import moe.chikalar.bili.recorder.RecordHelper;
import moe.chikalar.bili.recorder.Recorder;
import moe.chikalar.bili.recorder.RecorderFactory;
import moe.chikalar.bili.repo.RecordRoomRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Controller
@RequestMapping("record")
@Slf4j
public class RecordRoomController {

    private final RecordRoomRepository recordRoomRepository;

    @Autowired
    private RecorderFactory recorderFactory;


    public RecordRoomController(RecordRoomRepository recordRoomRepository) {
        this.recordRoomRepository = recordRoomRepository;
    }

    @RequestMapping("")
    public String index(Model model) {
        List<RecordRoom> all = Lists.newArrayList(recordRoomRepository.findAll());
        all
                .forEach(r -> {
                    ProgressDto progressDto = recordHelper.get(r.getId());
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
        recordRoom.setStatus("1");
        Optional<Recorder> recorderOpt = recorderFactory.getRecorder(recordRoom.getType());
        if (recorderOpt.isPresent()) {
            Recorder recorder = recorderOpt.get();
            recorder.onAdd(recordRoom);
        }
        RecordConfig config = new RecordConfig();
        config.setRoomId(roomId);
        config.setUname(recordRoom.getUname());
        recordRoom.setData(JSON.toJSONString(config, SerializerFeature.WriteMapNullValue));
        recordRoomRepository.save(recordRoom);
        return "redirect:/record";
    }

    @Autowired
    private RecordHelper recordHelper;


    @GetMapping("stopRecord")
    public String stopRecord(Long id) {
        log.info("即将停止录制 {} ", id);
        ProgressDto progressDto = recordHelper.get(id);
        if (progressDto != null) {
            progressDto.getStopStatus().set(true);
        } else {
            log.info("[{}] 该直播间尚未开始录制 {} ", id);
        }
        recordHelper.remove(id);
        return "redirect:/record";
    }

    @GetMapping("startRecord")
    public String startRecord(Long id) {
        Optional<RecordRoom> opt = recordRoomRepository.findById(id);
        if (opt.isPresent()) {
            RecordRoom recordRoom = opt.get();
            recordHelper.recordAndErrorHandle(recordRoom);
        }
        return "redirect:/record";
    }

    @GetMapping("delete")
    public String delete(Long id) throws InterruptedException {
        this.stopRecord(id);
        // stop 是异步的，所以这里sleep一下
        Thread.sleep(1000L);
        recordRoomRepository.deleteById(id);
        return "redirect:/record";
    }

    @GetMapping("edit")
    public String editGet(Long id, Model model){
        Optional<RecordRoom> recordRoom = recordRoomRepository.findById(id);
        if(recordRoom.isPresent()){
            model.addAttribute("obj", recordRoom.get());
            return "edit";
        }
        return "redirect:/record";

    }

    @PostMapping("edit")
    public String editPost(String status, String data, Long id){
        Optional<RecordRoom> recordRoom = recordRoomRepository.findById(id);
        if(recordRoom.isPresent()){
            RecordRoom s = recordRoom.get();
            s.setStatus(status);
            s.setData(data);
            recordRoomRepository.save(s);
        }
        return "redirect:/record";
    }

}