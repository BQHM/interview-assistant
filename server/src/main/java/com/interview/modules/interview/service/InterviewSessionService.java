package com.interview.modules.interview.service;

import static com.interview.modules.interview.model.InterviewSessionStatus.COMPLETED;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.interview.modules.interview.model.HistoricalQuestion;
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
 * 文件功能说明
 * <p>负责面试会话创建、答题、交卷等业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-05-28
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
    private final InterviewQuestionService interviewQuestionService;
    /**
     * 注入提示词的历史题目上限，避免历史越长提示词越大
     */
    private static final int MAX_HISTORICAL_QUESTIONS = 60;

    /**
     * 功能说明
     * <p>创建面试会话。</p>
     *
     * @param createInterviewRequest 创建面试请求
     * @return 面试会话信息
     * @author NobuNo
     * @date 2026-05-28
     */
    public InterviewSessionDTO createInterview(CreateInterviewRequest createInterviewRequest) {
        log.info("开始创建面试会话: resumeId={}, questionCount={}, skillId={}",
                createInterviewRequest.getResumeId(),
                createInterviewRequest.getQuestionCount(),
                createInterviewRequest.getSkillId()
        );

        Optional<ResumeEntity> optResumeEntity = resumeRepository.findById(createInterviewRequest.getResumeId());

        if (optResumeEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在");
        }

        String skillId = createInterviewRequest.getSkillId(); // 面试方向编号

        // 第一步：同一份简历、同一面试方向只复用最近一条未完成会话。
        // 这样用户重复点击“开始面试”时，不会意外创建多场相同的未完成面试。
        List<InterviewSessionStatus> unfinishedStatuses = List.of(InterviewSessionStatus.CREATED, InterviewSessionStatus.IN_PROGRESS);

        Optional<InterviewSessionEntity> optUnfinishedInterviewSessionEntity = interviewSessionRepository.findFirstByResumeIdAndSkillIdAndStatusInOrderByCreatedAtDesc(
                createInterviewRequest.getResumeId(),
                skillId,
                unfinishedStatuses);
        if (optUnfinishedInterviewSessionEntity.isPresent()) {
            String sessionId = optUnfinishedInterviewSessionEntity.get().getSessionId();
            log.info("检测到未完成面试会话，直接复用: resumeId={}, sessionId={}", createInterviewRequest.getResumeId(), sessionId);
            return getInterviewSession(sessionId);
        }

        // 第二步：加载历史题目。
        // 这些题目不会直接展示给用户，而是会在后续传给出题服务，提醒 AI 避免重复出题。
        List<HistoricalQuestion> historicalQuestions =
                getHistoricalQuestions(createInterviewRequest.getResumeId(), skillId);

        log.info("解析历史面试题目完成: resumeId={}, skillId={}, questionCount={}",
                createInterviewRequest.getResumeId(), skillId, historicalQuestions.size());

        // 第三步：准备当前简历和本次面试的基本参数。
        ResumeEntity resumeEntity = optResumeEntity.get(); // 简历实体
        String resumeText = resumeEntity.getResumeText(); // 简历正文
        Integer questionCount = createInterviewRequest.getQuestionCount(); // 题目数量

        // 第四步：根据简历、Skill 和历史题目生成新的面试题。
        List<InterviewQuestionDTO> interviewQuestionDTOs = interviewQuestionService.generateQuestions(resumeText, questionCount, skillId, historicalQuestions); // 题目列表

        String questionsJson;
        try {
            // 题目列表转 JSON
            questionsJson = objectMapper.writeValueAsString(interviewQuestionDTOs); // 题目列表JSON
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目序列化失败");
        }

        InterviewSessionEntity interviewSessionEntity = new InterviewSessionEntity();
        interviewSessionEntity.setSessionId(UUID.randomUUID().toString()); // 会话ID
        interviewSessionEntity.setResume(resumeEntity); // 关联简历
        interviewSessionEntity.setSkillId(skillId); // 面试方向编号
        interviewSessionEntity.setTotalQuestions(interviewQuestionDTOs.size()); // 题目总数
        interviewSessionEntity.setCurrentQuestionIndex(0); // 当前题目索引
        interviewSessionEntity.setStatus(InterviewSessionStatus.CREATED); // 会话状态
        interviewSessionEntity.setQuestionsJson(questionsJson); // 题目列表JSON

        // 保存会话实体
        InterviewSessionEntity savedInterviewSessionEntity = interviewSessionRepository.save(interviewSessionEntity);

        // 这里直接手动组装返回 DTO，先不抽公共方法，方便顺着代码理解返回值来源。
        InterviewSessionDTO interviewSessionDTO = new InterviewSessionDTO();
        interviewSessionDTO.setSessionId(savedInterviewSessionEntity.getSessionId()); // 会话ID
        interviewSessionDTO.setResumeId(resumeEntity.getId()); // 简历ID
        interviewSessionDTO.setTotalQuestions(savedInterviewSessionEntity.getTotalQuestions()); // 题目总数
        interviewSessionDTO.setCurrentQuestionIndex(savedInterviewSessionEntity.getCurrentQuestionIndex()); // 当前题目索引
        interviewSessionDTO.setQuestions(interviewQuestionDTOs); // 题目列表
        interviewSessionDTO.setStatus(savedInterviewSessionEntity.getStatus()); // 会话状态
        interviewSessionDTO.setCreatedAt(savedInterviewSessionEntity.getCreatedAt()); // 创建时间
        interviewSessionDTO.setSkillId(savedInterviewSessionEntity.getSkillId()); // 面试方向编号

        log.info("创建面试会话成功: sessionId={}", savedInterviewSessionEntity.getSessionId());
        return interviewSessionDTO;
    }

    /**
     * 功能说明
     * <p>查询面试会话详情。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试会话信息
     * @author NobuNo
     * @date 2026-05-28
     */
    @Transactional(readOnly = true)
    public InterviewSessionDTO getInterviewSession(String sessionId) {
        log.info("开始查询面试会话: sessionId={}", sessionId);

        // 1. 先根据 sessionId 查会话。
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository.findBySessionId(sessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        // 2. 数据库中的 questionsJson 是字符串，这里反序列化回题目列表。
        List<InterviewQuestionDTO> interviewQuestionDTOs;
        try {
            interviewQuestionDTOs = objectMapper.readValue(interviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 2.1. 根据会话ID查答案列表
        List<InterviewAnswerEntity> interviewAnswerEntities = interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(interviewSessionEntity);

        // 2.2. 填充用户答案
        InterviewQuestionAnswerAggregator.fillUserAnswers(interviewQuestionDTOs, interviewAnswerEntities);

        // 3. 手动组装返回 DTO。
        InterviewSessionDTO interviewSessionDTO = new InterviewSessionDTO();
        interviewSessionDTO.setSessionId(interviewSessionEntity.getSessionId()); // 会话ID
        interviewSessionDTO.setResumeId(interviewSessionEntity.getResume().getId()); // 简历ID
        interviewSessionDTO.setTotalQuestions(interviewSessionEntity.getTotalQuestions()); // 题目总数
        interviewSessionDTO.setSkillId(interviewSessionEntity.getSkillId()); // 面试方向编号
        interviewSessionDTO.setCurrentQuestionIndex(interviewSessionEntity.getCurrentQuestionIndex()); // 当前题目索引
        interviewSessionDTO.setQuestions(interviewQuestionDTOs); // 题目列表
        interviewSessionDTO.setStatus(interviewSessionEntity.getStatus()); // 会话状态
        interviewSessionDTO.setCreatedAt(interviewSessionEntity.getCreatedAt()); // 创建时间

        log.info("查询面试会话成功: sessionId={}", sessionId);
        return interviewSessionDTO;
    }

    /**
     * 功能说明
     * <p>提交面试答案。</p>
     *
     * @param submitAnswerRequest 提交答案请求
     * @return 提交答案结果
     * @author NobuNo
     * @date 2026-05-28
     */
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest submitAnswerRequest) {
        log.info("开始提交面试答案: sessionId={}, questionIndex={}",
                submitAnswerRequest.getSessionId(),
                submitAnswerRequest.getQuestionIndex());

        // 1. 根据 sessionId 查会话。
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository.findBySessionId(submitAnswerRequest.getSessionId());
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        // 1.1. 检查会话状态是否为已完成
        if (COMPLETED.equals(interviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成，不能继续提交答案");
        }

        if (!submitAnswerRequest.getQuestionIndex().equals(interviewSessionEntity.getCurrentQuestionIndex())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请按顺序作答");
        }

        List<InterviewQuestionDTO> interviewQuestionDTOs;
        try {
            interviewQuestionDTOs = objectMapper.readValue(
                    interviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 先定位本次提交答案对应的题目对象，后面会直接把 userAnswer 写回这道题。
        InterviewQuestionDTO targetQuestionDTO = null;
        for (InterviewQuestionDTO interviewQuestionDTO : interviewQuestionDTOs) {
            if (submitAnswerRequest.getQuestionIndex().equals(interviewQuestionDTO.getQuestionIndex())) {
                targetQuestionDTO = interviewQuestionDTO;
                break;
            }
        }

        if (targetQuestionDTO == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND, "面试问题不存在");
        }

        // 提交答案时不再修改 questionsJson，而是写入独立答案表。
        // questionsJson 只负责保存题目快照，interview_answers 负责保存用户答案。
        Optional<InterviewAnswerEntity> optInterviewAnswerEntity = interviewAnswerRepository.findBySessionAndQuestionIndex(interviewSessionEntity, submitAnswerRequest.getQuestionIndex());

        InterviewAnswerEntity interviewAnswerEntity;

        if (optInterviewAnswerEntity.isPresent()) {
            interviewAnswerEntity = optInterviewAnswerEntity.get();
            interviewAnswerEntity.setUserAnswer(submitAnswerRequest.getAnswer());
        } else {
            interviewAnswerEntity = new InterviewAnswerEntity();
            interviewAnswerEntity.setSession(interviewSessionEntity);
            interviewAnswerEntity.setQuestionIndex(targetQuestionDTO.getQuestionIndex());
            interviewAnswerEntity.setQuestion(targetQuestionDTO.getQuestion());
            interviewAnswerEntity.setCategory(targetQuestionDTO.getCategory());
            interviewAnswerEntity.setUserAnswer(submitAnswerRequest.getAnswer());
        }

        InterviewAnswerEntity savedInterviewAnswerEntity = interviewAnswerRepository.save(interviewAnswerEntity);
        fillAnswerEvaluation(savedInterviewAnswerEntity);
        interviewAnswerRepository.save(savedInterviewAnswerEntity);

        // 当前版本里 currentQuestionIndex 表示“下一道要回答的题目索引”。
        Integer nextQuestionIndex = submitAnswerRequest.getQuestionIndex() + 1;
        Integer currentQuestionIndex = Math.max(interviewSessionEntity.getCurrentQuestionIndex(), nextQuestionIndex);

        // 更新当前题目索引
        interviewSessionEntity.setCurrentQuestionIndex(Math.min(currentQuestionIndex, interviewSessionEntity.getTotalQuestions())); // 当前题目索引

        // 如果索引已经推进到题目总数，说明整场面试已答完；否则仍处于进行中。
        if (interviewSessionEntity.getCurrentQuestionIndex() >= interviewSessionEntity.getTotalQuestions()) {
            interviewSessionEntity.setStatus(COMPLETED); // 会话状态
        } else {
            interviewSessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS); // 会话状态
        }

        InterviewSessionEntity savedInterviewSessionEntity = interviewSessionRepository
                .save(interviewSessionEntity);
        log.info("提交面试答案成功: sessionId={}, questionIndex={}, status={}", savedInterviewSessionEntity.getSessionId(), submitAnswerRequest.getQuestionIndex(), savedInterviewSessionEntity.getStatus());

        // SubmitAnswerResponse 不是整场会话快照，而是“本次提交后下一步怎么走”的动作结果。
        SubmitAnswerResponse submitAnswerResponse = new SubmitAnswerResponse();
        submitAnswerResponse.setCurrentQuestionIndex(savedInterviewSessionEntity.getCurrentQuestionIndex());
        submitAnswerResponse.setTotalQuestions(savedInterviewSessionEntity.getTotalQuestions());

        boolean bolHasNextQuestion = submitAnswerResponse.getCurrentQuestionIndex() < submitAnswerResponse
                .getTotalQuestions();

        submitAnswerResponse.setHasNextQuestion(bolHasNextQuestion);

        // nextQuestion 基于“提交后的最新索引”来取，不是本次提交的题目本身。
        InterviewQuestionDTO nextQuestionDTO = null;
        if (bolHasNextQuestion) {
            nextQuestionDTO = interviewQuestionDTOs.get(submitAnswerResponse.getCurrentQuestionIndex());
        }
        submitAnswerResponse.setNextQuestion(nextQuestionDTO);

        return submitAnswerResponse;
    }

    /**
     * 功能说明
     * <p>填充答案评估结果。</p>
     *
     * @param interviewAnswerEntity 答案实体
     * @author NobuNo
     * @date 2026-05-28
     */
    private void fillAnswerEvaluation(InterviewAnswerEntity interviewAnswerEntity) {
        InterviewAnswerEvaluationDTO evaluationDTO = interviewAnswerEvaluationService
                .evaluateAnswer(interviewAnswerEntity);

        interviewAnswerEntity.setScore(evaluationDTO.getScore());
        interviewAnswerEntity.setFeedback(evaluationDTO.getFeedback());
        interviewAnswerEntity.setReferenceAnswer(evaluationDTO.getReferenceAnswer());

        try {
            interviewAnswerEntity.setKeyPointsJson(
                    objectMapper.writeValueAsString(evaluationDTO.getKeyPoints()));
        } catch (JacksonException e) {
            log.warn("面试答案关键点评估结果序列化失败: answerId={}", interviewAnswerEntity.getId(), e);
            interviewAnswerEntity.setKeyPointsJson("[]");
        }
    }

    /**
     * 功能说明
     * <p>获取当前面试题。</p>
     *
     * @param sessionId 面试会话编号
     * @return 当前面试题信息
     * @author NobuNo
     * @date 2026-05-28
     */
    public CurrentQuestionResponseDTO getCurrentQuestion(String sessionId) {

        log.info("开始获取当前面试题: sessionId={}", sessionId);

        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        List<InterviewQuestionDTO> interviewQuestionDTOs;
        try {
            interviewQuestionDTOs = objectMapper.readValue(
                    interviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // currentQuestionIndex 表示下一道待答题目的索引。
        Integer currentQuestionIndex = interviewSessionEntity.getCurrentQuestionIndex();

        CurrentQuestionResponseDTO currentQuestionResponseDTO = new CurrentQuestionResponseDTO();

        // 当前索引越界说明已经没有下一题了，这属于正常完成状态，不是异常。
        if (currentQuestionIndex == null || currentQuestionIndex >= interviewQuestionDTOs.size()) {
            currentQuestionResponseDTO.setCompleted(true);
            currentQuestionResponseDTO.setMessage("所有问题已回答完毕");
            currentQuestionResponseDTO.setQuestion(null);
            return currentQuestionResponseDTO;
        }

        // 否则直接按当前索引定位当前题目，返回给前端展示。
        InterviewQuestionDTO interviewQuestionDTO = interviewQuestionDTOs.get(currentQuestionIndex);

        currentQuestionResponseDTO.setCompleted(false);
        currentQuestionResponseDTO.setMessage(null);
        currentQuestionResponseDTO.setQuestion(interviewQuestionDTO);

        log.info("获取当前面试题成功: sessionId={}, questionIndex={}",
                sessionId, currentQuestionIndex);

        return currentQuestionResponseDTO;
    }

    /**
     * 功能说明
     * <p>提前完成面试。</p>
     *
     * @param sessionId 面试会话编号
     * @author NobuNo
     * @date 2026-05-28
     */
    public void completeInterview(String sessionId) {
        log.info("开始提前完成面试: sessionId={}", sessionId);
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }
        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        if (COMPLETED.equals(interviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成");
        }

        interviewSessionEntity.setCurrentQuestionIndex(interviewSessionEntity.getTotalQuestions());
        interviewSessionEntity.setStatus(COMPLETED);

        interviewSessionRepository.save(interviewSessionEntity);
        log.info("面试提前完成: sessionId={}", sessionId);
    }

    /**
     * 功能说明
     * <p>根据简历编号查询未完成面试。</p>
     *
     * @param resumeId 简历编号
     * @return 未完成面试会话信息
     * @author NobuNo
     * @date 2026-05-28
     */
    public InterviewSessionDTO findUnfinishedSessionByResumeId(Long resumeId) {
        log.info("开始查询未完成面试会话: resumeId={}", resumeId);

        // 1. 先校验简历是否存在，避免"简历不存在却返回未找到会话"的歧义
        if (resumeRepository.findById(resumeId).isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在");
        }

        // 2. 定义未完成状态：刚创建或正在进行中
        List<InterviewSessionStatus> unfinishedStatuses = List.of(
                InterviewSessionStatus.CREATED,
                InterviewSessionStatus.IN_PROGRESS);

        // 3. 查最近一条未完成会话
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findFirstByResumeIdAndStatusInOrderByCreatedAtDesc(
                        resumeId,
                        unfinishedStatuses);

        // 4. 查不到就抛异常
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "未找到未完成的面试会话");
        }

        // 5. 复用已有方法组装完整 DTO，避免重复写反序列化逻辑
        String sessionId = optInterviewSessionEntity.get().getSessionId();
        log.info("查询未完成面试会话成功: resumeId={}, sessionId={}", resumeId, sessionId);
        return getInterviewSession(sessionId);
    }

    /**
     * 功能说明
     * <p>暂存面试答案。</p>
     *
     * @param sessionId         面试会话编号
     * @param saveAnswerRequest 暂存答案请求
     * @author NobuNo
     * @date 2026-05-28
     */
    public void saveAnswer(String sessionId, SaveAnswerRequest saveAnswerRequest) {
        log.info("开始暂存面试答案: sessionId={}, questionIndex={}", sessionId, saveAnswerRequest.getQuestionIndex());

        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        if (COMPLETED.equals(interviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "面试已完成，不能继续暂存答案");
        }

        // 从会话快照中取出题目列表，定位本次需要暂存答案的题目。
        List<InterviewQuestionDTO> interviewQuestionDTOs;
        try {
            interviewQuestionDTOs = objectMapper.readValue(
                    interviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 按题目索引查找目标题目，只更新这一题的答案内容。
        InterviewQuestionDTO targetQuestionDTO = null;
        for (InterviewQuestionDTO interviewQuestionDTO : interviewQuestionDTOs) {
            if (saveAnswerRequest.getQuestionIndex().equals(interviewQuestionDTO.getQuestionIndex())) {
                targetQuestionDTO = interviewQuestionDTO;
                break;
            }
        }

        if (targetQuestionDTO == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND, "面试问题不存在");
        }

        Optional<InterviewAnswerEntity> optInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionAndQuestionIndex(interviewSessionEntity, saveAnswerRequest.getQuestionIndex());

        InterviewAnswerEntity interviewAnswerEntity;

        if (optInterviewAnswerEntity.isPresent()) {
            interviewAnswerEntity = optInterviewAnswerEntity.get();
            interviewAnswerEntity.setUserAnswer(saveAnswerRequest.getAnswer());
        } else {
            interviewAnswerEntity = new InterviewAnswerEntity();
            interviewAnswerEntity.setSession(interviewSessionEntity);
            interviewAnswerEntity.setQuestionIndex(targetQuestionDTO.getQuestionIndex());
            interviewAnswerEntity.setQuestion(targetQuestionDTO.getQuestion());
            interviewAnswerEntity.setCategory(targetQuestionDTO.getCategory());
            interviewAnswerEntity.setUserAnswer(saveAnswerRequest.getAnswer());
        }

        // 暂存答案意味着用户已经开始作答，会话状态需要进入进行中。
        if (InterviewSessionStatus.CREATED.equals(interviewSessionEntity.getStatus())) {
            interviewSessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS);
        }

        interviewAnswerRepository.save(interviewAnswerEntity);
        interviewSessionRepository.save(interviewSessionEntity);

        log.info("暂存面试答案成功: sessionId={}, questionIndex={}",
                sessionId,
                saveAnswerRequest.getQuestionIndex());
    }

    /**
     * 功能说明
     * <p>查询并提取同一简历、同一面试方向的历史题目。</p>
     *
     * <p>方法只保留题目正文、题目类型和知识点摘要等出题所需信息，不修改历史会话数据。</p>
     *
     * @param resumeId 简历编号
     * @param skillId  面试方向编号
     * @return 去重后的历史题目列表
     * @author NobuNo
     * @date 2026-08-31
     */
    private List<HistoricalQuestion> getHistoricalQuestions(Long resumeId, String skillId) {
        // 只读取最近 10 场同简历、同 Skill 的会话，避免历史数据过多导致查询和 Prompt 过大。
        List<InterviewSessionEntity> sessionList =
                interviewSessionRepository.findTop10ByResumeIdAndSkillIdOrderByCreatedAtDesc(resumeId, skillId);

        // LinkedHashSet 同时具备“判断是否重复”和“保持原有顺序”两个作用。
        LinkedHashSet<String> seenQuestionSet = new LinkedHashSet<>();
        List<HistoricalQuestion> historicalQuestions = new ArrayList<>();

        for (InterviewSessionEntity sessionEntity : sessionList) {
            // 每个会话的题目以 JSON 字符串保存，这里先还原成题目对象列表。
            try {
                List<InterviewQuestionDTO> questionList =
                        objectMapper.readValue(
                                sessionEntity.getQuestionsJson(),
                                new TypeReference<List<InterviewQuestionDTO>>() {
                                });

                for (InterviewQuestionDTO questionDTO : questionList) {
                    // 收集够了就不再继续
                    if (historicalQuestions.size() >= MAX_HISTORICAL_QUESTIONS) {
                        break;
                    }

                    // 空题目没有去重价值，也不能作为有效的历史出题上下文。
                    if (questionDTO == null || questionDTO.getQuestion() == null || questionDTO.getQuestion().isBlank()) {
                        continue;
                    }

                    // add 返回 true 表示题目第一次出现；返回 false 表示已经存在，自动跳过重复题。
                    if (seenQuestionSet.add(questionDTO.getQuestion())) {
                        historicalQuestions.add(
                                new HistoricalQuestion(questionDTO.getQuestion(), questionDTO.getType(), questionDTO.getTopicSummary()));
                    }
                }
            } catch (JacksonException e) {
                log.warn("解析历史面试题目失败: sessionId={}", sessionEntity.getSessionId(), e);
            }
        }

        return historicalQuestions;
    }
}
