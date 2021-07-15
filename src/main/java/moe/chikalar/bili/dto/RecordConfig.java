package moe.chikalar.bili.dto;

import lombok.Data;

@Data
public class RecordConfig {
    private String roomId;
    private String uname;
    private String saveFolder;
    private Long retryInterval = 20L;

    private Boolean danmuRecord = true;


    private Boolean convertToMp4 = false;
    private Boolean convertToMp4Delete = true;

    // 分割文件要求convertToMp4为true
    private Boolean splitFileByTime = false;
    private Integer splitFileDurationInSeconds = 3600;
    private Boolean splitFileBySize = false;
    private Integer splitFileSizeInM = 4000;
    private Boolean splitFileDeleteSource = false;


}
