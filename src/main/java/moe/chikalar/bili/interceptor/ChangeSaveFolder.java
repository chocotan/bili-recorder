package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.entity.RecordRoom;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;


@Component
public class ChangeSaveFolder implements RecordListener {
    @Override
    public String beforeRecord(RecordRoom recordRoom, RecordConfig config, String path) {
        if (StringUtils.isBlank(config.getSaveFolder())) {
            return path;
        }
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        fileName = config.getSaveFolder() + "/" + fileName;
        File folderFile = new File(config.getSaveFolder());
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        return fileName;
    }
}
