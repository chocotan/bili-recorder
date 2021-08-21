package moe.chikalar.recorder.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hiczp.bilibili.api.BilibiliClient;
import com.hiczp.bilibili.api.BilibiliClientProperties;
import com.hiczp.bilibili.api.Continuation;
import com.hiczp.bilibili.api.passport.model.LoginResponse;
import com.jayway.jsonpath.JsonPath;
import io.reactivex.subjects.Subject;
import javaslang.Tuple;
import javaslang.Tuple2;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import moe.chikalar.recorder.dmj.bili.data.BiliDataUtil;
import moe.chikalar.recorder.dmj.bili.data.BiliMsg;
import moe.chikalar.recorder.dmj.bili.data.ByteUtils;
import moe.chikalar.recorder.dmj.bili.data.InitRequestDto;
import moe.chikalar.recorder.utils.HttpClientUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.ByteString;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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



    public static BilibiliClient login(String username, String password) throws IOException, ExecutionException, InterruptedException {
        BilibiliClientProperties bilibiliClientProperties = new BilibiliClientProperties();
        bilibiliClientProperties.setAppKey("bca7e84c2d947ac6");
        bilibiliClientProperties.setAppSecret("60698ba2f68e01ce44738920a0ffe768");
        BilibiliClient client = new BilibiliClient(bilibiliClientProperties, HttpLoggingInterceptor.Level.NONE);
        CompletableFuture<BilibiliClient> clientFuture = new CompletableFuture<BilibiliClient>();
        client.login(username, password, null, null,null,new Continuation<LoginResponse>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }
            @Override
            public void resumeWithException(@NotNull Throwable throwable) {

            }
            @Override
            public void resume(LoginResponse loginResponse) {
                clientFuture.complete(client);
            }
        });
        return clientFuture.get();
    }


//    public static void main(String[] args) throws Exception {
//        String loginKey = getLoginKey();
//        System.out.println(loginKey);
//        System.out.println("" + JsonPath.read(loginKey, "data.key"));
//        System.out.println("" + JsonPath.read(loginKey, "data.hash"));
////        System.out.println(rsa("hello", "-----BEGIN PUBLIC KEY-----\nMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDjb4V7EidX/ym28t2ybo0U6t0n\n6p4ej8VjqKHg100va6jkNbNTrLQqMCQCAYtXMXXp2Fwkk6WR+12N9zknLjf+C9sx\n/+l48mjUU8RqahiFD1XT/u2e0m2EN029OhCgkHx3Fc/KlFSIbak93EH/XlYis0w+\nXl69GV6klzgxW6d2xQIDAQAB\n-----END PUBLIC KEY-----\n"));
//    }

    public static String sign(Map<String, String> params, String appSecret) {
        // 签名规则： md5(url编码后的请求参数（body）)
        String body = params.entrySet().stream().map(e -> {
            try {
                return e.getKey() + "=" + URLEncoder.encode(e.getValue(), String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                return "";
            }
        })
                .collect(Collectors.joining("&"));
        return DigestUtils.md5Hex(body + appSecret);
    }

    public static String rsa(String str, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        key = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8)));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] secretMessageBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
        String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);
        return encodedMessage;

    }


    /**
     * 初始化ws连接，在连接之前要调用三个接口组装成一个byte数组作为第一个请求数据发送
     * 给B站ws服务端
     *
     * @param url    这个ulr是调用这个接口之前的接口获取到的弹幕ws地址
     * @param roomId 房间号?
     * @return 连接的ws对象
     */
    public static WebSocket initWebsocket(String url, String roomId, Subject<byte[]> queue) {
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
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                System.out.println();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
                queue.onNext(bytes.toByteArray());
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
