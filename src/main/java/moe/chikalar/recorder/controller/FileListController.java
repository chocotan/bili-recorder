package moe.chikalar.recorder.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.Data;
import moe.chikalar.recorder.configuration.BiliRecorderProperties;
import moe.chikalar.recorder.dto.RecordConfig;
import moe.chikalar.recorder.entity.RecordRoom;
import moe.chikalar.recorder.repo.RecordRoomRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RequestMapping("file")
@Controller
public class FileListController {
    @Autowired
    private BiliRecorderProperties recorderProperties;
    @Autowired
    private RecordRoomRepository recordRoomRepository;


    @RequestMapping("download")
    public ResponseEntity<Resource> download(String path,
                                             Long id,
                                             HttpServletResponse response) {
        Optional<RecordRoom> r = recordRoomRepository.findById(id);
        if (!r.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        RecordConfig recordConfig = JSON.parseObject(r.get().getData(), RecordConfig.class);
        String folder = recorderProperties.getWorkPath();
        if (folder.endsWith(File.separator)) {
            folder = folder.substring(0, folder.length() - 1);
        }
        if (StringUtils.isNotBlank(recordConfig.getSaveFolder())) {
            folder = recordConfig.getSaveFolder();
        } else {
            folder = folder + File.separator + r.get().getUname();
        }
        String workPath = folder;
        if (workPath.endsWith(File.separator)) {
            workPath = workPath.substring(0, workPath.length() - 1);
        }
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        String currentPath = workPath + path;
        File file = new File(currentPath);
        if (file.exists() && file.isDirectory()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                .inline()
                .filename(file.getName())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @RequestMapping("list")
    public String list(Model model, String path,
                       Long id) {
        if (StringUtils.isBlank(path)) {
            path = File.separator;
        }
        String workPath = recorderProperties.getWorkPath();
        if (workPath.endsWith(File.separator)) {
            workPath = workPath.substring(0, workPath.length() - 1);
        }

        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        // 当前的绝对路径
        String currentPath = workPath + path;
        model.addAttribute("isFirstLevel", false);
        if (id != null && File.separator.equals(path)) {
            Optional<RecordRoom> r = recordRoomRepository.findById(id);
            if (r.isPresent()) {
                RecordConfig recordConfig = JSON.parseObject(r.get().getData(), RecordConfig.class);
                currentPath = StringUtils.isNotBlank(recordConfig.getSaveFolder()) ?
                        recordConfig.getSaveFolder() : (recorderProperties.getWorkPath() + File.separator
                        + r.get().getUname());
                model.addAttribute("isFirstLevel", true);
            }
        }
        model.addAttribute("currentPath", currentPath);
        model.addAttribute("isRoot", path.equals(File.separator));

        if (!path.equals(File.separator))
            model.addAttribute("return", path.substring(0, path.lastIndexOf(File.separator)));
        else {
            // 当path=/的时候，查询所有主播的目录，而不是根目录
            if (id == null) {
                List<FileDto> collect = Lists.newArrayList(recordRoomRepository.findAll())
                        .stream()
                        .map(r -> {
                            RecordConfig recordConfig = JSON.parseObject(r.getData(), RecordConfig.class);
                            boolean useCustomFolder = StringUtils.isBlank(recordConfig.getSaveFolder());
                            String saveFolder = useCustomFolder
                                    ? recorderProperties.getWorkPath() : recordConfig.getSaveFolder();
                            File saveFolderFile = new File(saveFolder);
                            FileDto f = new FileDto();
                            f.setName(r.getUname());
                            f.setFile(false);
                            f.setLastModified(new Date(saveFolderFile.lastModified()));
                            f.setFileLength("");
                            f.setPath("");
                            f.setId(r.getId());
                            return f;
                        }).collect(Collectors.toList());
                model.addAttribute("fileList", collect);
                return "fileList";
            }
        }


        File file = new File(currentPath);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files, (a, b) -> (int) (b.lastModified() / 1000 -
                    a.lastModified() / 1000));
            if (!path.endsWith(File.separator)) {
                path = path + File.separator;
            }
            String finalPath = path;
            List<FileDto> fileList = Arrays.stream(files)
                    .map(d -> {
                        FileDto f = new FileDto();
                        f.setName(d.getName());
                        f.setFile(d.isFile());
                        f.setLastModified(new Date(d.lastModified()));
                        f.setFileLength(d.isFile() ? humanReadableByteCountBin(d.length()) : "0M");
                        f.setPath(finalPath + d.getName());
                        f.setId(id);
                        return f;
                    }).collect(Collectors.toList());

            model.addAttribute("fileList", fileList);
        }
        return "fileList";
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }

    @Data
    public static class FileDto {
        private Long id;
        private String name;
        private Boolean file;
        private String fileLength;
        private Date lastModified;
        private String path;
    }
}
