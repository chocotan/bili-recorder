package moe.chikalar.recorder.uploader;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;

import java.util.List;

/**
 * 视频上传接口
 */
public interface VideoUploader {
    String upload2(RecordConfig config, RecordHistory recordHistory, List<String> files) throws Exception;
}
