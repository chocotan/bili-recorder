package moe.chikalar.recorder.dto;

import lombok.Data;

@Data
public class RecordConfig {
    private String roomId;
    private String uname;
    private String saveFolder;
    private Boolean danmuRecord = false;

    private Boolean convertToMp4 = false;
    private Boolean convertToMp4Delete = true;

    // 分割文件要求convertToMp4为true
    private Boolean splitFileByTime = false;
    private Integer splitFileDurationInSeconds = 3600;
    private Boolean splitFileBySize = false;
    private Long splitFileSizeInM = 4000L;
    private Boolean splitFileDeleteSource = false;

    private Boolean uploadToBili = false;
    private String uploadUsername;
    private String uploadPassword;

    private String uploadTitleTemplate = "【${uname}】 ${date}录播-${title}";
    private String uploadDescTemplate = "${uname} ${datetime} 录播 \n${uname}直播间：https://live.bilibili.com/${roomId}";
    private Integer uploadTid;
}
