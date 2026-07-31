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
     * @param tblInterviewSessionEntity 面试会话实体
     * @return 面试历史列表项
     * @author NobuNo
     * @date 2026-05-16
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
        cplInterviewSessionListItemDTO.setSkillId(tblInterviewSessionEntity.getSkillId());
        return cplInterviewSessionListItemDTO;
    }

    /**
     * 功能说明
     * <p>转换面试历史详情。</p>
     *
     * @param tblInterviewSessionEntity 面试会话实体
     * @param lstInterviewQuestionDTO 面试题列表
     * @return 面试历史详情
     * @author NobuNo
     * @date 2026-05-16
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
        cplInterviewDetailDTO.setSkillId(tblInterviewSessionEntity.getSkillId());
        return cplInterviewDetailDTO;
    }
}
