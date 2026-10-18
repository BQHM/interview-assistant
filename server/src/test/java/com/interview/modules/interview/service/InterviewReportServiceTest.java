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
        InterviewSessionEntity sessionEntity = createSession(
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));

        BusinessException exception = catchThrowableOfType(
                () -> interviewReportService.generateReport("session-001"),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTERVIEW_NOT_COMPLETED.getCode());
    }

    @Test
    void generateReport_shouldAggregateAnswersScoreFeedbackReferenceAndKeyPoints()
            throws JacksonException {
        InterviewSessionEntity sessionEntity = createSession(
                InterviewSessionStatus.COMPLETED,
                2,
                2);
        InterviewAnswerEntity firstAnswerEntity = createAnswer(
                sessionEntity,
                0,
                "我会先按业务分层，再说明事务边界。",
                88,
                "回答清晰。",
                "参考答案一",
                "[\"业务分层\",\"事务边界\"]");
        InterviewAnswerEntity secondAnswerEntity = createAnswer(
                sessionEntity,
                1,
                "我会结合 Redis 缓存一致性说明。",
                76,
                "可以继续补充细节。",
                "参考答案二",
                "[\"缓存一致性\",\"异常处理\"]");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(sessionEntity))
                .thenReturn(List.of(firstAnswerEntity, secondAnswerEntity));

        InterviewReportDTO reportDTO = interviewReportService.generateReport("session-001");

        assertThat(reportDTO.getCompleted()).isTrue();
        assertThat(reportDTO.getTotalQuestions()).isEqualTo(2);
        assertThat(reportDTO.getAnsweredQuestions()).isEqualTo(2);
        assertThat(reportDTO.getUnansweredQuestions()).isZero();
        assertThat(reportDTO.getQuestionReports()).hasSize(2);
        assertThat(reportDTO.getQuestionReports().get(0).getScore()).isEqualTo(88);
        assertThat(reportDTO.getQuestionReports().get(0).getEvaluation()).isEqualTo("回答清晰。");
        assertThat(reportDTO.getQuestionReports().get(0).getReferenceAnswer()).isEqualTo("参考答案一");
        assertThat(reportDTO.getQuestionReports().get(0).getKeyPoints())
                .containsExactly("业务分层", "事务边界");
    }

    @Test
    void generateReport_shouldCountUnansweredQuestions() throws JacksonException {
        InterviewSessionEntity sessionEntity = createSession(
                InterviewSessionStatus.COMPLETED,
                3,
                3);
        InterviewAnswerEntity answerEntity = createAnswer(
                sessionEntity,
                0,
                "我会结合项目说明。",
                72,
                "回答基本完整。",
                "参考答案",
                "[\"项目经验\"]");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(sessionEntity))
                .thenReturn(List.of(answerEntity));

        InterviewReportDTO reportDTO = interviewReportService.generateReport("session-001");

        assertThat(reportDTO.getAnsweredQuestions()).isEqualTo(1);
        assertThat(reportDTO.getUnansweredQuestions()).isEqualTo(2);
        assertThat(reportDTO.getQuestionReports().get(1).getAnswered()).isFalse();
        assertThat(reportDTO.getQuestionReports().get(1).getScore()).isZero();
        assertThat(reportDTO.getQuestionReports().get(1).getEvaluation())
                .contains("未作答");
    }

    private InterviewSessionEntity createSession(
            InterviewSessionStatus status,
            Integer currentQuestionIndex,
            Integer totalQuestions) throws JacksonException {
        ResumeEntity resumeEntity = new ResumeEntity();
        resumeEntity.setId(11L);

        InterviewSessionEntity sessionEntity = new InterviewSessionEntity();
        sessionEntity.setSessionId("session-001");
        sessionEntity.setResume(resumeEntity);
        sessionEntity.setStatus(status);
        sessionEntity.setCurrentQuestionIndex(currentQuestionIndex);
        sessionEntity.setTotalQuestions(totalQuestions);
        sessionEntity.setQuestionsJson(objectMapper.writeValueAsString(createQuestions(totalQuestions)));
        return sessionEntity;
    }

    private List<InterviewQuestionDTO> createQuestions(Integer totalQuestions) {
        return java.util.stream.IntStream.range(0, totalQuestions)
                .mapToObj(this::createQuestion)
                .toList();
    }

    private InterviewQuestionDTO createQuestion(Integer questionIndex) {
        InterviewQuestionDTO questionDTO = new InterviewQuestionDTO();
        questionDTO.setQuestionIndex(questionIndex);
        questionDTO.setQuestion("第 " + questionIndex + " 题");
        questionDTO.setType("PROJECT");
        questionDTO.setCategory("项目经验");
        return questionDTO;
    }

    private InterviewAnswerEntity createAnswer(
            InterviewSessionEntity sessionEntity,
            Integer questionIndex,
            String userAnswer,
            Integer score,
            String feedback,
            String referenceAnswer,
            String keyPointsJson) {
        InterviewAnswerEntity answerEntity = new InterviewAnswerEntity();
        answerEntity.setSession(sessionEntity);
        answerEntity.setQuestionIndex(questionIndex);
        answerEntity.setQuestion("第 " + questionIndex + " 题");
        answerEntity.setCategory("项目经验");
        answerEntity.setUserAnswer(userAnswer);
        answerEntity.setScore(score);
        answerEntity.setFeedback(feedback);
        answerEntity.setReferenceAnswer(referenceAnswer);
        answerEntity.setKeyPointsJson(keyPointsJson);
        return answerEntity;
    }
}
