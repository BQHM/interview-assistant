package com.interview.modules.interview.service.convert;

import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import org.springframework.stereotype.Component;

/**
 * 面试会话转换器，负责 Interview 模块会话相关 DTO 组装。
 */
@Component
public class InterviewSessionConverter {

    /**
     * 将面试会话实体转换为历史列表项 DTO。
     */
    public InterviewSessionListItemDTO convertToInterviewSessionListItemDTO(
            InterviewSessionEntity tblInterviewSessionEntity) {

        InterviewSessionListItemDTO cplInterviewSessionListItemDTO = new InterviewSessionListItemDTO();
        cplInterviewSessionListItemDTO.setSessionId(tblInterviewSessionEntity.getSessionId());
        cplInterviewSessionListItemDTO.setResumeId(tblInterviewSessionEntity.getResume().getId());
        cplInterviewSessionListItemDTO.setTotalQuestions(tblInterviewSessionEntity.getTotalQuestions());
        cplInterviewSessionListItemDTO.setCurrentQuestionIndex(tblInterviewSessionEntity.getCurrentQuestionIndex());
        cplInterviewSessionListItemDTO.setStatus(tblInterviewSessionEntity.getStatus());
        cplInterviewSessionListItemDTO.setCreatedAt(tblInterviewSessionEntity.getCreatedAt());
        return cplInterviewSessionListItemDTO;
    }
}
