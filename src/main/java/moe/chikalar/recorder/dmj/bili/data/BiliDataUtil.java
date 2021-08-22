package moe.chikalar.recorder.dmj.bili.data;

import moe.chikalar.recorder.struct.JavaStruct;
import moe.chikalar.recorder.struct.StructException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.HexUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BanqiJane
 */
public class BiliDataUtil {
    private static final Logger LOGGER = LogManager.getLogger(BiliDataUtil.class);

    /**
     * 处理父数据包 单线程引用写法
     */
    @SuppressWarnings("unused")
    public static List<String> handle_Message(ByteBuffer message) throws Exception {
        byte[] bytes = ByteUtils.decodeValue(message);
        byte[] bs = null;
        List<String> resultStr = new ArrayList<>();
        BiliMsg biliMsg = null;
        if (bytes.length > 0) {
            biliMsg = decode(bytes);
            int data_len = biliMsg.getPackageLength();
            int head_len = biliMsg.getPackageHeadLength();
            int data_ver = biliMsg.getPackageVersion();
            int data_type = biliMsg.getPackageType();
            int data_other = biliMsg.getPackageOther();
            bs = ByteUtils.subBytes(bytes, head_len, data_len - head_len);
            if (data_ver == 2) {
                if (data_type == 5) {
                    resultStr.addAll(BiliDataUtil.handle_zlibMessage(ByteUtils.BytesTozlibInflate(bs)));
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                }
            } else if (data_ver == 1) {
                if (data_type == 3) {
                    long byteslong = ByteUtils.byteslong(bs);
                    // TODO 房间人气
                } else if (data_type == 8) {
                    // 返回{code 0} 验证头消息成功后返回
                    resultStr.add(new String(bs, StandardCharsets.UTF_8));

                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                    LOGGER.debug("！！！！！！！！！！未知数据(1)v:" + data_ver + "t:" + data_type + ":" + resultStr);
                }
            } else if (data_ver == 0) {
                resultStr.add(new String(bs, StandardCharsets.UTF_8));
            } else {
                resultStr.add(HexUtils.toHexString(bs));
                LOGGER.debug("！！！！！！！！！！未知数据(1):" + resultStr);
            }
        }
        return resultStr;
    }

    /**
     * 处理解压后子数据包
     */
    @SuppressWarnings("unused")
    public static List<String> handle_zlibMessage(byte[] bytes) throws Exception {
        int offect = 0;
        List<String> resultStr = new ArrayList<>();
        int maxLen = bytes.length;
        byte[] byte_c = null;
        BiliMsg biliMsg = null;
        int data_len = 0;
        int head_len = 0;
        int data_ver = 0;
        int data_type = 0;
        int data_other = 0;
        byte[] bs = null;
        while (offect < maxLen) {
            byte_c = ByteUtils.subBytes(bytes, offect, maxLen - offect);
            biliMsg = decode(byte_c);
            data_len = biliMsg.getPackageLength();
            head_len = biliMsg.getPackageHeadLength();
            data_ver = biliMsg.getPackageVersion();
            data_type = biliMsg.getPackageType();
            data_other = biliMsg.getPackageOther();
            bs = ByteUtils.subBytes(byte_c, head_len, data_len - head_len);
            if (data_ver == 2) {
                if (data_type == 5) {
                    resultStr.add(ByteUtils.BytesTozlibInflateString(bs));
                    LOGGER.debug("其他未处理消息(2):" + resultStr);
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                    LOGGER.debug("！！！！！！！！！！未知数据(2)v:" + data_ver + "t:" + data_type + ":" + resultStr);
                }
            } else if (data_ver == 1) {
                if (data_type == 3) {
                    // TODO 人气
                    long byteslong = ByteUtils.byteslong(bs);
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                    // TODO 这是个啥玩意儿
                    LOGGER.debug("！！！！！！！！！！未知数据(2)v:" + data_ver + "t:" + data_type + ":" + resultStr);
                }
            } else if (data_ver == 0) {
                resultStr.add(new String(bs, "utf-8"));
            } else {
                resultStr.add(HexUtils.toHexString(bs));
                LOGGER.debug("！！！！！！！！！！未知数据(2):" + resultStr);
            }
            bs = null;
            offect += data_len;
        }
        return resultStr;
    }

    /**
     * 将接受到的b站ws响应byte数组转换为对象
     *
     * @param bytes 数据集
     * @return 转换后的弹幕对象
     */
    public static BiliMsg decode(byte[] bytes) {
        BiliMsg biliMsg = BiliMsg.getBarrageHeadHandle();
        try {
            JavaStruct.unpack(biliMsg, bytes);
        } catch (StructException e) {
            // ignored
        }
        return biliMsg;
    }

    /**
     * 弹幕集打包
     *
     * @param biliMsg 等待打包的弹幕响应对象
     * @return 打包后的byte array
     */
    public static byte[] encode(BiliMsg biliMsg) {
        byte[] b = null;
        try {
            b = JavaStruct.pack(biliMsg);
        } catch (StructException e) {
            // ignored
        }
        return b;
    }
}
