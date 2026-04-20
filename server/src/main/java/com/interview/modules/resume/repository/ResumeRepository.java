package com.interview.modules.resume.repository;

import com.interview.modules.resume.model.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 简历Repository - 负责和数据库打交道
 */
@Repository
public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {

    /**
     * 根据文件哈希查找简历（用于去重检查）
     */
    Optional<ResumeEntity> findByFileHash(String fileHash);

    List<ResumeEntity> findAllByOrderByUploadedAtDesc();

}
