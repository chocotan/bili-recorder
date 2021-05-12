package moe.chikalar.bili.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jayway.jsonpath.JsonPath;
import lombok.Data;
import moe.chikalar.bili.utils.HttpClientUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KugouApi {

    public static KugouResponseDto<KugouLiveStatus> getLiveStatus(String roomId) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://fanxing.kugou.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        long ts = System.currentTimeMillis();
        String res = HttpClientUtil.get(
                String.format("https://service1.fanxing.kugou.com/roomcen/room/web/roomStatus?roomId=%s&version=1000&std_plat=7&std_dev=%%22%%22&std_imei=%%22%%22&times=%d&channel=0&sign\\=%%22%%22\\&_\\=%d", roomId, ts, ts), additionalHeaders);
        return JSON.parseObject(res, new TypeReference<KugouResponseDto<KugouLiveStatus>>() {
        });
    }

    public static KugouResponseDto<KugouRoomInfo> getRoomInfo(String roomId) throws IOException {

        long ts = System.currentTimeMillis();
        String url = String.format("https://fx.service.kugou.com/vodcenter/songorder/getInServiceOrder?roomId=%s&_=%d", roomId, ts);
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://fanxing.kugou.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url, additionalHeaders);
        return JSON.parseObject(res, new TypeReference<KugouResponseDto<KugouRoomInfo>>() {
        });
    }


    public static List<String> getPlayUrl(String roomId) throws IOException {
        long ts = System.currentTimeMillis();
        String url = "https://fx1.service.kugou.com/video/pc/live/pull/mutiline/streamaddr?std_rid=" +
                roomId +
                "&std_plat=7&streamType=1-5&ua=fx-flash&targetLiveTypes=4&version=1.0&_=" +
                ts;
        String formatUrl = String.format(url, roomId);
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String s = HttpClientUtil.get(formatUrl, additionalHeaders);
        return JsonPath.read(s, "$.data.lines[*].streamProfiles[*].httpsFlv[*]");
    }

    @Data
    public static class KugouLiveStatus {
        private String liveSessionId;
        private Integer clientType;
        private Integer liveInRoomId;
        private Integer liveStarRoomId;
        private Integer liveStatus;
        private Integer liveType;
        private Integer roomId;
        private Integer roomType;
        private Integer timeOut;
        private Integer transientStatus;
    }

    @Data
    public static class KugouRoomInfo {
        private Long autoStopTimeLeft;
        private String nickName;
        private String userLogo;
        private Integer chiefUserKugouId;
        private Long chiefUserId;
        private String chiefUserName;
        private String chiefUserLogo;
        private String orderId;
        private String songName;
        private String singerName;
        private Long roomId;
        private Long starKugouId;
        private Integer orderState;
        private Integer wanted;
    }

    @Data
    public static class KugouResponseDto<T> {
        private Integer code;
        private T data;
        private String msg;
        private Long times;
    }


}
