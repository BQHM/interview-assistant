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
        ResumeEntity tblResumeEntity = createResume(11L, "熟悉 Spring Boot、MySQL、Redis。");
        List<InterviewQuestionDTO> lstQuestionDTO = createQuestions(3);
        CreateInterviewRequest cplRequest = createInterviewRequest(11L, 3);

        when(resumeRepository.findById(11L)).thenReturn(Optional.of(tblResumeEntity));
        when(interviewSessionRepository.findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
                eq(11L),
                eq("java-backend"),
                anyList())).thenReturn(Optional.empty());
        when(interviewQuestionService.generateQuestions(tblResumeEntity.getResumeText(), 3, "java-backend"))
                .thenReturn(lstQuestionDTO);
        when(interviewSessionRepository.save(any(InterviewSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InterviewSessionDTO cplSessionDTO = interviewSessionService.createInterview(cplRequest);

        assertThat(cplSessionDTO.getSessionId()).isNotBlank();
        assertThat(cplSessionDTO.getResumeId()).isEqualTo(11L);
        assertThat(cplSessionDTO.getSkillId()).isEqualTo("java-backend");
        assertThat(cplSessionDTO.getTotalQuestions()).isEqualTo(3);
        assertThat(cplSessionDTO.getCurrentQuestionIndex()).isZero();
        assertThat(cplSessionDTO.getStatus()).isEqualTo(InterviewSessionStatus.CREATED);
        assertThat(cplSessionDTO.getQuestions())
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2);
        verify(interviewSessionRepository).save(argThat(
                sessionEntity -> "java-backend".equals(sessionEntity.getSkillId())));
    }

    @Test
    void createInterview_shouldReuseUnfinishedSessionForSameResume() throws JacksonException {
        ResumeEntity tblResumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity tblExistingSessionEntity = createSession(
                tblResumeEntity,
                "session-001",
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);
        InterviewAnswerEntity tblAnswerEntity = createAnswer(
                tblExistingSessionEntity,
                0,
                "我会从项目模块和接口设计说明。");

        when(resumeRepository.findById(11L)).thenReturn(Optional.of(tblResumeEntity));
        when(interviewSessionRepository.findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
                eq(11L),
                eq("java-backend"),
                anyList())).thenReturn(Optional.of(tblExistingSessionEntity));
        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblExistingSessionEntity));
        when(interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(tblExistingSessionEntity))
                .thenReturn(List.of(tblAnswerEntity));

        InterviewSessionDTO cplSessionDTO = interviewSessionService.createInterview(
                createInterviewRequest(11L, 3));

        assertThat(cplSessionDTO.getSessionId()).isEqualTo("session-001");
        assertThat(cplSessionDTO.getSkillId()).isEqualTo("java-backend");
        assertThat(cplSessionDTO.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
        assertThat(cplSessionDTO.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(cplSessionDTO.getQuestions().get(0).getUserAnswer())
                .isEqualTo("我会从项目模块和接口设计说明。");
        verify(interviewQuestionService, never()).generateQuestions(any(), any(), any());
        verify(interviewSessionRepository, never()).save(any(InterviewSessionEntity.class));
    }

    @Test
    void submitAnswer_shouldAdvanceCurrentQuestionIndex() throws JacksonException {
        ResumeEntity tblResumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity tblSessionEntity = createSession(
                tblResumeEntity,
                "session-001",
                InterviewSessionStatus.CREATED,
                0,
                3);
        SubmitAnswerRequest cplRequest = createSubmitAnswerRequest(
                "session-001",
                0,
                "我会先按 Controller、Service、Repository 分层说明项目结构。");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));
        when(interviewAnswerRepository.findBySessionAndQuestionIndex(tblSessionEntity, 0))
                .thenReturn(Optional.empty());
        when(interviewAnswerRepository.save(any(InterviewAnswerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(interviewAnswerEvaluationService.evaluateAnswer(any(InterviewAnswerEntity.class)))
                .thenReturn(createEvaluation());
        when(interviewSessionRepository.save(tblSessionEntity)).thenReturn(tblSessionEntity);

        SubmitAnswerResponse cplResponse = interviewSessionService.submitAnswer(cplRequest);

        assertThat(cplResponse.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(cplResponse.getHasNextQuestion()).isTrue();
        assertThat(cplResponse.getNextQuestion().getQuestionIndex()).isEqualTo(1);
        assertThat(tblSessionEntity.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(tblSessionEntity.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
    }

    @Test
    void submitAnswer_shouldRejectCompletedSession() throws JacksonException {
        ResumeEntity tblResumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity tblSessionEntity = createSession(
                tblResumeEntity,
                "session-001",
                InterviewSessionStatus.COMPLETED,
                3,
                3);
        SubmitAnswerRequest cplRequest = createSubmitAnswerRequest(
                "session-001",
                0,
                "面试完成后不应该还能提交。");

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));

        BusinessException exception = catchThrowableOfType(
                () -> interviewSessionService.submitAnswer(cplRequest),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTERVIEW_ALREADY_COMPLETED.getCode());
        verify(interviewAnswerRepository, never()).save(any(InterviewAnswerEntity.class));
    }

    @Test
    void completeInterview_shouldMarkSessionCompletedBeforeAllAnswers() throws JacksonException {
        ResumeEntity tblResumeEntity = createResume(11L, "熟悉 Spring Boot。");
        InterviewSessionEntity tblSessionEntity = createSession(
                tblResumeEntity,
                "session-001",
                InterviewSessionStatus.IN_PROGRESS,
                1,
                3);

        when(interviewSessionRepository.findBySessionId("session-001"))
                .thenReturn(Optional.of(tblSessionEntity));
        when(interviewSessionRepository.save(tblSessionEntity)).thenReturn(tblSessionEntity);

        interviewSessionService.completeInterview("session-001");

        assertThat(tblSessionEntity.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(tblSessionEntity.getCurrentQuestionIndex()).isEqualTo(3);
        verify(interviewSessionRepository).save(tblSessionEntity);
    }

    private CreateInterviewRequest createInterviewRequest(Long lngResumeId, Integer intQuestionCount) {
        CreateInterviewRequest cplRequest = new CreateInterviewRequest();
        cplRequest.setResumeId(lngResumeId);
        cplRequest.setQuestionCount(intQuestionCount);
        return cplRequest;
    }

    private SubmitAnswerRequest createSubmitAnswerRequest(
            String strSessionId,
            Integer intQuestionIndex,
            String strAnswer) {
        SubmitAnswerRequest cplRequest = new SubmitAnswerRequest();
        cplRequest.setSessionId(strSessionId);
        cplRequest.setQuestionIndex(intQuestionIndex);
        cplRequest.setAnswer(strAnswer);
        return cplRequest;
    }

    private ResumeEntity createResume(Long lngResumeId, String strResumeText) {
        ResumeEntity tblResumeEntity = new ResumeEntity();
        tblResumeEntity.setId(lngResumeId);
        tblResumeEntity.setResumeText(strResumeText);
        return tblResumeEntity;
    }

    private InterviewSessionEntity createSession(
            ResumeEntity tblResumeEntity,
            String strSessionId,
            InterviewSessionStatus status,
            Integer intCurrentQuestionIndex,
            Integer intTotalQuestions) throws JacksonException {
        InterviewSessionEntity tblSessionEntity = new InterviewSessionEntity();
        tblSessionEntity.setSessionId(strSessionId);
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
            String strUserAnswer) {
        InterviewAnswerEntity tblAnswerEntity = new InterviewAnswerEntity();
        tblAnswerEntity.setSession(tblSessionEntity);
        tblAnswerEntity.setQuestionIndex(intQuestionIndex);
        tblAnswerEntity.setQuestion("第 " + intQuestionIndex + " 题");
        tblAnswerEntity.setCategory("项目经验");
        tblAnswerEntity.setUserAnswer(strUserAnswer);
        return tblAnswerEntity;
    }

    private InterviewAnswerEvaluationDTO createEvaluation() {
        InterviewAnswerEvaluationDTO cplEvaluationDTO = new InterviewAnswerEvaluationDTO();
        cplEvaluationDTO.setScore(80);
        cplEvaluationDTO.setFeedback("回答基本完整。");
        cplEvaluationDTO.setReferenceAnswer("参考答案");
        cplEvaluationDTO.setKeyPoints(List.of("分层设计", "项目经验"));
        return cplEvaluationDTO;
    }
}
