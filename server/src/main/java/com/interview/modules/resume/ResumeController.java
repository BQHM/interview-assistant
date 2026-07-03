package com.interview.modules.resume;

import com.interview.common.result.Result;
import com.interview.modules.resume.model.dto.ResumeAnalysisDTO;
import com.interview.modules.resume.model.dto.ResumeDetailDTO;
import com.interview.modules.resume.model.dto.ResumeListItemDTO;
import com.interview.modules.resume.model.dto.ResumeUploadResponseDTO;
import com.interview.modules.resume.service.ResumeAnalysisQueryService;
import com.interview.modules.resume.service.ResumeQueryService;
import com.interview.modules.resume.service.ResumeUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件功能说明
 * <p>负责简历模块接口入口。</p>
 *
 * @author NobuNo
 * @date 2026-04-02
 */
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeUploadService uploadService;
    private final ResumeQueryService resumeQueryService;
    private final ResumeAnalysisQueryService resumeAnalysisQueryService;

    /**
     * 功能说明
     * <p>上传简历文件。</p>
     *
     * @param file 简历文件
     * @return 简历上传结果
     * @author NobuNo
     * @date 2026-04-02
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

    /**
     * 功能说明
     * <p>查询简历详情。</p>
     *
     * @param id 简历编号
     * @return 简历详情
     * @author NobuNo
     * @date 2026-04-02
     */
    @GetMapping("/{id}")
    public Result<ResumeDetailDTO> getById(@PathVariable Long id) {
        ResumeDetailDTO cplResumeDetailDTO = resumeQueryService.getById(id);
        return Result.success(cplResumeDetailDTO);
    }

    /**
     * 功能说明
     * <p>查询简历分析结果。</p>
     *
     * @param id 简历编号
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-02
     */
    @GetMapping("/{id}/analysis")
    public Result<ResumeAnalysisDTO> getResumeAnalysisById(@PathVariable Long id) {
        ResumeAnalysisDTO resume = resumeAnalysisQueryService.getResumeAnalysis(id);
        return Result.success(resume);
    }

    /**
     * 功能说明
     * <p>查询简历列表。</p>
     *
     * @return 简历列表
     * @author NobuNo
     * @date 2026-04-02
     */
    @GetMapping
    public Result<List<ResumeListItemDTO>> listResumes() {
        List<ResumeListItemDTO> lstResumeList = resumeQueryService.listResumes();
        return Result.success(lstResumeList);
    }

}
