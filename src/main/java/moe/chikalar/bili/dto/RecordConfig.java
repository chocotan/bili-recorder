package moe.chikalar.bili.dto;

import lombok.Data;

@Data
public class RecordConfig {
    private String roomId;
    private String uname;
    private String saveFolder;

    private boolean convertToMp4 = false;
    private boolean convertToMp4Delete = false;

    private Long retryInterval = 20L;
}
