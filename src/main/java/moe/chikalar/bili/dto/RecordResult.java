package moe.chikalar.bili.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RecordResult {
    private Boolean success;
    private String filePath;
    private Exception exception;

    private RecordResult(){}

    public static RecordResult success(String filePath){
        RecordResult recordResult = new RecordResult();
        recordResult.success = true;
        recordResult.filePath = filePath;
        return recordResult;
    }

    public static RecordResult error(Exception e){
        RecordResult recordResult = new RecordResult();
        recordResult.exception = e;
        recordResult.success = false;
        return recordResult;
    }
}
