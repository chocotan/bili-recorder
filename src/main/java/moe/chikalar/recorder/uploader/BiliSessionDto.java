package moe.chikalar.recorder.uploader;

import lombok.Data;

@Data
public class BiliSessionDto {
    private String accessToken;
    private String refreshToken;
    private String mid;
    private long createTime;

}
