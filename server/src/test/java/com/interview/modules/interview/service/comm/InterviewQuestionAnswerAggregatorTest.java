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
        List<InterviewQuestionDTO> lstInterviewQuestionDTO = new ArrayList<>();

        InterviewQuestionDTO cplFirstQuestionDTO = new InterviewQuestionDTO();
        cplFirstQuestionDTO.setQuestionIndex(0);
        cplFirstQuestionDTO.setQuestion("请介绍一下自己");
        lstInterviewQuestionDTO.add(cplFirstQuestionDTO);

        InterviewQuestionDTO cplSecondQuestionDTO = new InterviewQuestionDTO();
        cplSecondQuestionDTO.setQuestionIndex(1);
        cplSecondQuestionDTO.setQuestion("请介绍 Spring Boot");
        lstInterviewQuestionDTO.add(cplSecondQuestionDTO);

        List<InterviewAnswerEntity> lstInterviewAnswerEntity = new ArrayList<>();

        InterviewAnswerEntity tblFirstAnswerEntity = new InterviewAnswerEntity();
        tblFirstAnswerEntity.setQuestionIndex(0);
        tblFirstAnswerEntity.setUserAnswer("我是 Java 后端开发。");
        lstInterviewAnswerEntity.add(tblFirstAnswerEntity);

        InterviewQuestionAnswerAggregator.fillUserAnswers(
                lstInterviewQuestionDTO,
                lstInterviewAnswerEntity);

        assertThat(lstInterviewQuestionDTO.get(0).getUserAnswer())
                .isEqualTo("我是 Java 后端开发。");
        assertThat(lstInterviewQuestionDTO.get(1).getUserAnswer())
                .isNull();
    }
}