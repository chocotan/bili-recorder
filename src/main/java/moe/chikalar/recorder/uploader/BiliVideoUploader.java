package moe.chikalar.recorder.uploader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class BiliVideoUploader implements VideoUploader {


    // 1.登录
    // -- 对于每个视频
    // 2.preupload接口
    // 3.preupload接口的返回值，再调用preupload接口，返回uploadUrl
    // 4.调用url接口
    // --
    // 5.验证码接口 调用https://member.bilibili.com/x/geetest/pre/add
    // 6.调用http://member.bilibili.com/x/vu/client/add
    @Override
    public String upload2(RecordConfig config, RecordHistory recordHistory, List<String> files) throws Exception {
        // 登录开始
        String username = config.getUploadUsername();
        String password = config.getUploadPassword();
        String accessToken = config.getUploadAccessToken();
        String mid = config.getUploadMid();
        if (StringUtils.isBlank(accessToken)) {
            throw new RuntimeException("accessToken为空，不上传, " + username);
        }
        boolean expired = false;
        // 检查是否已经过期，调用用户信息接口
        try {
            String myInfo = BiliApi.appMyInfo(accessToken);
            String uname = JsonPath.read(myInfo, "data.name");
            if (StringUtils.isBlank(uname)) {
                expired = true;
            }
        } catch (Exception e) {
            expired = true;
        }
        if (expired) {
            throw new RuntimeException("登录已过期，请重新登录, " + username);
        }
        // 登录结束
        List<String> names = new ArrayList<>();
        int idx = 1;
        for (String file : files) {
            String preRes = BiliApi.preUpload(accessToken, mid, "ugcfr/pc3");
            JSONObject preResObj = JSON.parseObject(preRes);
            String url = preResObj.getString("url");
            String complete = preResObj.getString("complete");
            String filename = preResObj.getString("filename");
            // 分段上传
            long fileSize = new File(file).length();
            long chunkSize = 1024 * 1024 * 5;
            long chunkNum = (long) Math.ceil((double) fileSize / chunkSize);
            MessageDigest md5Digest = DigestUtils.getMd5Digest();
            try (RandomAccessFile r = new RandomAccessFile(file, "r")) {
                for (int i = 0; i < chunkNum; i++) {
                    int tryCount = 0;
                    Exception toThrow = null;
                    while (tryCount < 5) {
                        try {
                            r.seek(i * chunkSize);
                            byte[] bytes = new byte[(int) chunkSize];
                            int read = r.read(bytes);
                            if (read == -1) {
                                break;
                            }
                            if (read != bytes.length)
                                bytes = ArrayUtils.subarray(bytes, 0, read);
                            md5Digest.update(bytes);
                            String s = BiliApi.uploadChunk(url, filename, bytes, read,
                                    i + 1, (int) chunkNum);
                            if (s != null && !s.contains("OK")) {
                                throw new RuntimeException("上传返回异常");
                            }
                            log.info("[{}] 上传视频 {} 进度{}/{}, resp={}", recordHistory.getRecordRoom().getId(),
                                    file, i + 1, chunkNum, s);
                            tryCount = 5;
                            toThrow = null;
                        } catch (Exception e) {
                            log.info("[{}] 上传视频 {} 进度{}/{}, exception={}", recordHistory.getRecordRoom().getId(),
                                    file, i + 1, chunkNum, ExceptionUtils.getStackTrace(e));
                            toThrow = e;
                        }
                    }
                    if (toThrow != null) {
                        throw toThrow;
                    }

                }
            }
            String md5 = DatatypeConverter.printHexBinary(md5Digest.digest()).toLowerCase();
            BiliApi.completeUpload(complete, (int) chunkNum, fileSize, md5,
                    new File(file).getName(), "2.0.0.1054");
            names.add(filename);
            idx++;
        }

        List<SingleVideoDto> dtos = new ArrayList<>();
        for (int i = 0, namesSize = names.size(); i < namesSize; i++) {
            String n = names.get(i);
            SingleVideoDto dto = new SingleVideoDto();
            dto.setTitle("P" + (i + 1));
            dto.setDesc("");
            dto.setFilename(n);
            dtos.add(dto);
        }
        VideoUploadDto videoUploadDto = new VideoUploadDto();
        if (config.getUploadTid() != null) {
            videoUploadDto.setTid(config.getUploadTid());
        }
        Date date = recordHistory.getStartTime();
        String uname = recordHistory.getRecordRoom().getUname();
        String title = StringUtils.isNotBlank(recordHistory.getTitle()) ? recordHistory.getTitle()
                : "直播录像";
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
        String dateTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        title = config.getUploadTitleTemplate().replace("${uname}", uname)
                .replace("${date}", dateStr)
                .replace("${datetime}", dateTimeStr)
                .replace("${title}", title)
                .replace("${roomId}", recordHistory.getRecordRoom().getRoomId());
        // 只有B站类型的才有uid
        if ("bili".equals(recordHistory.getRecordRoom().getType())) {
            title = title.replace("${uid}",
                    "" + recordHistory.getRecordRoom().getUid());
        }
        String desc = config.getUploadDescTemplate().replace("${uname}", uname)
                .replace("${date}", dateStr)
                .replace("${datetime}", dateTimeStr)
                .replace("${title}", title)
                .replace("${uid}", "" + recordHistory.getRecordRoom().getUid())
                .replace("${roomId}", recordHistory.getRecordRoom().getRoomId());
        // 只有B站类型的才有uid
        if ("bili".equals(recordHistory.getRecordRoom().getType())) {
            desc = desc.replace("${uid}",
                    "" + recordHistory.getRecordRoom().getUid());
        }
        videoUploadDto.setTitle(title);
        videoUploadDto.setDesc(desc);
        videoUploadDto.setDynamic(desc);
        videoUploadDto.setVideos(dtos);
        videoUploadDto.setTag(config.getUname());
        return BiliApi.publish(accessToken, videoUploadDto);
    }


//    public static void main(String[] args) {
//        String file = "E:\\data\\ffmpeg\\bin\\output.mp4";
//        RecordConfig config = new RecordConfig();
//
//
//        config.setUploadUsername("");
//        config.setUploadPassword("");
//        config.setUploadUid("134580");
//        config.setUname("扇宝");
//        RecordHistory recordHistory = new RecordHistory();
//        RecordRoom recordRoom = new RecordRoom();
//        recordRoom.setUname("扇宝");
//        recordHistory.setRecordRoom(recordRoom);
//        recordHistory.setStartTime(new Date(System.currentTimeMillis() - 24 * 3600 * 1000 * 3));
//        new BiliVideoUploader().upload(config, recordHistory, Arrays.asList(
//                "E:\\data\\ffmpeg\\bin\\output.mp4"
//        ));
//    }

//    public static void main(String[] args) throws IOException {
//        String file = "/home/choco/.bili/扇宝/22673512-扇宝-20210821232804-【扇情电台】棍棒底下出扇子-fixed.mp4";
//        long fileSize = new File(file).length();
//        long chunkSize = 1024 * 1024 * 5;
//        long chunkNum = (long) Math.ceil((double) fileSize / chunkSize);
//
//        RandomAccessFile r = new RandomAccessFile(file, "r");
//        for (int i = 0; i < chunkNum; i++) {
//            r.seek(i * chunkSize);
//            byte[] bytes = new byte[(int) chunkSize];
//            int read = r.read(bytes);
//            if (read == -1) {
//                break;
//            }
//            bytes = ArrayUtils.subarray(bytes, 0, read);
//            FileUtils.writeByteArrayToFile(new File("/home/choco/Downloads/output.mp4"), bytes, true);
//        }
//    }
}
