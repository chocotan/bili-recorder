package moe.chikalar.recorder.dto;

import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class ProgressDto {

    public ProgressDto(Boolean stopStatus) {
        this.stopStatus = new AtomicBoolean(stopStatus);
    }

    private Long startTime;
    private Long bytes = 0L;
    private AtomicBoolean stopStatus;

}
