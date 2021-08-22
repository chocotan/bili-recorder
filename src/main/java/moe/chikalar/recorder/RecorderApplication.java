package moe.chikalar.recorder;

import moe.chikalar.recorder.utils.HttpsTrustManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.LinkedList;

@SpringBootApplication
@EnableScheduling
@ServletComponentScan
public class RecorderApplication {

    public static void main(String[] args) {
        HttpsTrustManager.allowAllSSL();
        SpringApplication.run(RecorderApplication.class, args);
    }


    @Bean
    public LinkedList<Long> recordQueue() {
        return new LinkedList<>();
    }

}
