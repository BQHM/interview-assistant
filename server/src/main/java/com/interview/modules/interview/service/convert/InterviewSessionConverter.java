package com.interview.modules.interview.service.convert;

import java.util.List;

import com.interview.modules.interview.model.dto.InterviewDetailDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;

/**
 * 文件功能说明
 * <p>负责面试会话 DTO 转换。</p>
 *
 * @author NobuNo
 * @date 2026-05-16
 */
public class InterviewSessionConverter {

    private InterviewSessionConverter() {
    }

    /**
     * 功能说明
     * <p>转换面试历史列表项。</p>
     *
     * @param interviewSessionEntity 面试会话实体
     * @return 面试历史列表项
     * @author NobuNo
     * @date 2026-05-16
     */
    public static InterviewSessionListItemDTO convertToInterviewSessionListItemDTO(
            InterviewSessionEntity interviewSessionEntity) {

        InterviewSessionListItemDTO interviewSessionListItemDTO = new InterviewSessionListItemDTO();
        interviewSessionListItemDTO.setSessionId(interviewSessionEntity.getSessionId());
        interviewSessionListItemDTO.setResumeId(interviewSessionEntity.getResume().getId());
        interviewSessionListItemDTO.setTotalQuestions(interviewSessionEntity.getTotalQuestions());
        interviewSessionListItemDTO.setCurrentQuestionIndex(interviewSessionEntity.getCurrentQuestionIndex());
        interviewSessionListItemDTO.setStatus(interviewSessionEntity.getStatus());
        interviewSessionListItemDTO.setCreatedAt(interviewSessionEntity.getCreatedAt());
        interviewSessionListItemDTO.setSkillId(interviewSessionEntity.getSkillId());
        return interviewSessionListItemDTO;
    }

    /**
     * 功能说明
     * <p>转换面试历史详情。</p>
     *
     * @param interviewSessionEntity 面试会话实体
     * @param interviewQuestionDTOs 面试题列表
     * @return 面试历史详情
     * @author NobuNo
     * @date 2026-05-16
     */
    public static InterviewDetailDTO convertToInterviewDetailDTO(
            InterviewSessionEntity interviewSessionEntity,
            List<InterviewQuestionDTO> interviewQuestionDTOs) {

        InterviewDetailDTO interviewDetailDTO = new InterviewDetailDTO();
        interviewDetailDTO.setSessionId(interviewSessionEntity.getSessionId());
        interviewDetailDTO.setResumeId(interviewSessionEntity.getResume().getId());
        interviewDetailDTO.setTotalQuestions(interviewSessionEntity.getTotalQuestions());
        interviewDetailDTO.setCurrentQuestionIndex(interviewSessionEntity.getCurrentQuestionIndex());
        interviewDetailDTO.setStatus(interviewSessionEntity.getStatus());
        interviewDetailDTO.setCreatedAt(interviewSessionEntity.getCreatedAt());
        interviewDetailDTO.setQuestions(interviewQuestionDTOs);
        interviewDetailDTO.setSkillId(interviewSessionEntity.getSkillId());
        return interviewDetailDTO;
    }
}
