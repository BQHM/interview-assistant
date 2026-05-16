package com.interview.modules.interview.service.convert;

import java.util.List;

import com.interview.modules.interview.model.dto.InterviewDetailDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 面试会话转换器，负责 Interview 模块会话相关 DTO 组装。
 */
public class InterviewSessionConverter {

    private InterviewSessionConverter() {
    }

    /**
     * 将面试会话实体转换为历史列表项 DTO。
     */
    public static InterviewSessionListItemDTO convertToInterviewSessionListItemDTO(
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

    /**
     * 将面试会话实体和题目快照转换为历史详情 DTO。
     */
    public static InterviewDetailDTO convertToInterviewDetailDTO(
            InterviewSessionEntity tblInterviewSessionEntity,
            List<InterviewQuestionDTO> lstInterviewQuestionDTO) {

        InterviewDetailDTO cplInterviewDetailDTO = new InterviewDetailDTO();
        cplInterviewDetailDTO.setSessionId(tblInterviewSessionEntity.getSessionId());
        cplInterviewDetailDTO.setResumeId(tblInterviewSessionEntity.getResume().getId());
        cplInterviewDetailDTO.setTotalQuestions(tblInterviewSessionEntity.getTotalQuestions());
        cplInterviewDetailDTO.setCurrentQuestionIndex(tblInterviewSessionEntity.getCurrentQuestionIndex());
        cplInterviewDetailDTO.setStatus(tblInterviewSessionEntity.getStatus());
        cplInterviewDetailDTO.setCreatedAt(tblInterviewSessionEntity.getCreatedAt());
        cplInterviewDetailDTO.setQuestions(lstInterviewQuestionDTO);
        return cplInterviewDetailDTO;
    }
}
