package com.interview.modules.resume;

import com.interview.common.result.Result;
import com.interview.modules.resume.model.ResumeAnalysisDTO;
import com.interview.modules.resume.model.ResumeAnalysisEntity;
import com.interview.modules.resume.model.ResumeDetailDTO;
import com.interview.modules.resume.model.ResumeUploadResponseDTO;
import com.interview.modules.resume.service.ResumeAnalysisQueryService;
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
    private final ResumeAnalysisQueryService resumeAnalysisQueryService;

    /**
     * 上传简历接口
     * POST /api/resumes/upload
     */
    @PostMapping("/upload")
    public Result<ResumeUploadResponseDTO> upload(@RequestParam("file") MultipartFile file) {
        // 调用业务层
        ResumeUploadResponseDTO resumeDTO = uploadService.uploadAndSave(file);

        // 重复简历不再按异常处理，而是直接返回已有记录信息。
        if (resumeDTO.getDuplicate()) {
            return Result.success("检测到相同简历，已返回已有记录", resumeDTO);
        } else {
            return Result.success(resumeDTO);
        }
    }

    @GetMapping("/{id}")
    /**
     * 根据主键查询单份简历详情。
     */
    public Result<ResumeDetailDTO> getById(@PathVariable Long id) {
        ResumeDetailDTO cplResumeDetailDTO = resumeQueryService.getById(id);
        return Result.success(cplResumeDetailDTO);
    }

    @GetMapping("/{id}/analysis")
    /**
     * 根据主键查询单份简历结果。
     */
    public Result<ResumeAnalysisDTO> getResumeAnalysisById(@PathVariable Long id) {
        ResumeAnalysisDTO resume = resumeAnalysisQueryService.getResumeAnalysis(id);
        return Result.success(resume);
    }

}
