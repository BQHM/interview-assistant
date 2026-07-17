package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewReportDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.repository.InterviewAnswerRepository;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.resume.model.entity.ResumeEntity;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 面试报告服务测试。
 */
@ExtendWith(MockitoExtension.class)
class InterviewReportServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewAnswerRepository interviewAnswerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InterviewReportService interviewReportService;

    @BeforeEach
    void setUp() {
        interviewReportService = new InterviewReportService(
                interviewSessionRepository,
                interviewAnswerRepository,
                objectMapper);
    }

    @Test
    void generateReport_shouldRejectUncompletedSession() throws JacksonException {
        InterviewSessionEntity tblSessionEntity = createSession(
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));

        BusinessException exception = catchThrowableOfType(
                () -> interviewReportService.generateReport("session-001"),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTERVIEW_NOT_COMPLETED.getCode());
    }

    @Test
    void generateReport_shouldAggregateAnswersScoreFeedbackReferenceAndKeyPoints()
            throws JacksonException {
        InterviewSessionEntity tblSessionEntity = createSession(
                InterviewSessionStatus.COMPLETED,
                2,
                2);
        InterviewAnswerEntity tblFirstAnswerEntity = createAnswer(
                tblSessionEntity,
                0,
                "我会先按业务分层，再说明事务边界。",
                88,
                "回答清晰。",
                "参考答案一",
                "[\"业务分层\",\"事务边界\"]");
        InterviewAnswerEntity tblSecondAnswerEntity = createAnswer(
                tblSessionEntity,
                1,
                "我会结合 Redis 缓存一致性说明。",
                76,
                "可以继续补充细节。",
                "参考答案二",
                "[\"缓存一致性\",\"异常处理\"]");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(tblSessionEntity))
                .thenReturn(List.of(tblFirstAnswerEntity, tblSecondAnswerEntity));

        InterviewReportDTO cplReportDTO = interviewReportService.generateReport("session-001");

        assertThat(cplReportDTO.getCompleted()).isTrue();
        assertThat(cplReportDTO.getTotalQuestions()).isEqualTo(2);
        assertThat(cplReportDTO.getAnsweredQuestions()).isEqualTo(2);
        assertThat(cplReportDTO.getUnansweredQuestions()).isZero();
        assertThat(cplReportDTO.getQuestionReports()).hasSize(2);
        assertThat(cplReportDTO.getQuestionReports().get(0).getScore()).isEqualTo(88);
        assertThat(cplReportDTO.getQuestionReports().get(0).getEvaluation()).isEqualTo("回答清晰。");
        assertThat(cplReportDTO.getQuestionReports().get(0).getReferenceAnswer()).isEqualTo("参考答案一");
        assertThat(cplReportDTO.getQuestionReports().get(0).getKeyPoints())
                .containsExactly("业务分层", "事务边界");
    }

    @Test
    void generateReport_shouldCountUnansweredQuestions() throws JacksonException {
        InterviewSessionEntity tblSessionEntity = createSession(
                InterviewSessionStatus.COMPLETED,
                3,
                3);
        InterviewAnswerEntity tblAnswerEntity = createAnswer(
                tblSessionEntity,
                0,
                "我会结合项目说明。",
                72,
                "回答基本完整。",
                "参考答案",
                "[\"项目经验\"]");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(tblSessionEntity))
                .thenReturn(List.of(tblAnswerEntity));

        InterviewReportDTO cplReportDTO = interviewReportService.generateReport("session-001");

        assertThat(cplReportDTO.getAnsweredQuestions()).isEqualTo(1);
        assertThat(cplReportDTO.getUnansweredQuestions()).isEqualTo(2);
        assertThat(cplReportDTO.getQuestionReports().get(1).getAnswered()).isFalse();
        assertThat(cplReportDTO.getQuestionReports().get(1).getScore()).isZero();
        assertThat(cplReportDTO.getQuestionReports().get(1).getEvaluation())
                .contains("未作答");
    }

    private InterviewSessionEntity createSession(
            InterviewSessionStatus status,
            Integer intCurrentQuestionIndex,
            Integer intTotalQuestions) throws JacksonException {
        ResumeEntity tblResumeEntity = new ResumeEntity();
        tblResumeEntity.setId(11L);

        InterviewSessionEntity tblSessionEntity = new InterviewSessionEntity();
        tblSessionEntity.setSessionId("session-001");
        tblSessionEntity.setResume(tblResumeEntity);
        tblSessionEntity.setStatus(status);
        tblSessionEntity.setCurrentQuestionIndex(intCurrentQuestionIndex);
        tblSessionEntity.setTotalQuestions(intTotalQuestions);
        tblSessionEntity.setQuestionsJson(objectMapper.writeValueAsString(createQuestions(intTotalQuestions)));
        return tblSessionEntity;
    }

    private List<InterviewQuestionDTO> createQuestions(Integer intTotalQuestions) {
        return java.util.stream.IntStream.range(0, intTotalQuestions)
                .mapToObj(this::createQuestion)
                .toList();
    }

    private InterviewQuestionDTO createQuestion(Integer intQuestionIndex) {
        InterviewQuestionDTO cplQuestionDTO = new InterviewQuestionDTO();
        cplQuestionDTO.setQuestionIndex(intQuestionIndex);
        cplQuestionDTO.setQuestion("第 " + intQuestionIndex + " 题");
        cplQuestionDTO.setType("PROJECT");
        cplQuestionDTO.setCategory("项目经验");
        return cplQuestionDTO;
    }

    private InterviewAnswerEntity createAnswer(
            InterviewSessionEntity tblSessionEntity,
            Integer intQuestionIndex,
            String strUserAnswer,
            Integer intScore,
            String strFeedback,
            String strReferenceAnswer,
            String strKeyPointsJson) {
        InterviewAnswerEntity tblAnswerEntity = new InterviewAnswerEntity();
        tblAnswerEntity.setSession(tblSessionEntity);
        tblAnswerEntity.setQuestionIndex(intQuestionIndex);
        tblAnswerEntity.setQuestion("第 " + intQuestionIndex + " 题");
        tblAnswerEntity.setCategory("项目经验");
        tblAnswerEntity.setUserAnswer(strUserAnswer);
        tblAnswerEntity.setScore(intScore);
        tblAnswerEntity.setFeedback(strFeedback);
        tblAnswerEntity.setReferenceAnswer(strReferenceAnswer);
        tblAnswerEntity.setKeyPointsJson(strKeyPointsJson);
        return tblAnswerEntity;
    }
}
