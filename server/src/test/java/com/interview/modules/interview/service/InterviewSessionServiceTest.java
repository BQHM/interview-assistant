package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.interview.modules.interview.model.dto.InterviewAnswerEvaluationDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
import com.interview.modules.interview.model.dto.SubmitAnswerResponse;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.interview.model.request.SubmitAnswerRequest;
import com.interview.modules.interview.repository.InterviewAnswerRepository;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 面试会话服务测试。
 */
@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewAnswerRepository interviewAnswerRepository;

    @Mock
    private InterviewAnswerEvaluationService interviewAnswerEvaluationService;

    @Mock
    private InterviewQuestionService interviewQuestionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private InterviewSessionService interviewSessionService;

    @BeforeEach
    void setUp() {
        interviewSessionService = new InterviewSessionService(
                resumeRepository,
                interviewSessionRepository,
                objectMapper,
                interviewAnswerRepository,
                interviewAnswerEvaluationService,
                interviewQuestionService);
    }

    @Test
    void createInterview_shouldCreateSessionSuccessfully() {
        ResumeEntity resumeEntity = createResume(11L, "熟悉 Spring Boot、MySQL、Redis。");
        List<InterviewQuestionDTO> questionDTOs = createQuestions(3);
        CreateInterviewRequest request = createInterviewRequest(11L, 3);

        when(resumeRepository.findById(11L)).thenReturn(Optional.of(resumeEntity));
        when(interviewSessionRepository.findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
                eq(11L),
                eq("java-backend"),
                anyList())).thenReturn(Optional.empty());
        when(interviewQuestionService.generateQuestions(eq(resumeEntity.getResumeText()), eq(3), eq("java-backend"), anyList()))
                .thenReturn(questionDTOs);
        when(interviewSessionRepository.save(any(InterviewSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InterviewSessionDTO sessionDTO = interviewSessionService.createInterview(request);

        assertThat(sessionDTO.getSessionId()).isNotBlank();
        assertThat(sessionDTO.getResumeId()).isEqualTo(11L);
        assertThat(sessionDTO.getSkillId()).isEqualTo("java-backend");
        assertThat(sessionDTO.getTotalQuestions()).isEqualTo(3);
        assertThat(sessionDTO.getCurrentQuestionIndex()).isZero();
        assertThat(sessionDTO.getStatus()).isEqualTo(InterviewSessionStatus.CREATED);
        assertThat(sessionDTO.getQuestions())
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2);
        verify(interviewSessionRepository).save(argThat(
                sessionEntity -> "java-backend".equals(sessionEntity.getSkillId())));
    }

    @Test
    void createInterview_shouldReuseUnfinishedSessionForSameResume() throws JacksonException {
        ResumeEntity resumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity existingSessionEntity = createSession(
                resumeEntity,
                "session-001",
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);
        InterviewAnswerEntity answerEntity = createAnswer(
                existingSessionEntity,
                0,
                "我会从项目模块和接口设计说明。");

        when(resumeRepository.findById(11L)).thenReturn(Optional.of(resumeEntity));
        when(interviewSessionRepository.findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
                eq(11L),
                eq("java-backend"),
                anyList())).thenReturn(Optional.of(existingSessionEntity));
        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(existingSessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(existingSessionEntity))
                .thenReturn(List.of(answerEntity));

        InterviewSessionDTO sessionDTO = interviewSessionService.createInterview(
                createInterviewRequest(11L, 3));

        assertThat(sessionDTO.getSessionId()).isEqualTo("session-001");
        assertThat(sessionDTO.getSkillId()).isEqualTo("java-backend");
        assertThat(sessionDTO.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
        assertThat(sessionDTO.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(sessionDTO.getQuestions().get(0).getUserAnswer())
                .isEqualTo("我会从项目模块和接口设计说明。");
        verify(interviewQuestionService, never()).generateQuestions(any(), any(), any(), anyList());
        verify(interviewSessionRepository, never()).save(any(InterviewSessionEntity.class));
    }

    @Test
    void submitAnswer_shouldAdvanceCurrentQuestionIndex() throws JacksonException {
        ResumeEntity resumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity sessionEntity = createSession(
                resumeEntity,
                "session-001",
                InterviewSessionStatus.CREATED,
                0,
                3);
        SubmitAnswerRequest request = createSubmitAnswerRequest(
                "session-001",
                0,
                "我会先按 Controller、Service、Repository 分层说明项目结构。");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));
        when(interviewAnswerRepository.findBySessionAndQuestionIndex(sessionEntity, 0))
                .thenReturn(Optional.empty());
        when(interviewAnswerRepository.save(any(InterviewAnswerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(interviewAnswerEvaluationService.evaluateAnswer(any(InterviewAnswerEntity.class)))
                .thenReturn(createEvaluation());
        when(interviewSessionRepository.save(sessionEntity)).thenReturn(sessionEntity);

        SubmitAnswerResponse response = interviewSessionService.submitAnswer(request);

        assertThat(response.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(response.getHasNextQuestion()).isTrue();
        assertThat(response.getNextQuestion().getQuestionIndex()).isEqualTo(1);
        assertThat(sessionEntity.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(sessionEntity.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
    }

    @Test
    void submitAnswer_shouldRejectCompletedSession() throws JacksonException {
        ResumeEntity resumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity sessionEntity = createSession(
                resumeEntity,
                "session-001",
                InterviewSessionStatus.COMPLETED,
                3,
                3);
        SubmitAnswerRequest request = createSubmitAnswerRequest(
                "session-001",
                0,
                "面试完成后不应该还能提交。");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));

        BusinessException exception = catchThrowableOfType(
                () -> interviewSessionService.submitAnswer(request),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTERVIEW_ALREADY_COMPLETED.getCode());
        verify(interviewAnswerRepository, never()).save(any(InterviewAnswerEntity.class));
    }

    @Test
    void completeInterview_shouldMarkSessionCompletedBeforeAllAnswers() throws JacksonException {
        ResumeEntity resumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity sessionEntity = createSession(
                resumeEntity,
                "session-001",
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(sessionEntity));
        when(interviewSessionRepository.save(sessionEntity)).thenReturn(sessionEntity);

        interviewSessionService.completeInterview("session-001");

        assertThat(sessionEntity.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(sessionEntity.getCurrentQuestionIndex()).isEqualTo(3);
        verify(interviewSessionRepository).save(sessionEntity);
    }

    private CreateInterviewRequest createInterviewRequest(Long resumeId, Integer questionCount) {
        CreateInterviewRequest request = new CreateInterviewRequest();
        request.setResumeId(resumeId);
        request.setQuestionCount(questionCount);
        return request;
    }

    private SubmitAnswerRequest createSubmitAnswerRequest(
            String sessionId,
            Integer questionIndex,
            String answer) {
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setSessionId(sessionId);
        request.setQuestionIndex(questionIndex);
        request.setAnswer(answer);
        return request;
    }

    private ResumeEntity createResume(Long resumeId, String resumeText) {
        ResumeEntity resumeEntity = new ResumeEntity();
        resumeEntity.setId(resumeId);
        resumeEntity.setResumeText(resumeText);
        return resumeEntity;
    }

    private InterviewSessionEntity createSession(
            ResumeEntity resumeEntity,
            String sessionId,
            InterviewSessionStatus status,
            Integer currentQuestionIndex,
            Integer totalQuestions) throws JacksonException {
        InterviewSessionEntity sessionEntity = new InterviewSessionEntity();
        sessionEntity.setSessionId(sessionId);
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
            String userAnswer) {
        InterviewAnswerEntity answerEntity = new InterviewAnswerEntity();
        answerEntity.setSession(sessionEntity);
        answerEntity.setQuestionIndex(questionIndex);
        answerEntity.setQuestion("第 " + questionIndex + " 题");
        answerEntity.setCategory("项目经验");
        answerEntity.setUserAnswer(userAnswer);
        return answerEntity;
    }

    private InterviewAnswerEvaluationDTO createEvaluation() {
        InterviewAnswerEvaluationDTO evaluationDTO = new InterviewAnswerEvaluationDTO();
        evaluationDTO.setScore(80);
        evaluationDTO.setFeedback("回答基本完整。");
        evaluationDTO.setReferenceAnswer("参考答案");
        evaluationDTO.setKeyPoints(List.of("分层设计", "项目经验"));
        return evaluationDTO;
    }
}
