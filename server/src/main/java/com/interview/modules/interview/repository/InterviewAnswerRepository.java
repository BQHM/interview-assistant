package com.interview.modules.interview.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 面试答案 Repository，负责和 interview_answers 表交互。
 */
@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswerEntity, Long> {

        /**
         * 查询某场面试下某一道题的答案。
         */
        Optional<InterviewAnswerEntity> findBySessionAndQuestionIndex(InterviewSessionEntity tblInterviewSessionEntity,
                        Integer intQuestionIndex);

        /**
         * 查询某场面试下的全部答案。
         */
        List<InterviewAnswerEntity> findBySessionOrderByQuestionIndexAsc(
                        InterviewSessionEntity tblInterviewSessionEntity);

        /**
         * 删除某场面试下的全部答案。
         */
        void deleteBySession(
                        InterviewSessionEntity tblInterviewSessionEntity);
}
