package com.interview.modules.interview.service.comm;

import java.util.List;

import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;

/**
 * 面试题目答案聚合工具。
 * 负责把 questionsJson 解析出的题目快照和 interview_answers 查询出的答案记录按 questionIndex 合并。
 */
public class InterviewQuestionAnswerAggregator {

    private InterviewQuestionAnswerAggregator() {
    }

    /**
     * 将答案表中的 userAnswer 回填到题目 DTO 列表中。
     */
    public static void fillUserAnswers(
            List<InterviewQuestionDTO> lstInterviewQuestionDTO,
            List<InterviewAnswerEntity> lstInterviewAnswerEntity) {

        for (InterviewQuestionDTO cplInterviewQuestionDTO : lstInterviewQuestionDTO) {
            for (InterviewAnswerEntity tblInterviewAnswerEntity : lstInterviewAnswerEntity) {
                if (cplInterviewQuestionDTO.getQuestionIndex().equals(tblInterviewAnswerEntity.getQuestionIndex())) {
                    cplInterviewQuestionDTO.setUserAnswer(tblInterviewAnswerEntity.getUserAnswer());
                    break;
                }
            }
        }
    }
}
