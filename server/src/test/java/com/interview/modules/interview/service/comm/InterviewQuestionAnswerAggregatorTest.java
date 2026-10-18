package com.interview.modules.interview.service.comm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;

/**
 * 面试题目答案聚合工具测试。
 */
class InterviewQuestionAnswerAggregatorTest {

    /**
     * 当题目索引和答案索引一致时，应把答案回填到对应题目中。
     */
    @Test
    void fillUserAnswers_shouldFillMatchedAnswersByQuestionIndex() {
        List<InterviewQuestionDTO> interviewQuestionDTOs = new ArrayList<>();

        InterviewQuestionDTO firstQuestionDTO = new InterviewQuestionDTO();
        firstQuestionDTO.setQuestionIndex(0);
        firstQuestionDTO.setQuestion("请介绍一下自己");
        interviewQuestionDTOs.add(firstQuestionDTO);

        InterviewQuestionDTO secondQuestionDTO = new InterviewQuestionDTO();
        secondQuestionDTO.setQuestionIndex(1);
        secondQuestionDTO.setQuestion("请介绍 Spring Boot");
        interviewQuestionDTOs.add(secondQuestionDTO);

        List<InterviewAnswerEntity> interviewAnswerEntities = new ArrayList<>();

        InterviewAnswerEntity firstAnswerEntity = new InterviewAnswerEntity();
        firstAnswerEntity.setQuestionIndex(0);
        firstAnswerEntity.setUserAnswer("我是 Java 后端开发。");
        interviewAnswerEntities.add(firstAnswerEntity);

        InterviewQuestionAnswerAggregator.fillUserAnswers(
                interviewQuestionDTOs,
                interviewAnswerEntities);

        assertThat(interviewQuestionDTOs.get(0).getUserAnswer())
                .isEqualTo("我是 Java 后端开发。");
        assertThat(interviewQuestionDTOs.get(1).getUserAnswer())
                .isNull();
    }
}