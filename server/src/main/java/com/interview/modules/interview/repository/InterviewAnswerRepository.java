package com.interview.modules.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 文件功能说明
 * <p>负责面试答案数据访问。</p>
 *
 * @author NobuNo
 * @since 2026-05-18
 */
@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerEntity, Long> {

    /**
     * 功能说明
     * <p>查询某道题的答案。</p>
     *
     * @param tblInterviewSessionEntity 面试会话实体
     * @param intQuestionIndex 题目索引
     * @return 面试答案实体
     * @author NobuNo
     * @since 2026-05-18
     */
    Optional<InterviewAnswerEntity> findBySessionAndQuestionIndex(InterviewSessionEntity tblInterviewSessionEntity,
                    Integer intQuestionIndex);

    /**
     * 功能说明
     * <p>查询会话下的全部答案。</p>
     *
     * @param tblInterviewSessionEntity 面试会话实体
     * @return 面试答案列表
     * @author NobuNo
     * @since 2026-05-18
     */
    List<InterviewAnswerEntity> findBySessionOrderByQuestionIndexAsc(
                    InterviewSessionEntity tblInterviewSessionEntity);

    /**
     * 功能说明
     * <p>删除会话下的全部答案。</p>
     *
     * @param tblInterviewSessionEntity 面试会话实体
     * @author NobuNo
     * @since 2026-05-18
     */
    void deleteBySession(
                    InterviewSessionEntity tblInterviewSessionEntity);
}
