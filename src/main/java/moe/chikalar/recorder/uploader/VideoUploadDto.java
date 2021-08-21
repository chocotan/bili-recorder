package moe.chikalar.recorder.uploader;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VideoUploadDto {
    private Integer build = 1054;
    private Integer copyright = 2;
    private String cover = "";
    private String desc = "";
    private Integer no_reprint = 0;
    private Integer open_elec = 1;
    private String source = "直播";
    private String tag = "";
    private Integer tid = 27;
    private String title = "";
    private String dynamic = "";
    private List<SingleVideoDto> videos = new ArrayList<>();
}
