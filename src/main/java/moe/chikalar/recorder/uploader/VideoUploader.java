package moe.chikalar.recorder.uploader;

import moe.chikalar.recorder.dto.RecordConfig;

import java.util.List;

public interface VideoUploader {

    void login(RecordConfig config, List<String> files);
}
