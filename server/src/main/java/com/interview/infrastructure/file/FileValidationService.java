package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    public void validateResume(MultipartFile file) {

        // 防止传空文件
        if(file == null||file.isEmpty()){
            throw new BusinessException(ErrorCode.RESUME_FILE_EMPTY);
        }

        // 增加文件大小校验
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "简历文件大小不能超过 10MB");
        }

        // 防止陌生的文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.RESUME_FILE_TYPE_NOT_SUPPORTED);
        }

        // 只支持以下类型
        if (!contentType.equals("application/pdf")
                && !contentType.equals("application/msword")
                && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                && !contentType.equals("text/plain")) {
            throw new BusinessException(ErrorCode.RESUME_FILE_TYPE_NOT_SUPPORTED);
        }
    }
}
