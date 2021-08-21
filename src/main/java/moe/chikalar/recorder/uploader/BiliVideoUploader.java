package moe.chikalar.recorder.uploader;

import com.hiczp.bilibili.api.BilibiliClient;
import moe.chikalar.recorder.api.BiliApi;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.uploader.VideoUploader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BiliVideoUploader implements VideoUploader {
    @Override
    public void login(RecordConfig config, List<String> files) {
        try {
            BilibiliClient login = BiliApi.login(config.getUploadUsername(), config.getUploadPassword());
            System.out.println(login);
            Thread.sleep(1000000);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }










    public static void main(String[] args) {
        String file = "";
        RecordConfig config = new RecordConfig();
        new BiliVideoUploader().login(config, Collections.singletonList(file));
    }
}
