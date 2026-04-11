package com.interview.modules.resume;

import com.interview.common.result.Result;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.service.ResumeQueryService;
import com.interview.modules.resume.service.ResumeUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历模块的 HTTP 入口。
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeUploadService uploadService;
    private final ResumeQueryService resumeQueryService;

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

    @GetMapping("/{id}")
    /**
     * 根据主键查询单份简历详情。
     */
    public Result<ResumeEntity> getById(@PathVariable Long id) {
        ResumeEntity resume = resumeQueryService.getById(id);
        return Result.success(resume);
    }

}
