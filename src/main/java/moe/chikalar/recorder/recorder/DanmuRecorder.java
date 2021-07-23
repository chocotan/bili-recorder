package moe.chikalar.recorder.recorder;

import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import okhttp3.WebSocket;

import java.util.List;

public interface DanmuRecorder {


    void startRecord(String roomId, String fileName);

    PublishSubject<byte[]> getSubject();

    public WebSocket initWs(String url, String roomId, PublishSubject<byte[]> subject);

    public void sendHeartBeat(WebSocket ws, byte[] heartByte);

    byte[] getHeartBeatBytes();

    Tuple2<String, byte[]> beforeInitWs(String roomId, String fileName);

    List<String> decode(byte[] b) throws Exception;

    void writeToFile(String data, String file);
    void stop();
}
