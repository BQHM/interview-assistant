package com.interview.modules.resume.repository;

import com.interview.modules.resume.model.ResumeAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 简历分析结果 Repository，负责和 resume_analyses 表交互。
 */
@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysisEntity, Long> {

    /**
     * 根据简历 id 查询对应分析结果。
     */
    Optional<ResumeAnalysisEntity> findByResumeId(Long resumeId);

    List<ResumeAnalysisEntity> findByResumeIdIn(List<Long> lstResumeId);
}
