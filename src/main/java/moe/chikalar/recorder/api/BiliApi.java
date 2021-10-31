package moe.chikalar.recorder.api;

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
import moe.chikalar.recorder.dmj.bili.data.BiliDataUtil;
import moe.chikalar.recorder.dmj.bili.data.BiliMsg;
import moe.chikalar.recorder.dmj.bili.data.ByteUtils;
import moe.chikalar.recorder.dmj.bili.data.InitRequestDto;
import moe.chikalar.recorder.uploader.BiliSessionDto;
import moe.chikalar.recorder.uploader.VideoUploadDto;
import moe.chikalar.recorder.utils.HttpClientUtil;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class BiliApi {


    // TODO 修改为从properties中读取
    private static String appKey = "bca7e84c2d947ac6";
    private static String appSecret = "60698ba2f68e01ce44738920a0ffe768";

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

    // 该接口已过期
    @Deprecated
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

    public static String getUserInfo(String rid, Long uid) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + rid);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return HttpClientUtil.get("https://api.bilibili.com/x/space/acc/info?mid=" + uid, additionalHeaders);
    }

    public static BiliResponseDto<RoomInitDto> roomInit(String roomId) throws IOException {
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        String res = HttpClientUtil.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomId, additionalHeaders);
        BiliResponseDto<RoomInitDto> resp = JSON.parseObject(res, new TypeReference<BiliResponseDto<RoomInitDto>>() {
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


    public String playUrl(String roomId) throws IOException {
        String url = "https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&platform=web&qn=10000";
        String formatUrl = String.format(url, roomId);
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("referer", "https://live.bilibili.com/" + roomId);
        additionalHeaders.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return HttpClientUtil.get(formatUrl, additionalHeaders);
    }

    public static String getLoginKey() throws IOException {
        String url = "https://passport.bilibili.com/api/oauth2/getKey";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("build", "5370000");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.post(url, headers, params, true);
    }


    public static String getKeyAndLogin(String username, String password) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        String loginKeyResp = getLoginKey();
        String hash = JsonPath.read(loginKeyResp, "data.hash");
        String key = JsonPath.read(loginKeyResp, "data.key");
        String loginResp = login(hash, key, username, password,
                "", "", "");
        String codeUrl;
        try {
            codeUrl = JsonPath.read(loginResp, "data.url");
        } catch (Exception e) {
            // 正常
            return loginResp;
        }
        if (StringUtils.isNotBlank(codeUrl)) {
            // 解析url中的challenge
            UriComponents urlComponents = UriComponentsBuilder.fromHttpUrl(codeUrl)
                    .build();
            String challenge = urlComponents.getQueryParams().get("challenge").get(0);
            log.info("请在浏览器中打开 {}", codeUrl);
            log.info("请输入validate :");
            String validate = new Scanner(System.in).nextLine();
            log.info("请输入challenge :");
            challenge = new Scanner(System.in).nextLine();
            String seccode = validate + "|jordan";
            loginKeyResp = getLoginKey();
            hash = JsonPath.read(loginKeyResp, "data.hash");
            key = JsonPath.read(loginKeyResp, "data.key");
            loginResp = login(hash, key, username, password, challenge, seccode, validate);
            System.out.println(loginResp);
        }
        return loginResp;
    }


    public static String login(
            String hash,
            String key,
            String username,
            String password,
            String challenge,
            String seccode,
            String validate) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {

        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("build", "5370000");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);

        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");

        params.put("username", username);
        params.put("password", rsa(hash + password, key));
        if (StringUtils.isNotBlank(challenge)) {
            params.put("challenge", challenge);
            params.put("seccode", seccode);
            params.put("validate", validate);
        } else {
            params.put("challenge", "");
            params.put("seccode", "");
            params.put("validate", "");
        }

        params.put("sign", sign(params, appSecret));
        String url = "https://passport.bilibili.com/x/passport-login/oauth2/login";
        return HttpClientUtil.post(url, headers, params, true);
    }


    public static String sign(Map<String, String> params, String appSecret) {
        // 签名规则： md5(url编码后的请求参数（body）)
        String body = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return DigestUtils.md5Hex(body + appSecret);
    }

    public static String rsa(String str, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        key = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replace("-----END PUBLIC KEY-----", "");
        byte[] decode = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] secretMessageBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
        return Base64.getEncoder().encodeToString(encryptedMessageBytes);
    }

    public static String preUpload(BiliSessionDto dto, String profile) throws IOException {
        String url = "https://member.bilibili.com/preupload";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", dto.getAccessToken());
        params.put("build", "5370000");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));

        params.put("profile", profile);
        params.put("mid", dto.getMid());

        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.get(uriBuilder.toUriString(), headers);
    }


    public static String publish(BiliSessionDto dto, VideoUploadDto data) throws IOException {
        String url = "https://member.bilibili.com/x/vu/client/add?access_key=" + dto.getAccessToken();
        Map<String, String> query = new HashMap<>();
        query.put("access_key", dto.getAccessToken());
        String sign = sign(query, appSecret);
        url = url + "&sign=" + sign;
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");

        String body = JSON.toJSONString(data);
        return HttpClientUtil.post(url, headers, body);
    }

    public static String uploadChunk(
            String uploadUrl,
            String fileName,
            byte[] bytes, long size, int nowChunk,
            int chunkNum) throws ExecutionException, InterruptedException, IOException {
        String md5 = DigestUtils.md5Hex(bytes);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "2.0.0.1054");
        params.put("filesize", "" + size);
        params.put("chunk", "" + nowChunk);
        params.put("chunks", "" + chunkNum);
        params.put("md5", md5);
        params.put("file", bytes);
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", "PHPSESSID=" + fileName);
        return HttpClientUtil.upload(uploadUrl, headers, params);
    }

    public static String completeUpload(String url, Integer chunks,
                                        Long filesize,
                                        String md5,
                                        String name,
                                        String version) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("chunks", "" + chunks);
        params.put("filesize", "" + filesize);
        params.put("md5", "" + md5);
        params.put("name", "" + name);
        params.put("version", "" + version);
        return HttpClientUtil.post(url, new HashMap<>(), params, true);

    }

    public static String appMyInfo(BiliSessionDto dto) throws IOException {
        String url = "https://app.bilibili.com/x/v2/account/myinfo";
        Map<String, String> params = new TreeMap<>();
        params.put("appkey", appKey);
        params.put("access_key", dto.getAccessToken());
        params.put("build", "5370000");
        params.put("channel", "html5_app_bili");
        params.put("mobi_app", "android");
        params.put("platform", "android");
        params.put("ts", "" + System.currentTimeMillis() / 1000);
        params.put("sign", sign(params, appSecret));
        Map<String, String> headers = new HashMap<>();
        long currentSecond = Instant.now().getEpochSecond();
        headers.put("Display-ID", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5-" + currentSecond);
        headers.put("Buvid", "XXD9E43D7A1EBB6669597650E3EE417D9E7F5");
        headers.put("User-Agent", "Mozilla/5.0 BiliDroid/5.37.0 (bbcallen@gmail.com)");
        headers.put("Device-ID", "aBRoDWAVeRhsA3FDewMzS3lLMwM");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);
        params.forEach(uriBuilder::queryParam);
        return HttpClientUtil.get(uriBuilder.toUriString(), headers);
    }

    /**
     * 初始化ws连接，在连接之前要调用三个接口组装成一个byte数组作为第一个请求数据发送
     * 给B站ws服务端
     *
     * @param url    这个ulr是调用这个接口之前的接口获取到的弹幕ws地址
     * @param roomId 房间号
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
        private String cover;
        private Integer online;
        private Long roomid;
        private Long broadcastType;
        private Long onlineHidden;
    }

    @Data
    public static class RoomInitDto {
        private Long room_id;
        private Integer live_status;
    }
}
