package com.interview.modules.interview.service;

import static com.interview.modules.interview.model.InterviewSessionStatus.COMPLETED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.dto.CurrentQuestionResponseDTO;
import com.interview.modules.interview.model.dto.InterviewAnswerEvaluationDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
import com.interview.modules.interview.model.dto.SubmitAnswerResponse;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.interview.model.request.SaveAnswerRequest;
import com.interview.modules.interview.model.request.SubmitAnswerRequest;
import com.interview.modules.interview.repository.InterviewAnswerRepository;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.interview.service.comm.InterviewQuestionAnswerAggregator;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 面试会话管理服务。
 * 核心职责：管理面试会话的完整生命周期，包括创建、答题推进、提前交卷和报告生成。
 * <p>
 * 关键设计：
 * - currentQuestionIndex 表示"下一道待答题的索引"，从 0 开始
 * - 题目和答案统一存储在 questionsJson 字段中（当前阶段简化方案）
 * - 会话状态流转：CREATED → IN_PROGRESS → COMPLETED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewAnswerEvaluationService interviewAnswerEvaluationService;

    /**
     * 创建一场新的模拟面试。
     * 当前版本流程：查简历 -> 优先复用未完成会话 -> 生成题目并落库 -> 返回会话快照。
     */
    public InterviewSessionDTO createInterview(CreateInterviewRequest cplCreateInterviewRequest) {
        log.info("开始创建面试会话: resumeId={}, questionCount={}",
                cplCreateInterviewRequest.getResumeId(),
                cplCreateInterviewRequest.getQuestionCount());

        Optional<ResumeEntity> optResumeEntity = resumeRepository.findById(cplCreateInterviewRequest.getResumeId());

        if (optResumeEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在");
        }

        // 同一份简历在当前阶段只保留一条未完成会话，避免重复创建多场并行面试。
        List<InterviewSessionStatus> lstUnfinishedStatus = List.of(
                InterviewSessionStatus.CREATED,
                InterviewSessionStatus.IN_PROGRESS);
        Optional<InterviewSessionEntity> optUnfinishedInterviewSessionEntity = interviewSessionRepository
                .findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
                        cplCreateInterviewRequest.getResumeId(),
                        lstUnfinishedStatus);
        if (optUnfinishedInterviewSessionEntity.isPresent()) {
            String strSessionId = optUnfinishedInterviewSessionEntity.get().getSessionId();
            log.info("检测到未完成面试会话，直接复用: resumeId={}, sessionId={}",
                    cplCreateInterviewRequest.getResumeId(),
                    strSessionId);
            return getInterviewSession(strSessionId);
        }

        ResumeEntity tblResumeEntity = optResumeEntity.get(); // 简历实体
        String strResumeText = tblResumeEntity.getResumeText(); // 简历正文
        Integer intQuestionCount = cplCreateInterviewRequest.getQuestionCount(); // 题目数量

        List<InterviewQuestionDTO> lstInterviewQuestionDTO = buildQuestions(strResumeText, intQuestionCount); // 题目列表

        String strQuestionsJson;
        try {
            strQuestionsJson = objectMapper.writeValueAsString(lstInterviewQuestionDTO); // 题目列表JSON
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目序列化失败");
        }

        InterviewSessionEntity tblInterviewSessionEntity = new InterviewSessionEntity();
        tblInterviewSessionEntity.setSessionId(UUID.randomUUID().toString()); // 会话ID
        tblInterviewSessionEntity.setResume(tblResumeEntity); // 关联简历
        tblInterviewSessionEntity.setTotalQuestions(lstInterviewQuestionDTO.size()); // 题目总数
        tblInterviewSessionEntity.setCurrentQuestionIndex(0); // 当前题目索引
        tblInterviewSessionEntity.setStatus(InterviewSessionStatus.CREATED); // 会话状态
        tblInterviewSessionEntity.setQuestionsJson(strQuestionsJson); // 题目列表JSON

        InterviewSessionEntity tblSavedInterviewSessionEntity = interviewSessionRepository
                .save(tblInterviewSessionEntity);

        // 这里直接手动组装返回 DTO，先不抽公共方法，方便顺着代码理解返回值来源。
        InterviewSessionDTO cplInterviewSessionDTO = new InterviewSessionDTO();
        cplInterviewSessionDTO.setSessionId(tblSavedInterviewSessionEntity.getSessionId()); // 会话ID
        cplInterviewSessionDTO.setResumeId(tblResumeEntity.getId()); // 简历ID
        cplInterviewSessionDTO.setTotalQuestions(tblSavedInterviewSessionEntity.getTotalQuestions()); // 题目总数
        cplInterviewSessionDTO.setCurrentQuestionIndex(tblSavedInterviewSessionEntity.getCurrentQuestionIndex()); // 当前题目索引
        cplInterviewSessionDTO.setQuestions(lstInterviewQuestionDTO); // 题目列表
        cplInterviewSessionDTO.setStatus(tblSavedInterviewSessionEntity.getStatus()); // 会话状态
        cplInterviewSessionDTO.setCreatedAt(tblSavedInterviewSessionEntity.getCreatedAt()); // 创建时间

        log.info("创建面试会话成功: sessionId={}", tblSavedInterviewSessionEntity.getSessionId());
        return cplInterviewSessionDTO;
    }

    /**
     * 根据简历正文生成第一版规则题目。
     * 当前先用关键词命中方式生成题目，后续再平滑升级为 AI 出题。
     */
    private List<InterviewQuestionDTO> buildQuestions(String strResumeText, Integer intQuestionCount) {
        List<InterviewQuestionDTO> lstInterviewQuestionDTO = new ArrayList<>();

        lstInterviewQuestionDTO.add(createQuestion(0,
                "请你介绍一下自己，并重点说明你在简历中提到的后端项目经验。",
                "GENERAL",
                "综合表达"));

        if (strResumeText.contains("Spring Boot")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请讲一下你在项目中是如何使用 Spring Boot 做模块划分和接口设计的？",
                    "SPRING_BOOT",
                    "Spring Boot"));
        }

        if (strResumeText.contains("MySQL")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是如何设计 MySQL 索引并做 SQL 优化的？",
                    "MYSQL",
                    "MySQL"));
        }

        if (strResumeText.contains("Redis")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请介绍一下你在项目中使用 Redis 的场景，以及你是如何处理缓存一致性的？",
                    "REDIS",
                    "Redis"));
        }

        if (strResumeText.contains("Docker")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是怎么使用 Docker 的？它帮你解决了什么问题？",
                    "DOCKER",
                    "Docker"));
        }

        while (lstInterviewQuestionDTO.size() < intQuestionCount) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请结合你的项目经历，讲一个你实际解决过的技术问题，以及你的排查和优化过程。",
                    "PROJECT",
                    "项目经验"));
        }

        if (lstInterviewQuestionDTO.size() > intQuestionCount) {
            return lstInterviewQuestionDTO.subList(0, intQuestionCount);
        }

        return lstInterviewQuestionDTO;
    }

    /**
     * 构造单道面试题对象，供 buildQuestions 统一复用。
     */
    private InterviewQuestionDTO createQuestion(
            Integer intQuestionIndex,
            String strQuestion,
            String strType,
            String strCategory) {

        InterviewQuestionDTO cplInterviewQuestionDTO = new InterviewQuestionDTO();
        cplInterviewQuestionDTO.setQuestionIndex(intQuestionIndex); // 题目索引
        cplInterviewQuestionDTO.setQuestion(strQuestion); // 题目内容
        cplInterviewQuestionDTO.setType(strType); // 题目类型
        cplInterviewQuestionDTO.setCategory(strCategory); // 题目分类
        return cplInterviewQuestionDTO;
    }

    /**
     * 查询整场面试会话的完整快照。
     * 这个接口面向“看整场状态”，因此会返回全部题目、当前索引和会话状态等信息。
     */
    @Transactional(readOnly = true)
    public InterviewSessionDTO getInterviewSession(String strSessionId) {
        log.info("开始查询面试会话: sessionId={}", strSessionId);

        // 1. 先根据 sessionId 查会话。
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        // 2. 数据库中的 questionsJson 是字符串，这里反序列化回题目列表。
        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        List<InterviewAnswerEntity> lstInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionOrderByQuestionIndexAsc(tblInterviewSessionEntity);
        InterviewQuestionAnswerAggregator.fillUserAnswers(lstInterviewQuestionDTO, lstInterviewAnswerEntity);

        // 3. 手动组装返回 DTO。
        InterviewSessionDTO cplInterviewSessionDTO = new InterviewSessionDTO();
        cplInterviewSessionDTO.setSessionId(tblInterviewSessionEntity.getSessionId()); // 会话ID
        cplInterviewSessionDTO.setResumeId(tblInterviewSessionEntity.getResume().getId()); // 简历ID
        cplInterviewSessionDTO.setTotalQuestions(tblInterviewSessionEntity.getTotalQuestions()); // 题目总数
        cplInterviewSessionDTO.setCurrentQuestionIndex(tblInterviewSessionEntity.getCurrentQuestionIndex()); // 当前题目索引
        cplInterviewSessionDTO.setQuestions(lstInterviewQuestionDTO); // 题目列表
        cplInterviewSessionDTO.setStatus(tblInterviewSessionEntity.getStatus()); // 会话状态
        cplInterviewSessionDTO.setCreatedAt(tblInterviewSessionEntity.getCreatedAt()); // 创建时间

        log.info("查询面试会话成功: sessionId={}", strSessionId);
        return cplInterviewSessionDTO;
    }

    /**
     * 提交当前题答案，并返回“下一步怎么走”的结果。
     * 当前版本要求按顺序作答，返回值聚焦在是否还有下一题、下一题是谁以及当前进度。
     */
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest cplSubmitAnswerRequest) {
        log.info("开始提交面试答案: sessionId={}, questionIndex={}",
                cplSubmitAnswerRequest.getSessionId(),
                cplSubmitAnswerRequest.getQuestionIndex());

        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(cplSubmitAnswerRequest.getSessionId());

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        if (COMPLETED.equals(tblInterviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成，不能继续提交答案");
        }

        if (!cplSubmitAnswerRequest.getQuestionIndex().equals(tblInterviewSessionEntity.getCurrentQuestionIndex())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请按顺序作答");
        }

        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 先定位本次提交答案对应的题目对象，后面会直接把 userAnswer 写回这道题。
        InterviewQuestionDTO cplTargetQuestionDTO = null;
        for (InterviewQuestionDTO cplInterviewQuestionDTO : lstInterviewQuestionDTO) {
            if (cplSubmitAnswerRequest.getQuestionIndex().equals(cplInterviewQuestionDTO.getQuestionIndex())) {
                cplTargetQuestionDTO = cplInterviewQuestionDTO;
                break;
            }
        }

        if (cplTargetQuestionDTO == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND, "面试问题不存在");
        }

        // 提交答案时不再修改 questionsJson，而是写入独立答案表。
        // questionsJson 只负责保存题目快照，interview_answers 负责保存用户答案。
        Optional<InterviewAnswerEntity> optInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionAndQuestionIndex(
                        tblInterviewSessionEntity,
                        cplSubmitAnswerRequest.getQuestionIndex());

        InterviewAnswerEntity tblInterviewAnswerEntity;

        if (optInterviewAnswerEntity.isPresent()) {
            tblInterviewAnswerEntity = optInterviewAnswerEntity.get();
            tblInterviewAnswerEntity.setUserAnswer(cplSubmitAnswerRequest.getAnswer());
        } else {
            tblInterviewAnswerEntity = new InterviewAnswerEntity();
            tblInterviewAnswerEntity.setSession(tblInterviewSessionEntity);
            tblInterviewAnswerEntity.setQuestionIndex(cplTargetQuestionDTO.getQuestionIndex());
            tblInterviewAnswerEntity.setQuestion(cplTargetQuestionDTO.getQuestion());
            tblInterviewAnswerEntity.setCategory(cplTargetQuestionDTO.getCategory());
            tblInterviewAnswerEntity.setUserAnswer(cplSubmitAnswerRequest.getAnswer());
        }

        InterviewAnswerEntity tblSavedInterviewAnswerEntity = interviewAnswerRepository.save(tblInterviewAnswerEntity);
        fillAnswerEvaluation(tblSavedInterviewAnswerEntity);
        interviewAnswerRepository.save(tblSavedInterviewAnswerEntity);

        // 当前版本里 currentQuestionIndex 表示“下一道要回答的题目索引”。
        Integer intNextQuestionIndex = cplSubmitAnswerRequest.getQuestionIndex() + 1;
        Integer intCurrentQuestionIndex = Math.max(
                tblInterviewSessionEntity.getCurrentQuestionIndex(),
                intNextQuestionIndex);

        tblInterviewSessionEntity.setCurrentQuestionIndex(
                Math.min(intCurrentQuestionIndex, tblInterviewSessionEntity.getTotalQuestions())); // 当前题目索引
                                                                                                   // 如果索引已经推进到题目总数，说明整场面试已答完；否则仍处于进行中。
        if (tblInterviewSessionEntity.getCurrentQuestionIndex() >= tblInterviewSessionEntity.getTotalQuestions()) {
            tblInterviewSessionEntity.setStatus(COMPLETED); // 会话状态
        } else {
            tblInterviewSessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS); // 会话状态
        }

        InterviewSessionEntity tblSavedInterviewSessionEntity = interviewSessionRepository
                .save(tblInterviewSessionEntity);
        log.info("提交面试答案成功: sessionId={}, questionIndex={}, status={}",
                tblSavedInterviewSessionEntity.getSessionId(),
                cplSubmitAnswerRequest.getQuestionIndex(),
                tblSavedInterviewSessionEntity.getStatus());

        // SubmitAnswerResponse 不是整场会话快照，而是“本次提交后下一步怎么走”的动作结果。
        SubmitAnswerResponse cplSubmitAnswerResponse = new SubmitAnswerResponse();
        cplSubmitAnswerResponse.setCurrentQuestionIndex(tblSavedInterviewSessionEntity.getCurrentQuestionIndex());
        cplSubmitAnswerResponse.setTotalQuestions(tblSavedInterviewSessionEntity.getTotalQuestions());

        boolean bolHasNextQuestion = cplSubmitAnswerResponse.getCurrentQuestionIndex() < cplSubmitAnswerResponse
                .getTotalQuestions();

        cplSubmitAnswerResponse.setHasNextQuestion(bolHasNextQuestion);

        // nextQuestion 基于“提交后的最新索引”来取，不是本次提交的题目本身。
        InterviewQuestionDTO cplNextQuestionDTO = null;
        if (bolHasNextQuestion) {
            cplNextQuestionDTO = lstInterviewQuestionDTO.get(cplSubmitAnswerResponse.getCurrentQuestionIndex());
        }
        cplSubmitAnswerResponse.setNextQuestion(cplNextQuestionDTO);

        return cplSubmitAnswerResponse;
    }

    /**
     * 对已保存的答案生成评估结果，并回填到答案实体。
     *
     * 当前阶段采用同步评估：
     * - 优先调用 AI 生成评分、反馈、参考答案和关键点
     * - AI 失败时 InterviewAnswerEvaluationService 内部会自动使用规则版兜底
     * - keyPoints 使用 JSON 字符串保存，方便后续报告详情或前端展示
     */
    private void fillAnswerEvaluation(InterviewAnswerEntity tblInterviewAnswerEntity) {
        InterviewAnswerEvaluationDTO cplEvaluationDTO = interviewAnswerEvaluationService
                .evaluateAnswer(tblInterviewAnswerEntity);

        tblInterviewAnswerEntity.setScore(cplEvaluationDTO.getScore());
        tblInterviewAnswerEntity.setFeedback(cplEvaluationDTO.getFeedback());
        tblInterviewAnswerEntity.setReferenceAnswer(cplEvaluationDTO.getReferenceAnswer());

        try {
            tblInterviewAnswerEntity.setKeyPointsJson(
                    objectMapper.writeValueAsString(cplEvaluationDTO.getKeyPoints()));
        } catch (JacksonException e) {
            log.warn("面试答案关键点评估结果序列化失败: answerId={}", tblInterviewAnswerEntity.getId(), e);
            tblInterviewAnswerEntity.setKeyPointsJson("[]");
        }
    }

    /**
     * 获取当前流程步骤应该展示的题目。
     * 这个接口面向“当前该答哪一题”，而不是返回整场会话的全量信息。
     */
    public CurrentQuestionResponseDTO getCurrentQuestion(String strSessionId) {

        log.info("开始获取当前面试题: sessionId={}", strSessionId);

        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // currentQuestionIndex 表示下一道待答题目的索引。
        Integer intCurrentQuestionIndex = tblInterviewSessionEntity.getCurrentQuestionIndex();

        CurrentQuestionResponseDTO cplCurrentQuestionResponseDTO = new CurrentQuestionResponseDTO();

        // 当前索引越界说明已经没有下一题了，这属于正常完成状态，不是异常。
        if (intCurrentQuestionIndex == null || intCurrentQuestionIndex >= lstInterviewQuestionDTO.size()) {
            cplCurrentQuestionResponseDTO.setCompleted(true);
            cplCurrentQuestionResponseDTO.setMessage("所有问题已回答完毕");
            cplCurrentQuestionResponseDTO.setQuestion(null);
            return cplCurrentQuestionResponseDTO;
        }

        // 否则直接按当前索引定位当前题目，返回给前端展示。
        InterviewQuestionDTO cplInterviewQuestionDTO = lstInterviewQuestionDTO.get(intCurrentQuestionIndex);

        cplCurrentQuestionResponseDTO.setCompleted(false);
        cplCurrentQuestionResponseDTO.setMessage(null);
        cplCurrentQuestionResponseDTO.setQuestion(cplInterviewQuestionDTO);

        log.info("获取当前面试题成功: sessionId={}, questionIndex={}",
                strSessionId, intCurrentQuestionIndex);

        return cplCurrentQuestionResponseDTO;
    }

    /**
     * 提前交卷，将面试会话强制置为完成态。
     * 会把 currentQuestionIndex 推进到 totalQuestions，状态改为 COMPLETED。
     * 交卷后不允许继续提交答案，但允许生成报告。
     * 如果会话不存在或已经完成，则抛出业务异常。
     */
    public void completeInterview(String strSessionId) {
        log.info("开始提前完成面试: sessionId={}", strSessionId);
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }
        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        if (COMPLETED.equals(tblInterviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成");
        }

        tblInterviewSessionEntity.setCurrentQuestionIndex(tblInterviewSessionEntity.getTotalQuestions());
        tblInterviewSessionEntity.setStatus(COMPLETED);

        interviewSessionRepository.save(tblInterviewSessionEntity);
        log.info("面试提前完成: sessionId={}", strSessionId);
    }

    /**
     * 根据简历ID查询最近一条未完成的面试会话。
     * 未完成状态包括 CREATED 和 IN_PROGRESS。
     * 查到后复用 getInterviewSession() 组装完整 DTO。
     */
    public InterviewSessionDTO findUnfinishedSessionByResumeId(Long lngResumeId) {
        log.info("开始查询未完成面试会话: resumeId={}", lngResumeId);

        // 1. 先校验简历是否存在，避免"简历不存在却返回未找到会话"的歧义
        if (resumeRepository.findById(lngResumeId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在");
        }

        // 2. 定义未完成状态：刚创建或正在进行中
        List<InterviewSessionStatus> lstUnfinishedStatus = List.of(
                InterviewSessionStatus.CREATED,
                InterviewSessionStatus.IN_PROGRESS);

        // 3. 查最近一条未完成会话
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
                        lngResumeId,
                        lstUnfinishedStatus);

        // 4. 查不到就抛异常
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "未找到未完成的面试会话");
        }

        // 5. 复用已有方法组装完整 DTO，避免重复写反序列化逻辑
        String strSessionId = optInterviewSessionEntity.get().getSessionId();
        log.info("查询未完成面试会话成功: resumeId={}, sessionId={}", lngResumeId, strSessionId);
        return getInterviewSession(strSessionId);
    }

    /**
     * 暂存指定题目的答案草稿。
     * 与 submitAnswer 不同，此方法只更新答案内容，不推进 currentQuestionIndex。
     * 如果会话尚未开始作答，则在首次暂存后将状态从 CREATED 改为 IN_PROGRESS。
     */
    public void saveAnswer(String strSessionId, SaveAnswerRequest cplSaveAnswerRequest) {
        log.info("开始暂存面试答案: sessionId={}, questionIndex={}", strSessionId, cplSaveAnswerRequest.getQuestionIndex());

        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        if (COMPLETED.equals(tblInterviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成，不能继续暂存答案");
        }

        // 从会话快照中取出题目列表，定位本次需要暂存答案的题目。
        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 按题目索引查找目标题目，只更新这一题的答案内容。
        InterviewQuestionDTO cplTargetQuestionDTO = null;
        for (InterviewQuestionDTO cplInterviewQuestionDTO : lstInterviewQuestionDTO) {
            if (cplSaveAnswerRequest.getQuestionIndex().equals(cplInterviewQuestionDTO.getQuestionIndex())) {
                cplTargetQuestionDTO = cplInterviewQuestionDTO;
                break;
            }
        }

        if (cplTargetQuestionDTO == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND, "面试问题不存在");
        }

        Optional<InterviewAnswerEntity> optInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionAndQuestionIndex(tblInterviewSessionEntity, cplSaveAnswerRequest.getQuestionIndex());

        InterviewAnswerEntity tblInterviewAnswerEntity;

        if (optInterviewAnswerEntity.isPresent()) {
            tblInterviewAnswerEntity = optInterviewAnswerEntity.get();
            tblInterviewAnswerEntity.setUserAnswer(cplSaveAnswerRequest.getAnswer());
        } else {
            tblInterviewAnswerEntity = new InterviewAnswerEntity();
            tblInterviewAnswerEntity.setSession(tblInterviewSessionEntity);
            tblInterviewAnswerEntity.setQuestionIndex(cplTargetQuestionDTO.getQuestionIndex());
            tblInterviewAnswerEntity.setQuestion(cplTargetQuestionDTO.getQuestion());
            tblInterviewAnswerEntity.setCategory(cplTargetQuestionDTO.getCategory());
            tblInterviewAnswerEntity.setUserAnswer(cplSaveAnswerRequest.getAnswer());
        }

        // 暂存答案意味着用户已经开始作答，会话状态需要进入进行中。
        if (InterviewSessionStatus.CREATED.equals(tblInterviewSessionEntity.getStatus())) {
            tblInterviewSessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS);
        }

        interviewAnswerRepository.save(tblInterviewAnswerEntity);
        interviewSessionRepository.save(tblInterviewSessionEntity);

        log.info("暂存面试答案成功: sessionId={}, questionIndex={}",
                strSessionId,
                cplSaveAnswerRequest.getQuestionIndex());
    }

}
