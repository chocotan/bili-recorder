package moe.chikalar.bili.dmj.cmd;

import lombok.Data;

@Data
public class GiftDto extends BaseMsgDto {
    private Long uid;
    private String uname;
    private String giftName;
    private Long giftId;
    private Long price;
    private Long num;


}