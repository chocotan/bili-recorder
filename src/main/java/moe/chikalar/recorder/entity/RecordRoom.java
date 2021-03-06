package moe.chikalar.recorder.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Data;
import moe.chikalar.recorder.dto.RecordConfig;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
public class RecordRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomId;
    private String uname;
    private Long uid;
    // bili-1 kugou-2
    private String type;
    // 1-enable 2-disable 3-recording
    private String status;

    private String title;


    // 上次的异常
    @Lob
    private String lastError;

    // 大字段
    @Lob
    private String data;

    private Date createTime;
    private Date updateTime;

    @Transient
    private Long dataSize;

    @Transient
    public String getStatusText() {
        if (status != null) {
            switch (status) {
                case "1":
                    return "启用";
                case "2":
                    return "禁用";
                case "3":
                    return "录制中";
            }
        }
        return "未知数据，请删除";
    }

    @Transient
    public String getFormattedData() {
        try {
            return JSON.toJSONString(JSON.parseObject(data, RecordConfig.class), SerializerFeature.PrettyFormat,
                    SerializerFeature.WriteMapNullValue);
        } catch (Exception e) {
            return data;
        }
    }

    @Transient
    public Boolean getUploadToBili() {
        try {
            RecordConfig recordConfig = JSON.parseObject(data, RecordConfig.class);
            return recordConfig.getUploadToBili() != null && recordConfig.getUploadToBili();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecordRoom that = (RecordRoom) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
