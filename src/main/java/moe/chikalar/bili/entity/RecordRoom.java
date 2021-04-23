package moe.chikalar.bili.entity;

import lombok.Data;
import lombok.extern.java.Log;

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
    // bili-1
    private String type;
    // 1-enable 2-disable 3-recording
    private String status;

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
}
