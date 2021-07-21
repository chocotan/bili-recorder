package moe.chikalar.bili.interceptor;

import moe.chikalar.bili.dto.RecordConfig;
import moe.chikalar.bili.dto.RecordContext;
import moe.chikalar.bili.entity.RecordRoom;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;


@Component
public class ChangeSaveFolder implements RecordListener {
    @Override
    public void beforeRecord(RecordContext context, RecordConfig config) {
        if (StringUtils.isBlank(config.getSaveFolder())) {
            return;
        }
        String path = context.getPath();
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        fileName = config.getSaveFolder() + "/" + fileName;
        File folderFile = new File(config.getSaveFolder());
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        context.setPath(fileName);
    }
}
