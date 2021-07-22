package moe.chikalar.recorder.recorder;

import com.alibaba.fastjson.JSONArray;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.api.KugouApi;
import moe.chikalar.recorder.dmj.bili.cmd.BaseCommand;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.tomcat.util.buf.HexUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static moe.chikalar.recorder.dmj.bili.data.BiliConstants.heartByte;


// currentTs^^(currentTs-startTs)^^uid^^uname^^content
@Slf4j
public class KugouDanmuRecorder implements DanmuRecorder {
    private AtomicBoolean stop = new AtomicBoolean(false);
    private long startTs;
    private Thread heartBeatThread = null;
    private final String template = "%d^^%d^^%d^^%s^^%s\r\n";
    private WebSocket webSocket;
    private PublishSubject<BaseCommand> ps;
    private Disposable subscribe;


    @Override
    public void startRecord(String roomId, String fileName) {
        this.startTs = System.currentTimeMillis();
        this.ps = PublishSubject.create();
        this.subscribe = this.ps.doOnNext(cmd -> {
            try {
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
                        FileUtils.write(file, text, StandardCharsets.UTF_8, true);
                    }
                }
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }).subscribe();
        // 获取弹幕websocket地址
        Tuple2<String, byte[]> res = KugouApi.beforeInitWs(roomId);
        // 初始化websocket连接
        this.webSocket = KugouApi.initWebsocket(res._1, roomId, ps);
        // 发送认证信息
        this.webSocket.send(ByteString.of(res._2));
        // 心跳
        heartBeatThread = new Thread(() -> {
            for (int i = 0; i < 10000000 && !stop.get(); i++) {
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
            this.webSocket.close(1000, "");
        }
        if (this.ps != null) {
            this.ps.onComplete();
            this.subscribe.dispose();
        }
    }
}
