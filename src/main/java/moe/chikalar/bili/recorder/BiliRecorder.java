package moe.chikalar.bili.recorder;

import javaslang.Tuple;
import javaslang.Tuple2;
import moe.chikalar.bili.api.BiliApi;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class BiliRecorder implements Recorder {

    public BiliApi.BiliResponseDto<BiliApi.BiliLiveStatus> getLiveStatus(String roomId) throws IOException {
        return BiliApi.getLiveStatus(roomId);
    }

    public void getMasterInfo() {
        String url = "http://api.live.bilibili.com/live_user/v1/Master/info";

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
                }
            }
        } catch (IOException e) {
            // ignored
        }
    }

    @Override
    public Tuple2<Boolean, String> check(RecordRoom recordRoom) throws IOException {
        BiliApi.BiliLiveStatus data = BiliApi.getLiveStatus(recordRoom.getRoomId()).getData();
        boolean liveStatus = data.getLiveStatus() == 1;
        if (liveStatus) {
            BiliApi.BiliRoomInfo roomInfo = BiliApi.getRoomInfo(recordRoom.getRoomId(), data.getUid()).getData();
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
