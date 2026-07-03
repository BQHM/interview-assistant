package com.interview.modules.resume.repository;

import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件功能说明
 * <p>负责简历分析结果数据访问。</p>
 *
 * @author NobuNo
 * @date 2026-04-14
 */
@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysisEntity, Long> {

    /**
     * 功能说明
     * <p>根据简历编号查询分析结果。</p>
     *
     * @param lngResumeId 简历编号
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-14
     */
    Optional<ResumeAnalysisEntity> findByResumeId(Long lngResumeId);

    /**
     * 功能说明
     * <p>根据简历编号列表查询分析结果。</p>
     *
     * @param lstResumeId 简历编号列表
     * @return 简历分析结果列表
     * @author NobuNo
     * @date 2026-04-14
     */
    List<ResumeAnalysisEntity> findByResumeIdIn(List<Long> lstResumeId);

    /**
     * 功能说明
     * <p>按分析时间倒序查询简历分析历史。</p>
     *
     * @param lngResumeId 简历编号
     * @return 简历分析历史列表
     * @author NobuNo
     * @date 2026-04-14
     */
    List<ResumeAnalysisEntity> findByResumeIdOrderByAnalyzedAtDesc(Long lngResumeId);
}
