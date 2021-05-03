package moe.chikalar.bili.job;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.recorder.RecordHelper;
import moe.chikalar.bili.recorder.Recorder;
import moe.chikalar.bili.recorder.RecorderFactory;
import moe.chikalar.bili.repo.RecordRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LiveStatusCheckJob implements CommandLineRunner {
    private final BiliRecorderProperties properties;

    private final RecordRoomRepository recordRoomRepository;

    private ScheduledExecutorService es = Executors.newScheduledThreadPool(2);

    private final RecordHelper recordHelper;

    public LiveStatusCheckJob(BiliRecorderProperties properties, RecordRoomRepository recordRoomRepository, RecorderFactory recorderFactory, RecordHelper recordHelper) {
        this.properties = properties;
        this.recordRoomRepository = recordRoomRepository;
        this.recordHelper = recordHelper;
    }

    public void run(String[] args) {
        // 刚启动的时候把所有ing的状态设为1
        recordRoomRepository.findByStatus("3")
                .forEach(r -> {
                    r.setStatus("1");
                    recordRoomRepository.save(r);
                });
        es.scheduleWithFixedDelay(() -> {
            // 获取所有启用的房间
            List<RecordRoom> recordRooms = recordRoomRepository.findByStatus("1");
            recordRooms
                    .forEach(d -> {
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException e) {
                            // ignored
                        }
                        recordHelper.recordAndErrorHandle(d);
                    });
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour < 8) {
                // 23-8点降低频率
                try {
                    Thread.sleep((long) (properties.getCheckInterval()));
                } catch (InterruptedException ignored) {
                }
            }
        }, 5, properties.getCheckInterval(), TimeUnit.SECONDS);

    }
}
