package moe.chikalar.recorder.recorder;

import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import okhttp3.WebSocket;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RecorderFactory {
    private final List<Recorder> recorderList;

    public RecorderFactory(List<Recorder> recorderList) {
        this.recorderList = recorderList;
    }

    public Optional<Recorder> getRecorder(String type) {
        return recorderList.stream()
                .filter(d -> type.equals(d.getType())).findFirst();
    }

    public DanmuRecorder getDanmuRecorder(String type){
        switch (type){
            case "bili":
                return new NewBiliDanmuRecorder();
            case "kugou":
                return new KugouDanmuRecorder();
            default:
                return new DanmuRecorder(){
                    @Override
                    public void startRecord(String roomId, String fileName) {

                    }
                    @Override
                    public PublishSubject<byte[]> getSubject() {
                        return null;
                    }

                    @Override
                    public WebSocket initWs(String url, String roomId, PublishSubject<byte[]> subject) {
                        return null;
                    }

                    @Override
                    public void sendHeartBeat(WebSocket ws, byte[] heartByte) {

                    }

                    @Override
                    public byte[] getHeartBeatBytes() {
                        return new byte[0];
                    }

                    @Override
                    public Tuple2<String, byte[]> beforeInitWs(String roomId, String fileName) {
                        return null;
                    }

                    @Override
                    public List<String> decode(byte[] b) throws Exception {
                        return null;
                    }

                    @Override
                    public void writeToFile(String data, String file) {

                    }

                    @Override
                    public void stop() {

                    }
                };
        }
    }

}
