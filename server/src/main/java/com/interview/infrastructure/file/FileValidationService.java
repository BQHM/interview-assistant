package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件功能说明
 * <p>负责上传文件基础校验。</p>
 *
 * @author NobuNo
 * @date 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class FileValidationService {


    private final ContentTypeDetectionService contentTypeDetectionService;

    /**
     * 功能说明
     * <p>校验简历文件。</p>
     *
     * @param file 简历文件
     * @return 文件内容类型
     * @author NobuNo
     * @date 2026-04-08
     */
    public String validateResume(MultipartFile file) {

        // 防止传空文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_FILE_EMPTY);
        }

        // 增加文件大小校验
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "简历文件大小不能超过 10MB");
        }

        // 基于文件内容检测真实 MIME 类型
        String contentType = contentTypeDetectionService.detectContentType(file);

        if (contentType == null) {
            throw new BusinessException(ErrorCode.RESUME_FILE_TYPE_NOT_SUPPORTED);
        }


        isSupportedResumeContentType(contentType);

        return contentType;
    }


    /**
     * 功能说明
     * <p>判断是否为支持的简历文件类型。</p>
     *
     * @param contentType 文件内容类型
     * @author NobuNo
     * @date 2026-04-08
     */
    private void isSupportedResumeContentType(String contentType) {
        // 只支持以下类型
        if (!contentType.equals("application/pdf")
                && !contentType.equals("application/msword")
                && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                && !contentType.equals("text/plain")) {
            throw new BusinessException(ErrorCode.RESUME_FILE_TYPE_NOT_SUPPORTED);
        }
    }
}
