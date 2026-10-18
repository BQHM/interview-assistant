package com.interview.modules.resume.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.common.model.AsyncTaskStatus;
import com.interview.infrastructure.file.DocumentParseService;
import com.interview.infrastructure.file.FileHashService;
import com.interview.infrastructure.file.FileStorageService;
import com.interview.infrastructure.file.FileValidationService;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.dto.ResumeUploadResponseDTO;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import com.interview.modules.resume.service.convert.ResumeUploadConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件功能说明
 * <p>负责简历上传业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadService {

    private final FileStorageService fileStorageService;
    private final ResumeRepository resumeRepository;
    private final FileValidationService fileValidationService;
    private final DocumentParseService documentParseService;
    private final FileHashService fileHashService;
    private final ResumeGradingService resumeGradingService;
    private final ResumeAnalysisPersistenceService resumeAnalysisPersistenceService;

    /**
     * 功能说明
     * <p>上传并保存简历。</p>
     *
     * @param file 简历文件
     * @return 简历上传结果
     * @author NobuNo
     * @date 2026-04-08
     */
    @Transactional
    public ResumeUploadResponseDTO uploadAndSave(MultipartFile file) {

        // 1. 上传前先做文件校验，尽早拦住非法输入
        String contentType = fileValidationService.validateResume(file);

        String originalFilename = file.getOriginalFilename();

        // 基于文件内容做去重，避免同一份简历被重复存储。
        String fileHash = fileHashService.calculate(file);

        Optional<ResumeEntity> optResumeEntity = resumeRepository.findByFileHash(fileHash);
        if (optResumeEntity.isPresent()) {
            ResumeEntity oldResumeEntity = optResumeEntity.get();
            log.info("命中重复简历，直接返回已有记录: resumeId={}, filename={}", oldResumeEntity.getId(), originalFilename);
            return ResumeUploadConverter.convertToResumeUploadResponseDTO(oldResumeEntity, true);
        }

        log.debug("识别到简历文件类型: filename={}, contentType={}", originalFilename, contentType);
        log.info("收到简历上传请求: {}", originalFilename);

        // 2. 从文件中提取简历正文文本，并做基础清洗
        String resumeText = documentParseService.parseResume(file);
        // 解析结果为空时直接失败，避免把无效简历写入存储和数据库。
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED);
        }
        log.info("简历文本解析成功: filename={}, textLength={}", originalFilename, resumeText.length());

        // 3. 上传原始文件到对象存储，返回文件存储路径 storageKey
        String storageKey = fileStorageService.uploadResume(file, contentType);
        log.info("文件已存储，Key: {}", storageKey);

        // 4. 组装数据库实体，保存文件元信息和解析后的文本
        ResumeEntity resumeEntity = new ResumeEntity();
        resumeEntity.setOriginalFilename(originalFilename);
        resumeEntity.setStorageKey(storageKey);
        resumeEntity.setFileSize(file.getSize());
        resumeEntity.setContentType(contentType);
        resumeEntity.setResumeText(resumeText);

        // 使用基于文件内容计算出的真实哈希值，后续可用于去重
        resumeEntity.setFileHash(fileHash);

        // 5. 持久化简历记录，uploadedAt 等字段由实体生命周期方法自动补全
        ResumeEntity savedResumeEntity = resumeRepository.save(resumeEntity);
        log.info("简历基础信息保存成功: resumeId={}", savedResumeEntity.getId());

        try {
            ResumeAnalysisResultDTO resumeAnalysisResultDTO = resumeGradingService.analyzeResume(resumeText);
            resumeAnalysisPersistenceService.saveAnalysis(savedResumeEntity, resumeAnalysisResultDTO);

            savedResumeEntity.setAnalyzeStatus(AsyncTaskStatus.COMPLETED);
            savedResumeEntity.setAnalyzeError(null);
            resumeRepository.save(savedResumeEntity);
            log.info("简历分析任务完成: resumeId={}", savedResumeEntity.getId());
        } catch (Exception e) {
            log.error("简历分析任务失败: {}", e.getMessage(), e);
            savedResumeEntity.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            savedResumeEntity.setAnalyzeError(e.getMessage());
            resumeRepository.save(savedResumeEntity);
        }

        return ResumeUploadConverter.convertToResumeUploadResponseDTO(savedResumeEntity, false);
    }
}
