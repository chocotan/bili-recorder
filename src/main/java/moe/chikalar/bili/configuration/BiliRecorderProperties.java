package moe.chikalar.bili.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "record")
public class BiliRecorderProperties {
    private Long checkInterval = 30L;
    private String workPath;
}
