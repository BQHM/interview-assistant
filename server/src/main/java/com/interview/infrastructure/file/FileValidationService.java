package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历文件上传前的基础校验。
 */
@Service
@RequiredArgsConstructor
public class FileValidationService {


    private final ContentTypeDetectionService contentTypeDetectionService;

    /**
     * 当前只做最小校验：判空、大小限制、MIME 类型白名单。
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
