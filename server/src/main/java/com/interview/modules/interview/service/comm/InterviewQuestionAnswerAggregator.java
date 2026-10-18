package com.interview.modules.interview.service.comm;

import java.util.List;

import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;

/**
 * 文件功能说明
 * <p>负责面试题和答案聚合。</p>
 *
 * @author NobuNo
 * @date 2026-06-29
 */
public class InterviewQuestionAnswerAggregator {

    private InterviewQuestionAnswerAggregator() {
    }

    /**
     * 功能说明
     * <p>填充题目中的用户答案。</p>
     *
     * @param interviewQuestionDTOs 面试题列表
     * @param interviewAnswerEntities 面试答案列表
     * @author NobuNo
     * @date 2026-06-29
     */
    public static void fillUserAnswers(
            List<InterviewQuestionDTO> interviewQuestionDTOs,
            List<InterviewAnswerEntity> interviewAnswerEntities) {

        for (InterviewQuestionDTO interviewQuestionDTO : interviewQuestionDTOs) {
            for (InterviewAnswerEntity interviewAnswerEntity : interviewAnswerEntities) {
                if (interviewQuestionDTO.getQuestionIndex().equals(interviewAnswerEntity.getQuestionIndex())) {
                    interviewQuestionDTO.setUserAnswer(interviewAnswerEntity.getUserAnswer());
                    break;
                }
            }
        }
    }
}
