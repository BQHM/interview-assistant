package com.interview.infrastructure.file;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import java.io.IOException;

@RequiredArgsConstructor
@Service
public class DocumentParseService {

    private final TextCleaningService textCleaningService;

    public String parseResume(MultipartFile file) {
        try {
            Tika tika = new Tika();
            String text = tika.parseToString(file.getInputStream());
            String cleanedText = textCleaningService.clean(text);
            return cleanedText;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "读取简历文件失败");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED, "简历解析失败");
        }
    }
}
