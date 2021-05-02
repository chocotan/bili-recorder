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

public class BiliApi {


    public static BiliResponseDto<BiliLiveStatus> getLiveStatus(String roomId) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId, additionalHeaders);
        return JSON.parseObject(res, new TypeReference<>() {
        });
    }

    public static BiliResponseDto<BiliMasterDto> getMasterInfo(String roomId, Long uid) throws IOException {
        String url = "http://api.live.bilibili.com/live_user/v1/Master/info";
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url + "?uid=" + uid, additionalHeaders);
        BiliResponseDto<BiliMasterDto> resp = JSON.parseObject(res, new TypeReference<>() {});
        return resp;
    }


    public static List<String> getPlayUrl(String roomId) throws IOException {
        String url = "https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&platform=web&qn=10000";
        String formatUrl = String.format(url, roomId);
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String s = HttpClientUtil.get(formatUrl, additionalHeaders);
        return JsonPath.read(s, "$.data.durl[*].url");
    }


    @Data
    public static class BiliResponseDto<T> {
        // 0：成功 1：参数错误
        private Integer code;
        private String msg;
        private String message;
        private T data;
    }

    @Data
    public static class BiliLiveStatus {
        // 房间号
        private Long roomId;
        private Long shortId;
        // uid
        private Long uid;
        private Long needP2p;
        private Boolean isHidden;
        private Boolean isLocked;
        private Boolean isPortrait;
        // 是否在直播 1-正在直播，0-未直播？
        private Integer liveStatus;
        private Integer hiddenTill;
        private Integer lockTill;
        private Boolean encrypted;
        private Boolean pwdVerified;
        // 时间戳（秒）
        private Long liveTime;
        private Integer roomShield;
        private Integer isSp;
        private Integer specialType;
    }

    @Data
    public static class BiliMasterDto {
        private BiliMasterInfoDto info;
        private String exp;
        private Integer followerNum;
        private String roomId;
        private String medalName;
        private Long gloryCount;
        private String pendant;
        private Integer link_group_num;
        private String roomNews;
    }

    @Data
    public static class BiliMasterInfoDto {
        // num	主播UID
        private String uid;
        //str	主播用户名

        private String uname;
        //	str	主播头像url
        private String face;
        //	obj	认证信息
        private String officialVerify;
        // num	主播性别
        private Integer gender;
    }

}
