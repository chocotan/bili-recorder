package moe.chikalar.recorder.recorder;

import javaslang.Tuple;
import javaslang.Tuple2;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.entity.RecordRoom;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BiliRecorder implements Recorder {

    public BiliApi.BiliResponseDto<BiliApi.BiliLiveStatus> getLiveStatus(String roomId) throws IOException {
        return BiliApi.getLiveStatus(roomId);
    }

    @Override
    public void onAdd(RecordRoom room) {
        BiliApi.BiliResponseDto<BiliApi.BiliLiveStatus> liveStatus = null;
        try {
            liveStatus = getLiveStatus(room.getRoomId());
            if (liveStatus.getCode() == 0) {
                Long uid = liveStatus.getData().getUid();
                BiliApi.BiliResponseDto<BiliApi.BiliMasterDto> masterInfo = BiliApi.getMasterInfo(room.getRoomId(), uid);
                if (liveStatus.getCode() == 0) {
                    room.setUname(masterInfo.getData().getInfo().getUname());
                    room.setUid(Long.valueOf(masterInfo.getData().getInfo().getUid()));
                }
            }
        } catch (IOException e) {
            // ignored
        }
    }

    @Override
    public Tuple2<Boolean, String> check(RecordRoom recordRoom) throws IOException {
        // TODO 如果b站接口异常（被限制），这里会nullpointer
        BiliApi.BiliRoomInfo roomInfo = BiliApi.getRoomInfo(recordRoom.getRoomId(),
                recordRoom.getUid()).getData();

        boolean liveStatus = roomInfo.getLiveStatus() == 1;
        if (liveStatus) {
            return Tuple.of(liveStatus, roomInfo.getTitle());
        } else {
            return Tuple.of(liveStatus, "");
        }
    }

    @Override
    public String getType() {
        return "bili";
    }

    @Override
    public List<String> getPlayUrl(String roomId) throws IOException {
        return BiliApi.getPlayUrl(roomId);
    }

}
