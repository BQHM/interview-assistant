package com.interview.infrastructure.file;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于文件内容探测真实 MIME 类型。
 */
@Service
public class ContentTypeDetectionService {

    private final Tika tika = new Tika();

    /**
     * 优先根据文件内容检测类型，读取失败时再回退到请求头中的 Content-Type。
     */
    public String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            return file.getContentType();
        }
    }

}
