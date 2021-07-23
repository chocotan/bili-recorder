package moe.chikalar.recorder.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jayway.jsonpath.JsonPath;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple;
import javaslang.Tuple2;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.dmj.bili.cmd.BaseCommand;
import moe.chikalar.recorder.dmj.bili.data.BiliDataUtil;
import moe.chikalar.recorder.utils.HttpClientUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
                "&std_plat=7&std_imei=fbf48cc0-c7f5-49f8-92b8-10dbb1fb8022&std_kid=0&streamType=1-2-5&ua=fx-flash&targetLiveTypes=1-5-6&version=1.0&supportEncryptMode=1&_=" +
                ts;
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String s = HttpClientUtil.get(url, additionalHeaders);
        return JsonPath.read(s, "$.data.lines[*].streamProfiles[*].httpsFlv[*]");
    }

    public static List<String> getPlayUrl2(String roomId) throws IOException {
        long ts = System.currentTimeMillis();
        String url = "https://fx1.service.kugou.com/video/pc/live/pull/mutiline/streamaddr?std_rid=" +
                roomId +
                "&std_plat=7&streamType=1-5&ua=fx-flash&targetLiveTypes=4&version=1.0&_=" +
                ts;
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String s = HttpClientUtil.get(url, additionalHeaders);
        return JsonPath.read(s, "$.data.lines[*].streamProfiles[*].httpsFlv[*]");
    }

    public static Tuple2<String, String> beforeInitWs(String roomId) {
        try {
            String url = "https://fx2.service.kugou.com/socket_scheduler/pc/v2/address.jsonp?rid=" +
                    roomId
                    + "&_v=7.0.0&_p=0&pv=20191231&at=102&cid=105";
            String str = HttpClientUtil.get(url);
            List<String> wsUrls = JsonPath.read(str, "$.data.addrs[*].host");
            String token = JsonPath.read(str, "$.data.soctoken");
            return Tuple.of(wsUrls.get(0), token);
        } catch (Exception e) {
            return null;
        }
    }

    public static WebSocket initWebsocket(String url, String roomId,
                                          PublishSubject<BaseCommand> queue) {
        Request request = new Request.Builder()
                .url(url)
                .header("Origin", "https://fanxing.kugou.com/")
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
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                try {
                    byte[] message = bytes.toByteArray();
                    int t = message.length;
                    int n = 9;
                    if (t == 0) {
                        return;
                    }

                } catch (Exception e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            }


        });
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
