package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordContext;
import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.core.Ordered;

public interface RecordListener extends Ordered {
    public default void beforeRecord(RecordContext context, RecordConfig config) {
    }

    public default RecordResult afterRecord(RecordResult recordResult, RecordConfig config){
        return recordResult;
    }

    public default int getOrder() {
        return 0;
    }

}
