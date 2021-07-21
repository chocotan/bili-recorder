package moe.chikalar.bili.dto;

import lombok.Data;
import moe.chikalar.bili.entity.RecordRoom;

import java.util.HashMap;
import java.util.Map;

@Data
public class RecordContext {
    private RecordRoom recordRoom;
    private String path;
    private Map<String, Object> attributes = new HashMap<>();

    public RecordContext() {
    }

    public void addAttribute(String key, Object attribute){
        attributes.put(key ,attribute);
    }

    public <T> T getAttribute(String key){
        return (T) attributes.get(key);
    }
}
