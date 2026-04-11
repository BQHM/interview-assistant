package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 基于文件内容计算稳定哈希，用于简历去重。
 */
@Service
public class FileHashService {

    /**
     * 当前使用 SHA-256，保证同一文件内容得到相同哈希值。
     */
    public String calculate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_FILE_EMPTY);
        }
        try {
            byte[] fileBytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            // 将摘要字节转成十六进制字符串，便于存库和比较。
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计算文件哈希失败");
        }
    }

}
