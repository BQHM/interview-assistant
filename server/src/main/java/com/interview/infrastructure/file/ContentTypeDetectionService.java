package com.interview.infrastructure.file;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件功能说明
 * <p>负责检测上传文件内容类型。</p>
 *
 * @author NobuNo
 * @date 2026-04-09
 */
@Service
public class ContentTypeDetectionService {

    private final Tika tika = new Tika();

    /**
     * 功能说明
     * <p>检测文件内容类型。</p>
     *
     * @param file 上传文件
     * @return 文件内容类型
     * @author NobuNo
     * @date 2026-04-09
     */
    public String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (IOException e) {
            return file.getContentType();
        }
    }

}
