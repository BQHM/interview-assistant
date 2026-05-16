package com.interview.modules.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 面试会话 Repository，负责和 interview_sessions 表交互。
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, Long> {

    /**
     * 根据会话ID查询面试会话。
     */
    Optional<InterviewSessionEntity> findBySessionId(String strSessionId);

    /**
     * 根据简历ID查询最近一条未完成的面试会话。
     * 未完成状态包括 CREATED 和 IN_PROGRESS，按创建时间倒序取最新一条。
     */
    Optional<InterviewSessionEntity> findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
            Long lngResumeId,
            List<InterviewSessionStatus> lstStatus);

    /**
     * 按创建时间倒序查询所有面试会话。
     */
    List<InterviewSessionEntity> findAllByOrderByCreatedAtDesc();
}
