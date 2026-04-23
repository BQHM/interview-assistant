package com.interview.modules.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.dto.*;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.request.SubmitAnswerRequest;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.interview.modules.interview.model.InterviewSessionStatus.COMPLETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建一场新的模拟面试。
     * 当前版本流程：查简历 -> 按简历关键词生成题目 -> 题目列表序列化入库 -> 返回会话快照。
     */
    public InterviewSessionDTO createInterview(CreateInterviewRequest cplCreateInterviewRequest) {
        log.info("开始创建面试会话: resumeId={}, questionCount={}",
                cplCreateInterviewRequest.getResumeId(),
                cplCreateInterviewRequest.getQuestionCount());

        Optional<ResumeEntity> optResumeEntity =
                resumeRepository.findById(cplCreateInterviewRequest.getResumeId());

        if (optResumeEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND, "简历不存在");
        }

        ResumeEntity tblResumeEntity = optResumeEntity.get(); // 简历实体
        String strResumeText = tblResumeEntity.getResumeText(); // 简历正文
        Integer intQuestionCount = cplCreateInterviewRequest.getQuestionCount(); // 题目数量

        List<InterviewQuestionDTO> lstInterviewQuestionDTO =
                buildQuestions(strResumeText, intQuestionCount); // 题目列表

        String strQuestionsJson;
        try {
            strQuestionsJson = objectMapper.writeValueAsString(lstInterviewQuestionDTO); // 题目列表JSON
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目序列化失败");
        }

        InterviewSessionEntity tblInterviewSessionEntity = new InterviewSessionEntity();
        tblInterviewSessionEntity.setSessionId(UUID.randomUUID().toString()); // 会话ID
        tblInterviewSessionEntity.setResume(tblResumeEntity); // 关联简历
        tblInterviewSessionEntity.setTotalQuestions(lstInterviewQuestionDTO.size()); // 题目总数
        tblInterviewSessionEntity.setCurrentQuestionIndex(0); // 当前题目索引
        tblInterviewSessionEntity.setStatus(InterviewSessionStatus.CREATED); // 会话状态
        tblInterviewSessionEntity.setQuestionsJson(strQuestionsJson); // 题目列表JSON

        InterviewSessionEntity tblSavedInterviewSessionEntity =
                interviewSessionRepository.save(tblInterviewSessionEntity);

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
                    "Spring Boot"
            ));
        }

        if (strResumeText.contains("MySQL")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是如何设计 MySQL 索引并做 SQL 优化的？",
                    "MYSQL",
                    "MySQL"
            ));
        }

        if (strResumeText.contains("Redis")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请介绍一下你在项目中使用 Redis 的场景，以及你是如何处理缓存一致性的？",
                    "REDIS",
                    "Redis"
            ));
        }

        if (strResumeText.contains("Docker")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是怎么使用 Docker 的？它帮你解决了什么问题？",
                    "DOCKER",
                    "Docker"
            ));
        }

        while (lstInterviewQuestionDTO.size() < intQuestionCount) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请结合你的项目经历，讲一个你实际解决过的技术问题，以及你的排查和优化过程。",
                    "PROJECT",
                    "项目经验"
            ));
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
    public InterviewSessionDTO getInterviewSession(String strSessionId) {
        log.info("开始查询面试会话: sessionId={}", strSessionId);

        // 1. 先根据 sessionId 查会话。
        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        // 2. 数据库中的 questionsJson 是字符串，这里反序列化回题目列表。
        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

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

        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(cplSubmitAnswerRequest.getSessionId());

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
                    new TypeReference<List<InterviewQuestionDTO>>() {}
            );
        } catch (JsonProcessingException e) {
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

        cplTargetQuestionDTO.setUserAnswer(cplSubmitAnswerRequest.getAnswer()); // 用户答案

        // 当前版本里 currentQuestionIndex 表示“下一道要回答的题目索引”。
        Integer intNextQuestionIndex = cplSubmitAnswerRequest.getQuestionIndex() + 1;
        Integer intCurrentQuestionIndex = Math.max(
                tblInterviewSessionEntity.getCurrentQuestionIndex(),
                intNextQuestionIndex);

        tblInterviewSessionEntity.setCurrentQuestionIndex(
                Math.min(intCurrentQuestionIndex, tblInterviewSessionEntity.getTotalQuestions())
        ); // 当前题目索引
        // 如果索引已经推进到题目总数，说明整场面试已答完；否则仍处于进行中。
        if (tblInterviewSessionEntity.getCurrentQuestionIndex() >= tblInterviewSessionEntity.getTotalQuestions()) {
            tblInterviewSessionEntity.setStatus(COMPLETED); // 会话状态
        } else {
            tblInterviewSessionEntity.setStatus(InterviewSessionStatus.IN_PROGRESS); // 会话状态
        }

        String strQuestionsJson;
        try {
            strQuestionsJson = objectMapper.writeValueAsString(lstInterviewQuestionDTO);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目序列化失败");
        }

        tblInterviewSessionEntity.setQuestionsJson(strQuestionsJson); // 题目列表JSON

        InterviewSessionEntity tblSavedInterviewSessionEntity =
                interviewSessionRepository.save(tblInterviewSessionEntity);
        log.info("提交面试答案成功: sessionId={}, questionIndex={}, status={}",
                tblSavedInterviewSessionEntity.getSessionId(),
                cplSubmitAnswerRequest.getQuestionIndex(),
                tblSavedInterviewSessionEntity.getStatus());

        // SubmitAnswerResponse 不是整场会话快照，而是“本次提交后下一步怎么走”的动作结果。
        SubmitAnswerResponse cplSubmitAnswerResponse = new SubmitAnswerResponse();
        cplSubmitAnswerResponse.setCurrentQuestionIndex(tblSavedInterviewSessionEntity.getCurrentQuestionIndex());
        cplSubmitAnswerResponse.setTotalQuestions(tblSavedInterviewSessionEntity.getTotalQuestions());

        boolean bolHasNextQuestion =
                cplSubmitAnswerResponse.getCurrentQuestionIndex() < cplSubmitAnswerResponse.getTotalQuestions();

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
     * 生成整场面试的规则版报告。
     * 当前先基于题目与答案生成统计、评分和总结，后续再升级为 AI 报告。
     */
    public InterviewReportDTO generateReport(String strSessionId) {
        log.info("开始生成面试报告: sessionId={}", strSessionId);
        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        if (!COMPLETED.equals(tblInterviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_COMPLETED, "面试尚未完成，无法生成报告");
        }

        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try{
            lstInterviewQuestionDTO = objectMapper.readValue(tblInterviewSessionEntity.getQuestionsJson(), new TypeReference<List<InterviewQuestionDTO>>() {
            });

        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        Integer intAnsweredQuestions = 0;
        Integer intTotalScore = 0;

        List<InterviewReportQuestionDTO> lstInterviewReportQuestionDTO = new ArrayList<>();

        for (InterviewQuestionDTO cplInterviewQuestionDTO : lstInterviewQuestionDTO) {
            String strUserAnswer = cplInterviewQuestionDTO.getUserAnswer(); // 用户答案
            boolean bolAnswered = strUserAnswer != null && !strUserAnswer.trim().isEmpty(); // 是否已回答

            if (bolAnswered) {
                intAnsweredQuestions++;
            }

            Integer intScore;
            String strEvaluation;

            if (!bolAnswered) {
                intScore = 0;
                strEvaluation = "未作答，建议补充该题答案。";
            } else if (strUserAnswer.trim().length() < 20) {
                intScore = 60;
                strEvaluation = "已作答，但答案较短，建议补充更多项目细节和技术实现。";
            } else if (strUserAnswer.trim().length() < 80) {
                intScore = 75;
                strEvaluation = "已作答，答案较完整，建议进一步加强表达的条理性和深度。";
            } else {
                intScore = 90;
                strEvaluation = "已作答，答案较完整，能够体现一定的项目经验和技术理解。";
            }

            intTotalScore += intScore;

            InterviewReportQuestionDTO cplInterviewReportQuestionDTO = new InterviewReportQuestionDTO();
            cplInterviewReportQuestionDTO.setQuestionIndex(cplInterviewQuestionDTO.getQuestionIndex()); // 题目索引
            cplInterviewReportQuestionDTO.setQuestion(cplInterviewQuestionDTO.getQuestion()); // 题目内容
            cplInterviewReportQuestionDTO.setCategory(cplInterviewQuestionDTO.getCategory()); // 题目分类
            cplInterviewReportQuestionDTO.setUserAnswer(strUserAnswer); // 用户答案
            cplInterviewReportQuestionDTO.setAnswered(bolAnswered); // 是否已回答
            cplInterviewReportQuestionDTO.setEvaluation(strEvaluation); // 单题点评
            cplInterviewReportQuestionDTO.setScore(intScore); // 单题分数

            lstInterviewReportQuestionDTO.add(cplInterviewReportQuestionDTO);
        }
        // 5. 统计整场面试的已答题数和未答题数。
        Integer intUnansweredQuestions =
                tblInterviewSessionEntity.getTotalQuestions() - intAnsweredQuestions;

        // 6. 生成整场面试的整体评价。
        String strOverallEvaluation;

        if (intAnsweredQuestions == 0) {
            strOverallEvaluation = "本次模拟面试尚未形成有效回答，建议先完成全部题目再查看报告。";
        } else if (intUnansweredQuestions > 0) {
            strOverallEvaluation = "本次模拟面试尚未全部完成，当前报告仅基于已回答题目生成，建议补全剩余题目。";
        } else {
            Integer intAverageScore = intTotalScore / tblInterviewSessionEntity.getTotalQuestions();

            if (intAverageScore >= 85) {
                strOverallEvaluation = "本次模拟面试完成度较高，整体回答较完整，能够体现较好的项目经验和技术表达能力。";
            } else if (intAverageScore >= 70) {
                strOverallEvaluation = "本次模拟面试整体表现较稳定，已具备一定的表达和技术基础，建议继续加强答案细节与深度。";
            } else {
                strOverallEvaluation = "本次模拟面试已完成，但部分答案仍较简略，建议结合实际项目进一步补充技术细节和解决思路。";
            }
        }

        // 7. 手动组装返回 DTO。
        InterviewReportDTO cplInterviewReportDTO = new InterviewReportDTO();
        cplInterviewReportDTO.setSessionId(tblInterviewSessionEntity.getSessionId()); // 会话ID
        cplInterviewReportDTO.setResumeId(tblInterviewSessionEntity.getResume().getId()); // 简历ID
        cplInterviewReportDTO.setTotalQuestions(tblInterviewSessionEntity.getTotalQuestions()); // 题目总数
        cplInterviewReportDTO.setAnsweredQuestions(intAnsweredQuestions); // 已回答题数
        cplInterviewReportDTO.setUnansweredQuestions(intUnansweredQuestions); // 未回答题数
        cplInterviewReportDTO.setCompleted(
                InterviewSessionStatus.COMPLETED.equals(tblInterviewSessionEntity.getStatus())
        ); // 是否已完成
        cplInterviewReportDTO.setOverallEvaluation(strOverallEvaluation); // 整体评价
        cplInterviewReportDTO.setQuestionReports(lstInterviewReportQuestionDTO); // 单题报告列表
        cplInterviewReportDTO.setGeneratedAt(LocalDateTime.now()); // 报告生成时间

        log.info("生成面试报告成功: sessionId={}, answeredQuestions={}, totalQuestions={}",
                strSessionId,
                intAnsweredQuestions,
                tblInterviewSessionEntity.getTotalQuestions());

        return cplInterviewReportDTO;

    }

    /**
     * 获取当前流程步骤应该展示的题目。
     * 这个接口面向“当前该答哪一题”，而不是返回整场会话的全量信息。
     */
    public CurrentQuestionResponseDTO getCurrentQuestion(String strSessionId) {

        log.info("开始获取当前面试题: sessionId={}", strSessionId);

        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(
                    tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {}
            );
        } catch (JsonProcessingException e) {
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
        InterviewQuestionDTO cplInterviewQuestionDTO =
                lstInterviewQuestionDTO.get(intCurrentQuestionIndex);

        cplCurrentQuestionResponseDTO.setCompleted(false);
        cplCurrentQuestionResponseDTO.setMessage(null);
        cplCurrentQuestionResponseDTO.setQuestion(cplInterviewQuestionDTO);

        log.info("获取当前面试题成功: sessionId={}, questionIndex={}",
                strSessionId, intCurrentQuestionIndex);

        return cplCurrentQuestionResponseDTO;
    }

    public void completeInterview(String strSessionId) {
        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(strSessionId);
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }
        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

    }
}
