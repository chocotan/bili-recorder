package moe.chikalar.recorder.dmj.bili.data;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.decoder.DecoderJNI;
import com.aayushatharva.brotli4j.decoder.DirectDecompress;
import com.aayushatharva.brotli4j.encoder.Encoder;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.struct.JavaStruct;
import moe.chikalar.recorder.struct.StructException;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
@Slf4j
public class BiliDataUtil {
    private static final Logger LOGGER = LogManager.getLogger(BiliDataUtil.class);

    static {
        try {
            Brotli4jLoader.ensureAvailability();
        } catch (Exception e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
        }
    }

    public static List<String> decodeMsg(ByteBuffer message) throws Exception {
        byte[] bytes = ByteUtils.decodeValue(message);
        byte[] bs;
        List<String> resultStr = new ArrayList<>();
        BiliMsg biliMsg;
        if (bytes.length > 0) {
            biliMsg = decode(bytes);
            int packageLength = biliMsg.getPackageLength();
            int packageHeadLength = biliMsg.getPackageHeadLength();
            int packageVersion = biliMsg.getPackageVersion();
            int dataType = biliMsg.getPackageType();
            int packageOther = biliMsg.getPackageOther();
            bs = ByteUtils.subBytes(bytes, packageHeadLength, packageLength - packageHeadLength);
            if (packageVersion == 2) {
                if (dataType == 5) {
                    resultStr.addAll(BiliDataUtil.deCompressZlib(ByteUtils.BytesTozlibInflate(bs)));
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                }
            } else if (packageVersion == 1) {
                if (dataType == 3) {
                    long byteslong = ByteUtils.byteslong(bs);
                    // TODO 房间人气
                } else if (dataType == 8) {
                    resultStr.add(new String(bs, StandardCharsets.UTF_8));

                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                }
            } else if (packageVersion == 0) {
                resultStr.add(new String(bs, StandardCharsets.UTF_8));
            } else if (packageVersion == 3) {
                DirectDecompress directDecompress = Decoder.decompress(bs); // or DirectDecompress.decompress(compressed);
                if (directDecompress.getResultStatus() == DecoderJNI.Status.DONE) {
                    resultStr.add(new String(directDecompress.getDecompressedData(), StandardCharsets.UTF_8));
                }

            } else {
                resultStr.add(HexUtils.toHexString(bs));
            }
        }
        return resultStr;
    }

    /**
     * 处理解压后子数据包
     */
    public static List<String> deCompressZlib(byte[] bytes) throws Exception {
        int offect = 0;
        List<String> resultStr = new ArrayList<>();
        int maxLen = bytes.length;
        byte[] byte_c;
        BiliMsg biliMsg;
        int data_len;
        int head_len;
        int data_ver;
        int data_type;
        int data_other = 0;
        byte[] bs;
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
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                }
            } else if (data_ver == 1) {
                if (data_type == 3) {
                    // TODO 人气
                    long byteslong = ByteUtils.byteslong(bs);
                } else {
                    resultStr.add(HexUtils.toHexString(bs));
                }
            } else if (data_ver == 0) {
                resultStr.add(new String(bs, StandardCharsets.UTF_8));
            } else {
                resultStr.add(HexUtils.toHexString(bs));
            }
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
