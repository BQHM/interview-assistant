package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.common.model.AsyncTaskStatus;
import com.interview.infrastructure.file.DocumentParseService;
import com.interview.infrastructure.file.FileHashService;
import com.interview.infrastructure.file.FileStorageService;
import com.interview.infrastructure.file.FileValidationService;
import com.interview.modules.resume.model.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.model.ResumeUploadResponseDTO;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

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
     * 简历上传主流程。
     * <p>
     * 当前版本负责 4 件事：
     * 1. 校验上传文件是否合法
     * 2. 解析并清洗简历文本
     * 3. 上传原始文件到 RustFS / S3
     * 4. 保存简历元信息和文本到数据库
     *
     * @param file 用户上传的 MultipartFile
     * @return 上传成功后的简历基础返回信息
     */
    @Transactional
    public ResumeUploadResponseDTO uploadAndSave(MultipartFile file) {

        // 1. 上传前先做文件校验，尽早拦住非法输入
        String strContentType = fileValidationService.validateResume(file);

        String strOriginalFilename = file.getOriginalFilename();

        // 基于文件内容做去重，避免同一份简历被重复存储。
        String strFileHash = fileHashService.calculate(file);

        Optional<ResumeEntity> optResumeEntity = resumeRepository.findByFileHash(strFileHash);
        if (optResumeEntity.isPresent()) {
            ResumeEntity tblOldResumeEntity = optResumeEntity.get();
            log.info("命中重复简历，直接返回已有记录: resumeId={}, filename={}", tblOldResumeEntity.getId(), strOriginalFilename);
            return convertResumeUploadResponseDTO(tblOldResumeEntity, true);
        }

        log.debug("识别到简历文件类型: filename={}, contentType={}", strOriginalFilename, strContentType);
        log.info("收到简历上传请求: {}", strOriginalFilename);

        // 2. 从文件中提取简历正文文本，并做基础清洗
        String strResumeText = documentParseService.parseResume(file);
        // 解析结果为空时直接失败，避免把无效简历写入存储和数据库。
        if (strResumeText == null || strResumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED);
        }
        log.info("简历文本解析成功: filename={}, textLength={}", strOriginalFilename, strResumeText.length());

        // 3. 上传原始文件到对象存储，返回文件存储路径 storageKey
        String strStorageKey = fileStorageService.uploadResume(file, strContentType);
        log.info("文件已存储，Key: {}", strStorageKey);

        // 4. 组装数据库实体，保存文件元信息和解析后的文本
        ResumeEntity tblResumeEntity = new ResumeEntity();
        tblResumeEntity.setOriginalFilename(strOriginalFilename);
        tblResumeEntity.setStorageKey(strStorageKey);
        tblResumeEntity.setFileSize(file.getSize());
        tblResumeEntity.setContentType(strContentType);
        tblResumeEntity.setResumeText(strResumeText);

        // 使用基于文件内容计算出的真实哈希值，后续可用于去重
        tblResumeEntity.setFileHash(strFileHash);

        // 5. 持久化简历记录，uploadedAt 等字段由实体生命周期方法自动补全
        ResumeEntity tblSavedResumeEntity = resumeRepository.save(tblResumeEntity);
        log.info("简历基础信息保存成功: resumeId={}", tblSavedResumeEntity.getId());

        try {
            ResumeAnalysisResultDTO cplResumeAnalysisResultDTO = resumeGradingService.analyzeResume(strResumeText);
            resumeAnalysisPersistenceService.saveAnalysis(tblSavedResumeEntity, cplResumeAnalysisResultDTO);

            tblSavedResumeEntity.setAnalyzeStatus(AsyncTaskStatus.COMPLETED);
            tblSavedResumeEntity.setAnalyzeError(null);
            resumeRepository.save(tblSavedResumeEntity);
            log.info("简历分析任务完成: resumeId={}", tblSavedResumeEntity.getId());
        } catch (Exception e) {
            log.error("简历分析任务失败: {}", e.getMessage(), e);
            tblSavedResumeEntity.setAnalyzeStatus(AsyncTaskStatus.FAILED);
            tblSavedResumeEntity.setAnalyzeError(e.getMessage());
            resumeRepository.save(tblSavedResumeEntity);
        }

        return convertResumeUploadResponseDTO(tblSavedResumeEntity, false);
    }


    /**
     * 统一组装上传接口的返回结果，兼容新上传和重复命中两种场景。
     */
    private ResumeUploadResponseDTO convertResumeUploadResponseDTO(ResumeEntity tblResumeEntity, Boolean boolIsDuplicate) {
        ResumeUploadResponseDTO cplResumeUploadResponseDTO = new ResumeUploadResponseDTO();
        cplResumeUploadResponseDTO.setResumeId(tblResumeEntity.getId());
        cplResumeUploadResponseDTO.setFilename(tblResumeEntity.getOriginalFilename());
        cplResumeUploadResponseDTO.setStorageKey(tblResumeEntity.getStorageKey());
        cplResumeUploadResponseDTO.setAnalyzeStatus(tblResumeEntity.getAnalyzeStatus());
        cplResumeUploadResponseDTO.setDuplicate(boolIsDuplicate);
        return cplResumeUploadResponseDTO;
    }
}
