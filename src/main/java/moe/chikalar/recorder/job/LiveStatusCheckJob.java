package moe.chikalar.recorder.job;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.recorder.RecordHelper;
import moe.chikalar.recorder.repo.RecordRoomRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
            try {

                List<RecordRoom> recordRooms = Lists.newArrayList(recordRoomRepository.findAll());
                recordRooms
                        .forEach(d -> {
                            if (!recordQueue.contains(d.getId()))
                                recordQueue.add(d.getId());
                        });
                Calendar cal = Calendar.getInstance();
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (hour < 8) {
                    // 23-8点降低频率
                    Thread.sleep((long) (properties.getCheckInterval()));

                }
            } catch (Exception ignored) {
                log.info(ExceptionUtils.getStackTrace(ignored));
            }
        }, 5, properties.getCheckInterval(), TimeUnit.SECONDS);
        startRecordPool();
    }

    public void addToRecordPool(Long id) {
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
                    if (recordHelper.get(id) == null) {
                        recordHelper.recordAndErrorHandle(currentRecordRoom);
                        Thread.sleep(5000L);
                    }
                } catch (Exception e) {
                    // ignored
                    log.info(ExceptionUtils.getStackTrace(e));
                }
            }
        });
    }
}
