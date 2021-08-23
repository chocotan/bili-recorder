package moe.chikalar.recorder.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
public class RecordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private RecordRoom recordRoom;
    private Date startTime;
    private Date endTime;
    private String filePath;
    private String danmuPath;
    private Long fileSize;
    private Long fileLength;
    private String status;
    private String title;

    // 0-不需要上传，1-待上传，2-正在上传，3-上传成功
    private String uploadStatus = "0";
    private Integer uploadRetryCount = 0;

    private Date updateTime;

    @Lob
    private String extraFiles = "";


    private Long realStartTime;
}
