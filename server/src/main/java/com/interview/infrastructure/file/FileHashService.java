package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件功能说明
 * <p>负责计算上传文件哈希。</p>
 *
 * @author NobuNo
 * @since 2026-04-09
 */
@Service
public class FileHashService {

    /**
     * 功能说明
     * <p>计算文件哈希。</p>
     *
     * @param file 上传文件
     * @return 文件哈希
     * @author NobuNo
     * @since 2026-04-09
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
