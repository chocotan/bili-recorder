package moe.chikalar.bili.controller;

import lombok.Data;
import moe.chikalar.bili.configuration.BiliRecorderProperties;
import org.apache.commons.io.IOUtils;
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
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@RequestMapping("file")
@Controller
public class FileListController {
    @Autowired
    BiliRecorderProperties recorderProperties;

    @RequestMapping("download")
    public ResponseEntity<Resource> download(String path, HttpServletResponse response) {
        String workPath = recorderProperties.getWorkPath();
        if (workPath.endsWith("/")) {
            workPath = workPath.substring(0, workPath.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String currentPath = workPath + path;
        File file = new File(currentPath);
        if (file.exists() && file.isDirectory()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
//        MediaType mediaType = MediaTypeFactory
//                .getMediaType(resource)
//                .orElse(MediaType.APPLICATION_OCTET_STREAM);
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
    public String list(Model model, @RequestParam(defaultValue = "/") String path) {
        String workPath = recorderProperties.getWorkPath();
        if (workPath.endsWith("/")) {
            workPath = workPath.substring(0, workPath.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String currentPath = workPath + path;
        model.addAttribute("currentPath", currentPath);
        model.addAttribute("isRoot", path.equals("/"));

        if (!path.equals("/"))
            model.addAttribute("return", path.substring(0, path.lastIndexOf("/")));

        File file = new File(currentPath);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if(!path.endsWith("/")){
                path = path + "/";
            }
            String finalPath = path;
            List<FileDto> fileList = Arrays.stream(files)
                    .map(d -> {
                        FileDto f = new FileDto();
                        f.setName(d.getName());
                        f.setFile(d.isFile());
                        f.setLastModified(new Date(d.lastModified()));
                        f.setFileLength(d.isFile() ? humanReadableByteCountBin(d.length()):"0M");
                        f.setPath(finalPath + d.getName());
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
        private String name;
        private Boolean file;
        private String fileLength;
        private Date lastModified;
        private String path;
    }
}
