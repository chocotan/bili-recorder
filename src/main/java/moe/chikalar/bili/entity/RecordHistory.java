package moe.chikalar.bili.entity;

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

}
