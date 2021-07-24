package moe.chikalar.recorder.recorder;

import com.alibaba.fastjson.JSONArray;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dmj.bili.cmd.BaseCommand;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractDanmuRecorder implements DanmuRecorder {
    protected AtomicBoolean stop = new AtomicBoolean(false);
    protected Thread heartBeatThread = null;
    protected WebSocket ws;
    protected final String template = "%d^^%d^^%d^^%s^^%s\r\n";
    protected Disposable subscribe;
    protected Long startTs = System.currentTimeMillis();

    public void startRecord(String roomId, String fileName) {
        Tuple2<String, byte[]> init = this.beforeInitWs(roomId, fileName);
        String wsUrl = init._1;
        byte[] verifyBytes = init._2;
        PublishSubject<byte[]> ps = getSubject();
        this.subscribe = ps.observeOn(Schedulers.io()).doOnNext(bytes -> {
            try {
                List<String> decodes = decode(bytes);
                decodes.forEach(decode -> {
                    writeToFile(decode, fileName);
                });
            } catch (Exception e) {
                log.info(ExceptionUtils.getStackTrace(e));
            }
        }).subscribe();
        this.ws = initWs(wsUrl, roomId, ps);
        ws.send(ByteString.of(verifyBytes));
        this.heartBeatThread = new Thread(() -> {
            for (int i = 0; i < 10000000 && !stop.get(); i++) {
                sendHeartBeat(ws, getHeartBeatBytes());
                try {
                    Thread.sleep(getHeartBeatInterval());
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        this.heartBeatThread.start();
    }

    public int getHeartBeatInterval() {
        return 30000;
    }

    public void stop() {
        stop.set(true);
        heartBeatThread.interrupt();
        if (this.ws != null) {
            this.ws.close(1000, "");
        }
    }

    @Override
    public void sendHeartBeat(WebSocket ws, byte[] heartByte) {
        ws.send(ByteString.of(heartByte));
    }

}
