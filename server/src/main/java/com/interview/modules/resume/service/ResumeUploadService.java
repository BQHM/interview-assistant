package com.interview.modules.resume.service;

import com.interview.infrastructure.file.FileStorageService;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadService {

    private final FileStorageService storageService;
    private final ResumeRepository resumeRepository;

    /**
     * 上传简历并保存到数据库
     *
     * @param file 用户上传的 MultipartFile
     * @return 保存成功的简历 ID
     */
    @Transactional // 架构师提醒：数据库操作必须开启事务，保证原子性
    public Long uploadAndSave(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("收到简历上传请求: {}", originalFilename);

        // 1. 调用已有的基础设施：保存文件到 RustFS/S3
        // 这个方法会返回文件的 storageKey (路径)
        String storageKey = storageService.uploadResume(file);
        log.info("文件已存储，Key: {}", storageKey);

        // 2. 构造数据库实体 (ResumeEntity)
        ResumeEntity resume = new ResumeEntity();
        resume.setOriginalFilename(originalFilename);
        resume.setStorageKey(storageKey);
        resume.setFileSize(file.getSize());
        resume.setContentType(file.getContentType());

        // 架构师思考：暂时随便造个 Hash 占位，下一阶段我会教你如何计算真实内容 Hash 以便去重
        resume.setFileHash("hash-" + System.currentTimeMillis());

        // 3. 保存到数据库
        // 注意：ResumeEntity 中我们写了 @PrePersist，所以 uploadedAt 会自动生成
        ResumeEntity savedResume = resumeRepository.save(resume);
        log.info("简历数据已入库，ID: {}", savedResume.getId());

        return savedResume.getId();
    }


}
