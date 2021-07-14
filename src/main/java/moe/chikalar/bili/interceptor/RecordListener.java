package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.core.Ordered;

public interface RecordListener extends Ordered {
    public default void beforeRecord(RecordRoom recordRoom, RecordConfig config, String path) {
    }

    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config);

    public default int getOrder() {
        return 0;
    }

}
