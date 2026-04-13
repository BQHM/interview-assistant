package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.infrastructure.file.DocumentParseService;
import com.interview.infrastructure.file.FileHashService;
import com.interview.infrastructure.file.FileStorageService;
import com.interview.infrastructure.file.FileValidationService;
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
        String contentType = fileValidationService.validateResume(file);

        String originalFilename = file.getOriginalFilename();

        // 基于文件内容做去重，避免同一份简历被重复存储。
        String fileHash = fileHashService.calculate(file);

        Optional<ResumeEntity> existResume = resumeRepository.findByFileHash(fileHash);
        if (existResume.isPresent()) {
            ResumeEntity oldResume = existResume.get();
            return convertResumeUploadResponseDTO(oldResume, true);
        }

        log.debug("识别到简历文件类型: filename={}, contentType={}", originalFilename, contentType);
        log.info("收到简历上传请求: {}", originalFilename);

        // 2. 从文件中提取简历正文文本，并做基础清洗
        String resumeText = documentParseService.parseResume(file);
        // 解析结果为空时直接失败，避免把无效简历写入存储和数据库。
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PARSE_FAILED);
        }

        // 3. 上传原始文件到对象存储，返回文件存储路径 storageKey
        String storageKey = fileStorageService.uploadResume(file, contentType);
        log.info("文件已存储，Key: {}", storageKey);

        // 4. 组装数据库实体，保存文件元信息和解析后的文本
        ResumeEntity resume = new ResumeEntity();
        resume.setOriginalFilename(originalFilename);
        resume.setStorageKey(storageKey);
        resume.setFileSize(file.getSize());
        resume.setContentType(contentType);
        resume.setResumeText(resumeText);

        // 使用基于文件内容计算出的真实哈希值，后续可用于去重
        resume.setFileHash(fileHash);

        // 5. 持久化简历记录，uploadedAt 等字段由实体生命周期方法自动补全
        ResumeEntity savedResume = resumeRepository.save(resume);

        ResumeUploadResponseDTO resumeUploadResponseDTO = convertResumeUploadResponseDTO(savedResume, false);

        return resumeUploadResponseDTO;
    }


    private ResumeUploadResponseDTO convertResumeUploadResponseDTO(ResumeEntity resume, Boolean isDuplicate) {
        ResumeUploadResponseDTO resumeUploadResponseDTO = new ResumeUploadResponseDTO();
        resumeUploadResponseDTO.setResumeId(resume.getId());
        resumeUploadResponseDTO.setFilename(resume.getOriginalFilename());
        resumeUploadResponseDTO.setStorageKey(resume.getStorageKey());
        resumeUploadResponseDTO.setAnalyzeStatus(resume.getAnalyzeStatus());
        resumeUploadResponseDTO.setDuplicate(isDuplicate);
        return resumeUploadResponseDTO;
    }
}
