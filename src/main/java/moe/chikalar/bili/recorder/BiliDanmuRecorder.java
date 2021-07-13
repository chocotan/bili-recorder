package moe.chikalar.bili.recorder;

import com.alibaba.fastjson.JSONArray;
import io.loli.dmj.cmd.BaseCommand;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import moe.chikalar.bili.api.BiliApi;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.buf.HexUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.loli.dmj.core.BiliConstants.heartByte;

// currentTs^^(currentTs-startTs)^^uid^^uname^^content
public class BiliDanmuRecorder implements DanmuRecorder {
    private String roomId;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private String fileName;
    private long startTs;
    private Thread heartBeatThread = null;
    private final String template = "%d^^%d^^%d^^%s^^%s\r\n";
    private WebSocket webSocket;
    private PublishSubject<BaseCommand> ps;


    @Override
    public void startRecord(String roomId, String fileName) {
        this.roomId = roomId;
        this.fileName = fileName;
        this.startTs = System.currentTimeMillis();
        this.ps = PublishSubject.create();
        this.ps.doOnNext(cmd -> {
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
                    file.createNewFile();
                    FileUtils.write(file, text, StandardCharsets.UTF_8, true);
                }
            }
        }).subscribe();
        // 获取弹幕websocket地址
        Tuple2<String, byte[]> res = BiliApi.beforeInitWs(roomId);
        // 初始化websocket连接
        this.webSocket = BiliApi.initWebsocket(res._1, roomId, ps);
        // 发送认证信息
        this.webSocket.send(ByteString.of(res._2));
        // 心跳
        heartBeatThread = new Thread(() -> {
            for (int i = 0; i < 10000000 && stop.get(); i++) {
                webSocket.send(ByteString.of(HexUtils.fromHexString(heartByte)));
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        heartBeatThread.start();
    }

    @Override
    public void stop() {
        stop.set(true);
        heartBeatThread.interrupt();
        if (this.webSocket != null) {
            this.webSocket.close(0, "");
        }
        if (this.ps != null) {
            this.ps.onComplete();
        }
    }
}
