package moe.chikalar.bili.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jayway.jsonpath.JsonPath;
import io.reactivex.subjects.Subject;
import javaslang.Tuple;
import javaslang.Tuple2;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dmj.cmd.BaseCommand;
import moe.chikalar.bili.dmj.data.BiliDataUtil;
import moe.chikalar.bili.dmj.data.BiliMsg;
import moe.chikalar.bili.dmj.data.ByteUtils;
import moe.chikalar.bili.dmj.data.InitRequestDto;
import moe.chikalar.bili.utils.HttpClientUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BiliApi {

    public static BiliResponseDto<BiliLiveStatus> getLiveStatus(String roomId) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId, additionalHeaders);
        return JSON.parseObject(res, new TypeReference<BiliResponseDto<BiliLiveStatus>>() {
        });
    }

    public static BiliResponseDto<BiliMasterDto> getMasterInfo(String roomId, Long uid) throws IOException {
        String url = "http://api.live.bilibili.com/live_user/v1/Master/info";
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url + "?uid=" + uid, additionalHeaders);
        BiliResponseDto<BiliMasterDto> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<BiliMasterDto>>() {
        });
        return resp;
    }

    public static BiliResponseDto<BiliRoomInfo> getRoomInfo(String roomId, Long uid) throws IOException {
        String url = "https://api.live.bilibili.com/room/v1/Room/getRoomInfoOld";
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get(url + "?mid=" + uid, additionalHeaders);
        BiliResponseDto<BiliRoomInfo> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<BiliRoomInfo>>() {
        });
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






    public static Tuple2<String, byte[]> beforeInitWs(String roomId) {
        try {
            // 额外的header: Referer
            Map<String, String> additionalHeaders = new HashMap<>();
            additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
            String danmuInfo = HttpClientUtil.get("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?" +
                    "id=" + roomId + "&type=0", additionalHeaders);

            JSONObject jsonObject = JSON.parseObject(danmuInfo);
            JSONObject data = jsonObject.getJSONObject("data");
            String token = data.getString("token");
            JSONArray hostList = data.getJSONArray("host_list");
            String hostUrl = "wss://" + ((JSONObject) hostList.get(0)).getString("host") + "/sub";

            InitRequestDto requestDto = new InitRequestDto();
            requestDto.setKey(token);
            requestDto.setRoomid(Long.valueOf(roomId));
            String s = JSON.toJSONString(requestDto);
            byte[] secondPart = s.getBytes(StandardCharsets.UTF_8);
            BiliMsg barrageHeadHandle = BiliMsg.getBarrageHeadHandle(
                    secondPart.length + 16,
                    (char) 16, (char) 1, 7,
                    1);
            byte[] firstPart = BiliDataUtil.encode(barrageHeadHandle);
            // 这是连接成功后要发送的ws数据
            byte[] req = ByteUtils.mergeByteArrays(firstPart, secondPart);
            return Tuple.of(hostUrl, req);
        } catch (Exception e) {
            // ignored
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }


    public String roomInit(String roomId) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return HttpClientUtil.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId, additionalHeaders);
    }

    public String playUrl(String roomId) throws IOException {
        String url = "https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&platform=web&qn=10000";
        String formatUrl = String.format(url, roomId);
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return HttpClientUtil.get(formatUrl, additionalHeaders);
    }

    /**
     * 初始化ws连接，在连接之前要调用三个接口组装成一个byte数组作为第一个请求数据发送
     * 给B站ws服务端
     *
     * @param url    这个ulr是调用这个接口之前的接口获取到的弹幕ws地址
     * @param roomId 房间号?
     * @return 连接的ws对象
     */
    public static WebSocket initWebsocket(String url, String roomId, Subject<BaseCommand> queue) {
        Request request = new Request.Builder()
                .url(url)
                .header("Origin", "https://live.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36")
                .build();
        return HttpClientUtil.getClient().newWebSocket(request, new WebSocketListener() {
            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                log.warn("Connection closed, roomid={}, reason={}", roomId, reason);
            }


            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                try {
                    log.warn("Failed to connect, roomid={}, response={}, reason={} ",
                            roomId, ExceptionUtils.getStackTrace(t), response.body().string());
                } catch (Exception e) {
                    // ignored
                }
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                super.onOpen(webSocket, response);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                System.out.println();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                try {
                    String msg = BiliDataUtil.handle_Message(bytes.asByteBuffer());
                    BaseCommand cmd = JSON.parseObject(msg, BaseCommand.class);
                    if (cmd != null && cmd.getCmd() != null) {
                        cmd.setRoomId(roomId);
                        queue.onNext(cmd);
                    }
                } catch (Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }


        });
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


    @Data
    public static class BiliRoomInfo {
        private Integer roomStatus;
        private Integer roundStatus;
        private Integer liveStatus;
        private String url;
        private String title;
        private String coverc;
        private Integer online;
        private Long roomid;
        private Long broadcastType;
        private Long onlineHidden;
    }
}
