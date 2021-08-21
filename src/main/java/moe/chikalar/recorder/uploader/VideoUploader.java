package moe.chikalar.recorder.uploader;

import com.hiczp.bilibili.api.member.model.AddResponse;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 视频上传接口
 */
public interface VideoUploader {

    AddResponse upload(RecordConfig config, RecordHistory recordHistory,
                       List<String> files) throws Exception;
}
