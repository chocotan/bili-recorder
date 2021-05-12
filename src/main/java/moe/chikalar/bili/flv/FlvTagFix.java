package moe.chikalar.bili.flv;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class FlvTagFix {
    public File fix(String flvPath, Boolean delete) throws IOException {
        return new FlvCheckerWithBufferEx().check(flvPath, delete);
    }
}
