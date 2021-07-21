package moe.chikalar.bili.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RecordResult {
    private Boolean success;
    private RecordContext context;
    private Exception exception;

    private RecordResult(){}

    public static RecordResult success(RecordContext context){
        RecordResult recordResult = new RecordResult();
        recordResult.success = true;
        recordResult.context = context;
        return recordResult;
    }

    public static RecordResult error(Exception e, RecordContext context){
        RecordResult recordResult = new RecordResult();
        recordResult.exception = e;
        recordResult.success = false;
        recordResult.context = context;
        return recordResult;
    }
}
