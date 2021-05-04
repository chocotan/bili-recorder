package moe.chikalar.bili.dto;

import lombok.Data;

@Data
public class RecordConfig {
    private String roomId;
    private String uname;
    private String saveFolder;
    private String moveFolder;
    private boolean debug = false;
    private Long retryInterval = 20L;
}
