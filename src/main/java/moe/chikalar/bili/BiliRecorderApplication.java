package moe.chikalar.bili;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BiliRecorderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiliRecorderApplication.class, args);
    }

}
