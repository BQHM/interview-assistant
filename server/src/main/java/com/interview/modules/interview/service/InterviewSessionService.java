package com.interview.modules.interview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;

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

    public InterviewSessionDTO getInterviewSession(String strSessionId) {
        log.info("开始查询面试会话: sessionId={}", strSessionId);

        Optional<InterviewSessionEntity> optInterviewSessionEntity =
                interviewSessionRepository.findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试会话不存在");
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
}
