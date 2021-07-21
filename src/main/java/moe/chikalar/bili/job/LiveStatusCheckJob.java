package moe.chikalar.bili.job;

import lombok.extern.slf4j.Slf4j;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.entity.RecordRoom;
import moe.chikalar.bili.recorder.RecordHelper;
import moe.chikalar.bili.repo.RecordRoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
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

    private LinkedList<Long> recordQueue;


    private ExecutorService recordPool = Executors.newFixedThreadPool(1);

    public LiveStatusCheckJob(BiliRecorderProperties properties,
                              RecordRoomRepository recordRoomRepository,
                              RecordHelper recordHelper,
                              LinkedList<Long> recordQueue) {
        this.properties = properties;
        this.recordRoomRepository = recordRoomRepository;
        this.recordHelper = recordHelper;
        this.recordQueue = recordQueue;
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
            // 检查上次录制时间
            List<RecordRoom> recordRooms = recordRoomRepository.findByStatus("1");
            recordRooms
                    .forEach(d -> {
                        if (!recordQueue.contains(d.getId()))
                            recordQueue.add(d.getId());
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
        startRecordPool();
    }

    public void addToRecordPool(Long id){
        recordQueue.add(id);
    }

    public void startRecordPool() {
        recordPool.submit(() -> {
            for (; ; ) {
                try {
                    Long id = recordQueue.poll();
                    if (id == null) {
                        Thread.sleep(6000);
                        continue;
                    }
                    Optional<RecordRoom> opt = recordRoomRepository.findById(id);
                    if (!opt.isPresent()) {
                        return;
                    }
                    RecordRoom currentRecordRoom = opt.get();
                    // 只有没有在录播中的 才录制
                    if (currentRecordRoom.getStatus().equals("1")) {
                        recordHelper.recordAndErrorHandle(currentRecordRoom);
                        Thread.sleep(6000L);
                    }
                } catch (Exception e) {
                    // ignored
                }
            }
        });
    }
}
