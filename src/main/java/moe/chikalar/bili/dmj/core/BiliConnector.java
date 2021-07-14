package moe.chikalar.bili.dmj.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple;
import javaslang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.dmj.cmd.BaseCommand;
import moe.chikalar.bili.dmj.data.BiliDataUtil;
import moe.chikalar.bili.dmj.data.BiliMsg;
import moe.chikalar.bili.dmj.data.ByteUtils;
import moe.chikalar.bili.dmj.data.InitRequestDto;
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
import java.util.Map;

@Slf4j
public class BiliConnector {
    private final HttpClientUtil httpClientUtil;
    private final PublishSubject<BaseCommand> queue;

    public BiliConnector(HttpClientUtil httpClientUtil,
                         PublishSubject<BaseCommand> queue) {
        this.httpClientUtil = httpClientUtil;
        this.queue = queue;
    }


    public Tuple2<String, byte[]> beforeInitWs(String roomId) {
        try {
            // 额外的header: Referer
            Map<String, String> additionalHeaders = new HashMap<>();
            additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
//            String roomInitInfo = httpClientUtil.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId,
//                    additionalHeaders);
//            String roomInfo = httpClientUtil.get("https://api.live.bilibili.com/room_ex/v1/RoomNews/get?roomid=" + roomId,
//                    additionalHeaders);
            String danmuInfo = httpClientUtil.get("https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?" +
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
            byte[] firstPart = BiliDataUtil.encode(BiliMsg.getBarrageHeadHandle(
                    secondPart.length + 16,
                    (char) 16, (char) 1, 7,
                    1));
            // 这是连接成功后要发送的ws数据
            byte[] req = ByteUtils.mergeByteArrays(firstPart, secondPart);
            return Tuple.of(hostUrl, req);
        } catch (IOException e) {
            // ignored
            log.error(ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    /**
     * 初始化ws连接，在连接之前要调用三个接口组装成一个byte数组作为第一个请求数据发送
     * 给B站ws服务端
     *
     * @param url    这个ulr是调用这个接口之前的接口获取到的弹幕ws地址
     * @param roomId 房间号?
     * @return 连接的ws对象
     */
    public WebSocket initWebsocket(String url, String roomId) {
        Request request = new Request.Builder()
                .url(url)
                .header("Origin", "https://live.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36")
                .build();
        return httpClientUtil.getClient().newWebSocket(request, new WebSocketListener() {
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
}
