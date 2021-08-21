package moe.chikalar.recorder.uploader;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordHistory;

import java.util.List;

public interface VideoUploader {

    void upload(RecordConfig config, RecordHistory recordHistory, List<String> files);
}
