package moe.chikalar.bili.dto;

import lombok.Data;

@Data
public class RecordConfig {
    private String roomId;
    private String uname;
    private String uid;
    private Integer retryTimes = 0;
}
