package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import java.io.IOException;

/**
 * 文件功能说明
 * <p>负责解析上传文档文本。</p>
 *
 * @author NobuNo
 * @since 2026-04-08
 */
@RequiredArgsConstructor
@Service
public class DocumentParseService {

    private final TextCleaningService textCleaningService;

    /**
     * 功能说明
     * <p>解析简历文件文本。</p>
     *
     * @param file 简历文件
     * @return 简历文本
     * @author NobuNo
     * @since 2026-04-08
     */
    public String parseResume(MultipartFile file) {
        try {
            Tika tika = new Tika();
            String text = tika.parseToString(file.getInputStream());
            return textCleaningService.clean(text);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "读取简历文件失败");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "简历解析失败");
        }
    }
}
