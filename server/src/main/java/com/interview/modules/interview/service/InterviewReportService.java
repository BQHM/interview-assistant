package com.interview.modules.interview.service;

import static com.interview.modules.interview.model.InterviewSessionStatus.COMPLETED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewReportDTO;
import com.interview.modules.interview.model.dto.InterviewReportQuestionDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.repository.InterviewAnswerRepository;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.interview.service.comm.InterviewQuestionAnswerAggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 文件功能说明
 * <p>负责面试报告生成业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-06-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReportService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final ObjectMapper objectMapper;

    /**
     * 功能说明
     * <p>生成面试报告。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试报告信息
     * @author NobuNo
     * @date 2026-06-29
     */
    @Transactional(readOnly = true)
    public InterviewReportDTO generateReport(String sessionId) {
        log.info("开始生成面试报告: sessionId={}", sessionId);
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);

        // 检查面试会话是否存在
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        // 获取面试会话实体
        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        // 检查状态是否为已完成
        if (!COMPLETED.equals(interviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_COMPLETED, "面试尚未完成，无法生成报告");
        }

        // 解析面试题目 JSON 字符串
        List<InterviewQuestionDTO> interviewQuestionDTOs;
        try {
            interviewQuestionDTOs = objectMapper.readValue(interviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 获取某场面试下的全部答案
        List<InterviewAnswerEntity> interviewAnswerEntities = interviewAnswerRepository.findBySessionOrderByQuestionIndexAsc(interviewSessionEntity);

        // 填充用户答案
        InterviewQuestionAnswerAggregator.fillUserAnswers(interviewQuestionDTOs, interviewAnswerEntities);

        Integer answeredQuestions = 0;// 已回答题目数量
        Integer totalScore = 0; // 总分数

        List<InterviewReportQuestionDTO> interviewReportQuestionDTOs = new ArrayList<>();

        // 遍历所有题目，填充已回答题目数量、总分数和单题报告
        for (InterviewQuestionDTO interviewQuestionDTO : interviewQuestionDTOs) {
            String userAnswer = interviewQuestionDTO.getUserAnswer(); // 用户答案
            boolean bolAnswered = userAnswer != null && !userAnswer.trim().isEmpty(); // 是否已回答

            if (bolAnswered) {
                answeredQuestions++;// 已回答题目数量增加
            }

            // 获取对应的答案记录
            InterviewAnswerEntity matchedAnswerEntity = findAnswerByQuestionIndex(interviewAnswerEntities, interviewQuestionDTO.getQuestionIndex());

            Integer score;// 单题分数
            String evaluation;// 单题点评

            // 处理未作答情况
            if (!bolAnswered) {
                score = 0;
                evaluation = "未作答，建议补充该题答案。";
            } else if (matchedAnswerEntity != null && matchedAnswerEntity.getScore() != null) {
                // 已生成评分
                score = matchedAnswerEntity.getScore();
                // 处理点评
                if (matchedAnswerEntity.getFeedback() == null || matchedAnswerEntity.getFeedback().trim().isEmpty()) {
                    evaluation = "已生成评分，但暂无详细点评。";
                } else {
                    evaluation = matchedAnswerEntity.getFeedback();
                }
            } else {
                // 未生成评分
                score = 60;
                evaluation = "已作答，但暂未生成评估结果，建议稍后重新查看报告。";
            }

            totalScore += score;// 总分数增加

            InterviewReportQuestionDTO interviewReportQuestionDTO = new InterviewReportQuestionDTO();
            interviewReportQuestionDTO.setQuestionIndex(interviewQuestionDTO.getQuestionIndex()); // 题目索引
            interviewReportQuestionDTO.setQuestion(interviewQuestionDTO.getQuestion()); // 题目内容
            interviewReportQuestionDTO.setCategory(interviewQuestionDTO.getCategory()); // 题目分类
            interviewReportQuestionDTO.setUserAnswer(userAnswer); // 用户答案
            interviewReportQuestionDTO.setAnswered(bolAnswered); // 是否已回答
            interviewReportQuestionDTO.setEvaluation(evaluation); // 单题点评
            interviewReportQuestionDTO.setScore(score); // 单题分数
            interviewReportQuestionDTO.setReferenceAnswer(null);
            interviewReportQuestionDTO.setKeyPoints(List.of());
            if (matchedAnswerEntity != null) {
                interviewReportQuestionDTO.setReferenceAnswer(matchedAnswerEntity.getReferenceAnswer());
                interviewReportQuestionDTO.setKeyPoints(parseKeyPoints(matchedAnswerEntity.getKeyPointsJson()));
            }
            interviewReportQuestionDTOs.add(interviewReportQuestionDTO);
        }

        // 5. 统计整场面试的已答题数和未答题数。
        Integer unansweredQuestions = interviewSessionEntity.getTotalQuestions() - answeredQuestions;

        // 6. 生成整场面试的整体评价。
        String overallEvaluation;

        // 处理未作答情况
        if (answeredQuestions == 0) {
            overallEvaluation = "本次模拟面试尚未形成有效回答，建议先完成全部题目再查看报告。";
        } else if (unansweredQuestions > 0) {
            overallEvaluation = "本次模拟面试尚未全部完成，当前报告仅基于已回答题目生成，建议补全剩余题目。";
        } else {
            Integer averageScore = totalScore / interviewSessionEntity.getTotalQuestions();

            if (averageScore >= 85) {
                overallEvaluation = "本次模拟面试完成度较高，整体回答较完整，能够体现较好的项目经验和技术表达能力。";
            } else if (averageScore >= 70) {
                overallEvaluation = "本次模拟面试整体表现较稳定，已具备一定的表达和技术基础，建议继续加强答案细节与深度。";
            } else {
                overallEvaluation = "本次模拟面试已完成，但部分答案仍较简略，建议结合实际项目进一步补充技术细节和解决思路。";
            }
        }

        // 7. 手动组装返回 DTO。
        InterviewReportDTO interviewReportDTO = new InterviewReportDTO();
        interviewReportDTO.setSessionId(interviewSessionEntity.getSessionId()); // 会话ID
        interviewReportDTO.setResumeId(interviewSessionEntity.getResume().getId()); // 简历ID
        interviewReportDTO.setTotalQuestions(interviewSessionEntity.getTotalQuestions()); // 题目总数
        interviewReportDTO.setAnsweredQuestions(answeredQuestions); // 已回答题数
        interviewReportDTO.setUnansweredQuestions(unansweredQuestions); // 未回答题数
        interviewReportDTO.setCompleted(COMPLETED.equals(interviewSessionEntity.getStatus())); // 是否已完成
        interviewReportDTO.setOverallEvaluation(overallEvaluation); // 整体评价
        interviewReportDTO.setQuestionReports(interviewReportQuestionDTOs); // 单题报告列表
        interviewReportDTO.setGeneratedAt(LocalDateTime.now()); // 报告生成时间

        log.info("生成面试报告成功: sessionId={}, answeredQuestions={}, totalQuestions={}", sessionId, answeredQuestions, interviewSessionEntity.getTotalQuestions());
        return interviewReportDTO;

    }

    /**
     * 功能说明
     * <p>根据题目索引查找答案记录。</p>
     *
     * @param interviewAnswerEntities 答案记录列表
     * @param questionIndex 题目索引
     * @return 答案记录
     * @author NobuNo
     * @date 2026-06-29
     */
    private InterviewAnswerEntity findAnswerByQuestionIndex(List<InterviewAnswerEntity> interviewAnswerEntities, Integer questionIndex) {

        for (InterviewAnswerEntity interviewAnswerEntity : interviewAnswerEntities) {
            if (questionIndex.equals(interviewAnswerEntity.getQuestionIndex())) {
                return interviewAnswerEntity;
            }
        }

        return null;
    }

    /**
     * 功能说明
     * <p>解析答案关键点。</p>
     *
     * @param keyPointsJson 关键点 JSON 字符串
     * @return 关键点列表
     * @author NobuNo
     * @date 2026-06-29
     */
    private List<String> parseKeyPoints(String keyPointsJson) {
        if (keyPointsJson == null || keyPointsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(keyPointsJson, new TypeReference<List<String>>() {
            });
        } catch (JacksonException e) {
            log.warn("面试答案关键点反序列化失败: keyPointsJson={}", keyPointsJson, e);
            return List.of();
        }
    }
}
