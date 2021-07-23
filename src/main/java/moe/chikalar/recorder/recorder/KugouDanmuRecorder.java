//package moe.chikalar.recorder.recorder;
//
//import com.alibaba.fastjson.JSONArray;
//import io.reactivex.disposables.Disposable;
//import io.reactivex.subjects.PublishSubject;
//import javaslang.Tuple2;
//import lombok.extern.slf4j.Slf4j;
//import moe.chikalar.recorder.api.KugouApi;
//import moe.chikalar.recorder.dmj.bili.cmd.BaseCommand;
//import moe.chikalar.recorder.dmj.bili.data.ByteUtils;
//import moe.chikalar.recorder.dmj.kugou.JythonStruct;
//import moe.chikalar.recorder.dmj.kugou.KugouDanmu;
//import moe.chikalar.recorder.dmj.kugou.Struct;
//import okhttp3.WebSocket;
//import okio.ByteString;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.exception.ExceptionUtils;
//import org.apache.tomcat.util.buf.HexUtils;
//import org.python.core.PyInteger;
//import org.python.core.PyObject;
//import org.python.core.PyString;
//import org.python.modules.struct;
//
//import java.io.File;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//
//// currentTs^^(currentTs-startTs)^^uid^^uname^^content
//@Slf4j
//public class KugouDanmuRecorder implements DanmuRecorder {
//    private AtomicBoolean stop = new AtomicBoolean(false);
//    private long startTs;
//    private Thread heartBeatThread = null;
//    private final String template = "%d^^%d^^%d^^%s^^%s\r\n";
//    private WebSocket webSocket;
//    private PublishSubject<BaseCommand> ps;
//    private Disposable subscribe;
//
//
//    private KugouDanmuField MAGIC = new KugouDanmuField(0, 1, 100);
//    private KugouDanmuField VERSION = new KugouDanmuField(1, 2, 1);
//    private KugouDanmuField TYPE = new KugouDanmuField(2, 1, 1);
//    private KugouDanmuField HEADER = new KugouDanmuField(3, 2, 12);
//    private KugouDanmuField CMD = new KugouDanmuField(4, 4, 0);
//    private KugouDanmuField PAYLOAD = new KugouDanmuField(5, 4, 0);
//    private KugouDanmuField ATTR = new KugouDanmuField(6, 1, 0);
//    private KugouDanmuField CRC = new KugouDanmuField(7, 2, 0);
//    private KugouDanmuField SKIP = new KugouDanmuField(8, 1, 0);
//    private List<KugouDanmuField> fields = Arrays.asList(
//            MAGIC, VERSION, TYPE, HEADER, CMD, PAYLOAD, ATTR, CRC, SKIP
//    );
//
//    @Override
//    public void startRecord(String roomId, String fileName) {
//        this.startTs = System.currentTimeMillis();
//        this.ps = PublishSubject.create();
//        this.subscribe = this.ps.doOnNext(cmd -> {
//            try {
//                if ("DANMU_MSG".equals(cmd.getCmd())) {
//                    JSONArray info = cmd.getInfo();
//                    if (info != null) {
//                        String msg = (String) info.get(1);
//                        JSONArray userInfo = (JSONArray) info.get(2);
//                        Long uid = Long.valueOf("" + userInfo.get(0));
//                        String uname = (String) userInfo.get(1);
//                        String text = String.format(template, System.currentTimeMillis(),
//                                (System.currentTimeMillis() - startTs), uid, uname, msg);
//                        File file = new File(fileName);
//                        FileUtils.write(file, text, StandardCharsets.UTF_8, true);
//                    }
//                }
//            } catch (Exception e) {
//                log.info(ExceptionUtils.getStackTrace(e));
//            }
//        }).subscribe();
//        // 获取弹幕websocket地址
//        Tuple2<String, String> res = KugouApi.beforeInitWs(roomId);
//        // 构造protobuf对象
////        'appid': 1010,
////                'clientid': 105,
////                'cmd': 201,
////                'deviceNo': '4edc0e89-ccaf-452c-bce4-00f4cb6bb5bb',
////                'kugouid': 0,
////                'platid': 18,
////                'referer': 0,
////                'roomid': rid,
////                'sid': '8b9b79a7-a742-4397-fcc0-94efa3a1c920',
////                'soctoken': soctoken,
////                'v': 20191231
//        KugouDanmu.LoginRequest loginRequest = KugouDanmu.LoginRequest.newBuilder()
//                .setAppid(1010)
//                .setClientid(105)
//                .setCmd(201)
//                .setDeviceNo("4edc0e89-ccaf-452c-bce4-00f4cb6bb5bb")
//                .setKugouid(0)
//                .setPlatid(18)
//                .setReferer(0)
//                .setRoomid(Integer.parseInt(roomId))
//                .setSid("8b9b79a7-a742-4397-fcc0-94efa3a1c920")
//                .setSoctoken(res._2)
//                .setV(20191231)
//                .build();
//        KugouDanmu.Message message = KugouDanmu.Message.newBuilder()
//                .setContent(loginRequest.toByteString())
//                .build();
//
//        byte[] messageBytes = message.toByteArray();
//        byte[] encode = encode(messageBytes);
//
//
//        // 初始化websocket连接
//        this.webSocket = KugouApi.initWebsocket(res._1, roomId, ps);
//        // 发送认证信息
//        this.webSocket.send(ByteString.of(encode));
//        // 心跳
//        heartBeatThread = new Thread(() -> {
//            for (int i = 0; i < 10000000 && !stop.get(); i++) {
//                webSocket.send(ByteString.of(HexUtils.fromHexString(heartByte)));
//                try {
//                    Thread.sleep(30000);
//                } catch (InterruptedException e) {
//                    break;
//                }
//            }
//        });
//        heartBeatThread.start();
//    }
//
//    private String heartByte = "64000100";
//
//    public byte[] encode(byte[] bytes) {
//        int n = fields.size();
//        int i = bytes.length;
//        PAYLOAD.value = i;
//        CMD.value = 201;
//
//        byte[] res = new byte[]{};
//        for (KugouDanmuField field : fields) {
//            int value = field.value;
//            String fmt = "";
//            if (field.length == 1) {
//                fmt = "!b";
//            } else if (field.length == 2) {
//                fmt = "!h";
//            } else {
//                fmt = "!i";
//            }
//            try {
//                byte[] pack = JythonStruct.pack(fmt, value);
//                res = ByteUtils.mergeByteArrays(res, pack);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            res = ByteUtils.mergeByteArrays(res, new Struct().pack("!i", i));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        res = ByteUtils.mergeByteArrays(ArrayUtils.subarray(res, 0, g(n)),
//                bytes);
//        return res;
//    }
//
//    public int g(int... e) {
//        int t = 0;
//        if (e.length > 1 && e[1] != 0) {
//            t = e[1];
//        } else {
//            t = 12;
//        }
//        int n = 0;
//        int i = 0;
//        int ee = e[0];
//        while (i < ee && i < fields.size()) {
//            n += fields.get(i).length;
//            i += 1;
//        }
//        if (ee == fields.size()) {
//            return n + t - 12;
//        } else {
//            return n;
//        }
//    }
//
//    public byte[] v(byte[] e, KugouDanmuField t) {
//        String fmt = null;
//        if (t.length == 1) {
//            fmt = "!b";
//        } else if (t.length == 2) {
//            fmt = "!h";
//        } else {
//            fmt = "!i";
//        }
//
//        r, =struct.unpack_from(fmt, e, g(t.index));
//        return r
//    }
//
//    @Override
//    public void stop() {
//        stop.set(true);
//        heartBeatThread.interrupt();
//        if (this.webSocket != null) {
//            this.webSocket.close(1000, "");
//        }
//        if (this.ps != null) {
//            this.ps.onComplete();
//            this.subscribe.dispose();
//        }
//    }
//
//    public static class KugouDanmuField {
//        private Integer index;
//        private Integer length;
//        private int value;
//
//        KugouDanmuField(Integer index, Integer length, int value) {
//            this.index = index;
//            this.length = length;
//            this.value = value;
//        }
//
//        public Integer getIndex() {
//            return index;
//        }
//
//        public Integer getLength() {
//            return length;
//        }
//
//        public int getValue() {
//            return value;
//        }
//    }
//
//    public static void main(String[] args) {
//        KugouDanmuRecorder recorder = new KugouDanmuRecorder();
//        recorder.startRecord("4387125", "/tmp/test.log");
//    }
//}
