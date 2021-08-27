package moe.chikalar.recorder.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "record")
public class BiliRecorderProperties {
    private Long checkInterval = 40L;
    private String workPath;
    private Long uploadReties = 5L;
    private Long uploadFileSizeMin = 10 * 1024 * 1024L;
}
