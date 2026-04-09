package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FileHashService {

    public String calculate(MultipartFile file){
//        1. 如果 file == null 或空，抛业务异常
//        2. 读取 file.getBytes()
//        3. MessageDigest.getInstance("SHA-256")
//        4. digest.update(bytes)
//        5. byte[] hashBytes = digest.digest()
//        6. 转成十六进制字符串
//        7. return hashString
        if (file==null||file.isEmpty()){
            throw new BusinessException(ErrorCode.RESUME_FILE_EMPTY);
        }
        try {
            byte[] fileBytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }catch (Exception e){
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "计算文件哈希失败");
        }
    }

}
