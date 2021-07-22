package moe.chikalar.recorder.dmj.bili.cmd;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class BaseCommand {
    private String roomId;
    private String cmd;
    private JSONObject data;
    private JSONArray info;
}
