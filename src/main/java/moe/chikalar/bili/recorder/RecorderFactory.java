package moe.chikalar.bili.recorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RecorderFactory {
    @Autowired
    private List<Recorder> recorderList;

    public Optional<Recorder> getRecorder(String type) {
        return recorderList.stream()
                .filter(d -> type.equals(d.getType())).findFirst();
    }

}
