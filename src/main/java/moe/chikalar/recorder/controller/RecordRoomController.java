package moe.chikalar.recorder.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.dto.ProgressDto;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.RecordHelper;
import moe.chikalar.recorder.recorder.Recorder;
import moe.chikalar.recorder.recorder.RecorderFactory;
import moe.chikalar.recorder.repo.RecordHistoryRepository;
import moe.chikalar.recorder.repo.RecordRoomRepository;
import moe.chikalar.recorder.uploader.BiliSessionDto;
import moe.chikalar.recorder.uploader.BiliVideoUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("record")
@Slf4j
public class RecordRoomController {

    private final RecordRoomRepository recordRoomRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecorderFactory recorderFactory;

    @Autowired
    private BiliRecorderProperties properties;

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
            log.info("[{}] 该直播间尚未开始录制 ", id);
            Optional<RecordRoom> roomOpt = recordRoomRepository.findById(id);
            if (roomOpt.isPresent()) {
                RecordRoom recordRoom = roomOpt.get();
                recordRoom.setStatus("1");
                recordRoomRepository.save(recordRoom);
            }
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
        historyRepository.deleteByRecordRoomId(id);
        recordRoomRepository.deleteById(id);
        return "redirect:/record";
    }

    @GetMapping("edit")
    public String editGet(Long id, Model model) {
        Optional<RecordRoom> recordRoom = recordRoomRepository.findById(id);
        if (recordRoom.isPresent()) {
            model.addAttribute("obj", recordRoom.get());
            return "edit";
        }
        return "redirect:/record";

    }

    @PostMapping("edit")
    public String editPost(String status, String data, Long id) {
        Optional<RecordRoom> recordRoom = recordRoomRepository.findById(id);
        if (recordRoom.isPresent()) {
            RecordRoom s = recordRoom.get();
            s.setStatus(status);
            // check json 合法
            JSON.parseObject(data, RecordConfig.class);
            s.setData(data);
            recordRoomRepository.save(s);
        }
        return "redirect:/record";
    }


    @RequestMapping("generateQR")
    public void generateQRCode(Long id, HttpServletResponse response) throws IOException, WriterException {
        Optional<RecordRoom> opt = recordRoomRepository.findById(id);
        if (opt.isPresent()) {
            RecordRoom recordRoom = opt.get();
            String data = recordRoom.getData();
            RecordConfig recordConfig = JSON.parseObject(data, RecordConfig.class);
            BiliApi.BiliResponseDto<BiliApi.GenerateQRDto> s = BiliApi.generateQRUrlTV();
            if (s.getCode() != 0) {
                throw new RuntimeException("生成二维码异常，请检查日志");
            }
            BitMatrix bm = new QRCodeWriter().encode(s.getData().getUrl(),
                    BarcodeFormat.QR_CODE, 256, 256);
            BufferedImage bi = MatrixToImageWriter.toBufferedImage(bm);
            ImageIO.write(bi, "jpg", response.getOutputStream());
            // 偷懒直接new一个Thread
            // new thread to check login status
            new Thread(() -> {
                for (int i = 0; i < 6; i++) {
                    try {
                        String loginResp = BiliApi.loginOnTV(s.getData().getAuth_code());
                        Integer code = JsonPath.read(loginResp, "code");
                        if (code == 0) {
                            log.info("{} 登录成功", recordConfig.getUploadUsername());
                            BiliSessionDto dto = JSON.parseObject(loginResp).getObject("data", BiliSessionDto.class);
                            recordConfig.setUploadAccessToken(dto.getAccessToken());
                            recordConfig.setUploadRefreshToken(dto.getRefreshToken());
                            recordConfig.setUploadTokenCreateTime(System.currentTimeMillis());
                            recordConfig.setUploadMid(dto.getMid());
                            recordRoomRepository.findById(id).ifPresent(d -> {
                                d.setData(JSON.toJSONString(recordConfig, SerializerFeature.PrettyFormat,
                                        SerializerFeature.WriteMapNullValue));
                                recordRoomRepository.save(d);
                            });
                        }
                        Thread.sleep(10000);
                    } catch (InterruptedException | IOException e) {
                        break;
                    }
                }
            }).start();
        }

    }
}