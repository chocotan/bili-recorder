package moe.chikalar.bili.dmj.cmd;

import lombok.Data;

import java.util.Date;

@Data
public class UserEntryDto extends BaseMsgDto {
    private String medal;
    private String medalLevel;
    private String uname;
    private Long uid;
    private Date timestamp;
}
