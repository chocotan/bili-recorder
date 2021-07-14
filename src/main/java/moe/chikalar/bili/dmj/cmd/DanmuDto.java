package moe.chikalar.bili.dmj.cmd;

import lombok.Data;

@Data
public class DanmuDto extends BaseMsgDto {
    private String band;
    private String bandLevel;
    private Long uid;
    private String uname;
    private String msg;

}
