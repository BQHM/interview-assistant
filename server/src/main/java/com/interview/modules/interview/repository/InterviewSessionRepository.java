package com.interview.modules.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 文件功能说明
 * <p>负责面试会话数据访问。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, Long> {

    /**
     * 功能说明
     * <p>根据会话编号查询面试会话。</p>
     *
     * @param strSessionId 面试会话编号
     * @return 面试会话实体
     * @author NobuNo
     * @date 2026-04-20
     */
    Optional<InterviewSessionEntity> findBySessionId(String strSessionId);

    /**
     * 功能说明
     * <p>查询简历最近一条指定状态的面试会话。</p>
     *
     * @param lngResumeId 简历编号
     * @param lstStatus 会话状态列表
     * @return 面试会话实体
     * @author NobuNo
     * @date 2026-04-20
     */
    Optional<InterviewSessionEntity> findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
            Long lngResumeId,
            List<InterviewSessionStatus> lstStatus);

    /**
     * 功能说明
     * <p>查询简历指定面试方向最近一条未完成会话。</p>
     *
     * @param lngResumeId 简历编号
     * @param strSkillId 面试方向编号
     * @param lstStatus 会话状态列表
     * @return 面试会话实体
     * @author NobuNo
     * @date 2026-04-20
     */
    Optional<InterviewSessionEntity> findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
            Long lngResumeId,
            String strSkillId,
            List<InterviewSessionStatus> lstStatus);

    /**
     * 功能说明
     * <p>按创建时间倒序查询面试会话。</p>
     *
     * @return 面试会话列表
     * @author NobuNo
     * @date 2026-04-20
     */
    List<InterviewSessionEntity> findAllByOrderByCreatedAtDesc();
}
