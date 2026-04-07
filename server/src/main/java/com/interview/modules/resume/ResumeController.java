package com.interview.modules.resume;

import com.interview.common.result.Result;
import com.interview.modules.resume.service.ResumeUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeUploadService uploadService;

    /**
     * 上传简历接口
     * POST /api/resumes/upload
     */
    @PostMapping("/upload")
    public Result<Long> upload(@RequestParam("file") MultipartFile file) {
        // 调用业务层
        Long resumeId = uploadService.uploadAndSave(file);

        // 返回统一结果
        return Result.success(resumeId);
    }

}
