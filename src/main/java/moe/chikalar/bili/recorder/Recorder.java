package moe.chikalar.bili.recorder;

import javaslang.Tuple2;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.repo.RecordRoomRepository;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public interface Recorder {
    public void onAdd(RecordRoom room);

    Tuple2<Boolean, String> check(RecordRoom room) throws IOException;

    public String getType();

    List<String> getPlayUrl(String roomId) throws IOException;

}
