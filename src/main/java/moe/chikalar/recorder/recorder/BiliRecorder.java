package moe.chikalar.recorder.recorder;

import com.jayway.jsonpath.JsonPath;
import javaslang.Tuple;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.entity.RecordRoom;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
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
        BiliApi.BiliResponseDto<BiliApi.RoomInitDto> roomInfo = BiliApi.roomInit(recordRoom.getRoomId());
        boolean liveStatus = false;
        String title = "直播";
        if (roomInfo.getData() != null) {
            liveStatus = roomInfo.getData().getLive_status() == 1;
            if (liveStatus) {
                try {
                    String userInfo = BiliApi.getUserInfo(recordRoom.getRoomId(), recordRoom.getUid());
                    title = JsonPath.read(userInfo, "$.data.live_room.title");
                } catch (Exception e) {
                    log.error("bili接口异常，{}，{}", "x/space/acc/info", ExceptionUtils.getStackTrace(e));
                }
            }
        } else {
            String userInfo = BiliApi.getUserInfo(recordRoom.getRoomId(), recordRoom.getUid());
            title = JsonPath.read(userInfo, "$.data.live_room.title");
            Object liveStatusVal = JsonPath.read(userInfo, "$.data.live_room.liveStatus");
            liveStatus = liveStatusVal.equals(1) || liveStatusVal.equals("1");
        }
        return Tuple.of(liveStatus, title);
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
