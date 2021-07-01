package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordResult;
import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.core.Ordered;

public interface RecordInterceptor extends Ordered {
    public RecordResult afterRecord(RecordRoom recordRoom, RecordResult recordResult);
    public default int getOrder(){
        return 0;
    }

}
