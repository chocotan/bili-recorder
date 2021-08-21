package moe.chikalar.recorder.uploader;

import com.alibaba.fastjson.JSON;
import com.hiczp.bilibili.api.BilibiliClient;
import com.hiczp.bilibili.api.member.model.PreUpload2Response;
import com.hiczp.bilibili.api.member.model.PreUploadResponse;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;
import moe.chikalar.recorder.entity.RecordRoom;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

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
    public void upload(RecordConfig config, RecordHistory recordHistory, List<String> files) {
        try {
            // TODO 需要保存access_token，调用用户信息接口判断登录状态是否失效了
            // TODO 失效了才重新登录
            BilibiliClient client = BiliApi.login(config.getUploadUsername(), config.getUploadPassword());
            PreUploadResponse preUpload = BiliApi.preUpload(client);
            String upcdn = "bda2";
            String probeVersion = "20200810";
            // mid=uid
            List<String> names = new ArrayList<>();
            for (String file : files) {
                PreUpload2Response preUpload2Response = BiliApi.preUpload2(client,
                        "ugcfr/pc3",
                        config.getUploadUid());
                String url = preUpload2Response.getUrl();
                String complete = preUpload2Response.getComplete();
                String filename = preUpload2Response.getFilename();
                // 分段上传
                long fileSize = new File(file).length();
                long chunkSize = 1024 * 1024 * 5;
                long chunkNum = (long) Math.ceil(fileSize / chunkSize);

                RandomAccessFile r = new RandomAccessFile(file, "r");
                for (int i = 0; i < chunkNum; i++) {
                    r.seek(i * chunkSize);
                    byte[] bytes = new byte[(int) chunkSize];
                    int read = r.read(bytes);
                    if (read == -1) {
                        break;
                    }
                    String s = BiliApi.uploadChunk(client, url, filename, bytes, bytes.length,
                            i + 1, (int) chunkNum);
                    System.out.println(i + "/" + chunkNum + " :" + s);
                }
                names.add(filename);
            }

            List<SingleVideoDto> dtos = new ArrayList<>();
            for (int i = 0, namesSize = names.size(); i < namesSize; i++) {
                String n = names.get(i);
                SingleVideoDto dto = new SingleVideoDto();
                dto.setTitle("" + i);
                dto.setDesc("");
                dto.setFilename(n);
                dtos.add(dto);
            }
            VideoUploadDto videoUploadDto = new VideoUploadDto();
            Date date = recordHistory.getStartTime();
            String uname = recordHistory.getRecordRoom().getUname();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
            String title = config.getTitleTemplate().replace("${uname}", uname)
                    .replace("${date}", dateStr);
            String desc = config.getDescTemplate().replace("${uname}", uname)
                    .replace("${date}", dateStr);
            videoUploadDto.setTitle(title);
            videoUploadDto.setDesc(desc);
            videoUploadDto.setDynamic(desc);
            videoUploadDto.setVideos(dtos);
            videoUploadDto.setTag(config.getUname());
            Map<String, Object> publish = BiliApi.publish(client, client.getToken(),
                    JSON.parseObject(JSON.toJSONString(videoUploadDto)));
            // TODO 调用创建接口
            System.out.println(publish);
            Thread.sleep(1000000);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    public static List<File> splitFile(File file, int sizeOfFileInMB) throws IOException {
        int counter = 1;
        List<File> files = new ArrayList<File>();
        int sizeOfChunk = 1024 * 1024 * sizeOfFileInMB;
        String eof = System.lineSeparator();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String name = file.getName();
            String line = br.readLine();
            while (line != null) {
                File newFile = new File(file.getParent(), name + "."
                        + String.format("%03d", counter++));
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {
                    int fileSize = 0;
                    while (line != null) {
                        byte[] bytes = (line + eof).getBytes(Charset.defaultCharset());
                        if (fileSize + bytes.length > sizeOfChunk)
                            break;
                        out.write(bytes);
                        fileSize += bytes.length;
                        line = br.readLine();
                    }
                }
                files.add(newFile);
            }
        }
        return files;
    }

    public static void main(String[] args) {
        String file = "E:\\data\\ffmpeg\\bin\\output.mp4";
        RecordConfig config = new RecordConfig();


        config.setUploadUsername("");
        config.setUploadPassword("");
        config.setUploadUid("134580");
        config.setUname("扇宝");
        RecordHistory recordHistory = new RecordHistory();
        RecordRoom recordRoom = new RecordRoom();
        recordRoom.setUname("扇宝");
        recordHistory.setRecordRoom(recordRoom);
        recordHistory.setStartTime(new Date(System.currentTimeMillis() - 24 * 3600 * 1000 * 3));
        new BiliVideoUploader().upload(config, recordHistory, Arrays.asList(
                "E:\\data\\ffmpeg\\bin\\output.mp4"
        ));
    }
}
