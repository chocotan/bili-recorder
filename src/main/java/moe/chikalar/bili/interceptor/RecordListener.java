package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.core.Ordered;

public interface RecordListener extends Ordered {
    public default String beforeRecord(RecordRoom recordRoom, RecordConfig config, String path) {
        return path;
    }

    public default RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult, RecordConfig config){
        return recordResult;
    }

    public default int getOrder() {
        return 0;
    }

}
