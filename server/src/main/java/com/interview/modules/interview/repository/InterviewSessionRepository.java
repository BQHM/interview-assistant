package com.interview.modules.interview.repository;

import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.interview.modules.interview.model.InterviewSessionStatus;

import java.util.List;
import java.util.Optional;

/**
 * 面试会话 Repository，负责和 interview_sessions 表交互。
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, Long> {

    /**
     * 根据会话ID查询面试会话。
     */
    Optional<InterviewSessionEntity> findBySessionId(String strSessionId);

    Optional<InterviewSessionEntity> findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
            Long resumeId,
            List<InterviewSessionStatus> statuses
    );
}
