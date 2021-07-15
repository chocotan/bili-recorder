package moe.chikalar.bili.recorder;

import javaslang.Tuple;
import javaslang.Tuple2;
import moe.chikalar.bili.api.KugouApi;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class KugouRecorder implements Recorder {
    @Override
    public void onAdd(RecordRoom room) {
        try {
            KugouApi.KugouResponseDto<KugouApi.KugouRoomInfo> roomInfo = KugouApi.getRoomInfo(room.getRoomId());
            if (roomInfo.getCode() == 0) {
                String nickName = roomInfo.getData().getNickName();
                room.setUname(nickName);
            }
        } catch (IOException e) {
            // ignored
        }

    }

    @Override
    public Tuple2<Boolean, String> check(RecordRoom room) throws IOException {
        KugouApi.KugouResponseDto<KugouApi.KugouLiveStatus> liveStatus = KugouApi.getLiveStatus(room.getRoomId());
        if (liveStatus.getCode() == 0) {
            return Tuple.of(liveStatus.getData().getLiveStatus() == 1, "");
        }
        return Tuple.of(false, liveStatus.getMsg());
    }

    @Override
    public String getType() {
        return "kugou";
    }

    @Override
    public List<String> getPlayUrl(String roomId) throws IOException {
        List<String> result = new ArrayList<>();
        result.addAll(KugouApi.getPlayUrl(roomId));
        if (result.size() == 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            result.addAll(KugouApi.getPlayUrl2(roomId));
        }
        return result;
    }
}
