package moe.chikalar.bili;

import com.hybhub.util.concurrent.ConcurrentSetBlockingQueue;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;

@SpringBootApplication
@EnableScheduling
public class BiliRecorderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiliRecorderApplication.class, args);
    }


    @Bean
    public LinkedList<Long> recordQueue() {
        return new LinkedList<>();
    }
}
