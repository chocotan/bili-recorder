package moe.chikalar.recorder.interceptor;

import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.dto.RecordContext;
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
