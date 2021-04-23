package moe.chikalar.bili.recorder;

import java.io.IOException;
import java.util.List;

public interface Recorder {
    boolean check(String roomId) throws IOException;

    public String getType();

    List<String> getPlayUrl(String roomId) throws IOException;
}
