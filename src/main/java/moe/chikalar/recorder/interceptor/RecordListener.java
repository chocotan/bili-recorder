package moe.chikalar.recorder.interceptor;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
import moe.chikalar.recorder.dto.RecordResult;
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
