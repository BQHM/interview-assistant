package com.interview.modules.interview.service.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.dto.InterviewDetailDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;

/**
 * 面试会话转换器测试。
 */
class InterviewSessionConverterTest {

    @Test
    void convertToInterviewSessionListItemDTO_shouldMapSkillId() {
        InterviewSessionEntity sessionEntity = createSessionEntity();

        InterviewSessionListItemDTO listItemDTO = InterviewSessionConverter
                .convertToInterviewSessionListItemDTO(sessionEntity);

        assertThat(listItemDTO.getSessionId()).isEqualTo("session-001");
        assertThat(listItemDTO.getResumeId()).isEqualTo(11L);
        assertThat(listItemDTO.getSkillId()).isEqualTo("java-backend");
    }

    @Test
    void convertToInterviewDetailDTO_shouldMapSkillId() {
        InterviewSessionEntity sessionEntity = createSessionEntity();
        List<InterviewQuestionDTO> questionList = List.of(new InterviewQuestionDTO());

        InterviewDetailDTO detailDTO = InterviewSessionConverter
                .convertToInterviewDetailDTO(sessionEntity, questionList);

        assertThat(detailDTO.getSkillId()).isEqualTo("java-backend");
        assertThat(detailDTO.getQuestions()).isSameAs(questionList);
    }

    private InterviewSessionEntity createSessionEntity() {
        ResumeEntity resumeEntity = new ResumeEntity();
        resumeEntity.setId(11L);

        InterviewSessionEntity sessionEntity = new InterviewSessionEntity();
        sessionEntity.setSessionId("session-001");
        sessionEntity.setResume(resumeEntity);
        sessionEntity.setSkillId("java-backend");
        sessionEntity.setTotalQuestions(3);
        sessionEntity.setCurrentQuestionIndex(1);
        sessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS);
        sessionEntity.setCreatedAt(LocalDateTime.of(2026, 7, 30, 10, 0));
        return sessionEntity;
    }
}
