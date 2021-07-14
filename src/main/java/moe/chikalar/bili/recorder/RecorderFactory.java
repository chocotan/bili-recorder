package moe.chikalar.bili.recorder;

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
                return new BiliDanmuRecorder();
            default:
                return new DanmuRecorder(){};
        }
    }

}
