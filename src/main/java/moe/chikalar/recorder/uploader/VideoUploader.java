package moe.chikalar.recorder.uploader;

import com.hiczp.bilibili.api.member.model.AddResponse;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 视频上传接口
 */
public interface VideoUploader {
    String upload2(RecordConfig config, RecordHistory recordHistory, List<String> files) throws Exception;
}
