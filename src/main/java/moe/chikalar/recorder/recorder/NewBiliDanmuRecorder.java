package moe.chikalar.recorder.recorder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.dmj.bili.cmd.BaseCommand;
import moe.chikalar.recorder.dmj.bili.data.BiliDataUtil;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.util.buf.HexUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class NewBiliDanmuRecorder extends AbstractDanmuRecorder {
    @Override
    public PublishSubject<byte[]> getSubject() {
        return PublishSubject.create();
    }

    @Override
    public WebSocket initWs(String url, String roomId, PublishSubject<byte[]> subject) {
        return BiliApi.initWebsocket(url, roomId, subject);
    }

    @Override
    public void sendHeartBeat(WebSocket ws, byte[] heartByte) {
        ws.send(ByteString.of(heartByte));
    }

    @Override
    public byte[] getHeartBeatBytes() {
        return HexUtils.fromHexString("0000001f0010000100000002000000015b6f626a656374204f626a6563745d");
    }

    @Override
    public Tuple2<String, byte[]> beforeInitWs(String roomId, String fileName) {
        return BiliApi.beforeInitWs(roomId);
    }

    @Override
    public List<String> decode(byte[] bytes) throws Exception {
        return BiliDataUtil.handle_Message(ByteBuffer.wrap(bytes));

    }

    @Override
    public void writeToFile(String data, String fileName) {
        BaseCommand cmd = JSON.parseObject(data, BaseCommand.class);
        if (cmd != null && StringUtils.isNotBlank(cmd.getCmd())) {
            if ("DANMU_MSG".equals(cmd.getCmd())) {
                JSONArray info = cmd.getInfo();
                if (info != null) {
                    String msg = (String) info.get(1);
                    JSONArray userInfo = (JSONArray) info.get(2);
                    Long uid = Long.valueOf("" + userInfo.get(0));
                    String uname = (String) userInfo.get(1);
                    String text = String.format(template, System.currentTimeMillis(),
                            (System.currentTimeMillis() - startTs), uid, uname, msg);
                    File file = new File(fileName);
                    try {
                        FileUtils.write(file, text, StandardCharsets.UTF_8, true);
                    } catch (IOException e) {
                        log.error(ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }
    }
}
