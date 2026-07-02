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

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReportService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final ObjectMapper objectMapper;

    /**
     * 生成整场面试的规则版报告。
     * 当前先基于题目与答案生成统计、评分和总结，后续再升级为 AI 报告。
     */
    @Transactional(readOnly = true)
    public InterviewReportDTO generateReport(String strSessionId) {
        log.info("开始生成面试报告: sessionId={}", strSessionId);
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        // 检查面试会话是否存在
        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        // 获取面试会话实体
        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        // 检查状态是否为已完成
        if (!COMPLETED.equals(tblInterviewSessionEntity.getStatus())) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_COMPLETED, "面试尚未完成，无法生成报告");
        }

        // 解析面试题目 JSON 字符串
        List<InterviewQuestionDTO> lstInterviewQuestionDTO;
        try {
            lstInterviewQuestionDTO = objectMapper.readValue(tblInterviewSessionEntity.getQuestionsJson(),
                    new TypeReference<List<InterviewQuestionDTO>>() {
                    });
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试题目反序列化失败");
        }

        // 获取某场面试下的全部答案
        List<InterviewAnswerEntity> lstInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionOrderByQuestionIndexAsc(tblInterviewSessionEntity);
        InterviewQuestionAnswerAggregator.fillUserAnswers(lstInterviewQuestionDTO, lstInterviewAnswerEntity);

        Integer intAnsweredQuestions = 0;// 已回答题目数量
        Integer intTotalScore = 0; // 总分数

        List<InterviewReportQuestionDTO> lstInterviewReportQuestionDTO = new ArrayList<>();

        for (InterviewQuestionDTO cplInterviewQuestionDTO : lstInterviewQuestionDTO) {
            String strUserAnswer = cplInterviewQuestionDTO.getUserAnswer(); // 用户答案
            boolean bolAnswered = strUserAnswer != null && !strUserAnswer.trim().isEmpty(); // 是否已回答

            if (bolAnswered) {
                intAnsweredQuestions++;// 已回答题目数量增加
            }

            // 获取对应的答案记录
            InterviewAnswerEntity tblMatchedAnswerEntity = findAnswerByQuestionIndex(
                    lstInterviewAnswerEntity,
                    cplInterviewQuestionDTO.getQuestionIndex());

            Integer intScore;// 单题分数
            String strEvaluation;// 单题点评

            // 处理未作答情况
            if (!bolAnswered) {
                intScore = 0;
                strEvaluation = "未作答，建议补充该题答案。";
            } else if (tblMatchedAnswerEntity != null && tblMatchedAnswerEntity.getScore() != null) {
                // 已生成评分
                intScore = tblMatchedAnswerEntity.getScore();
                // 处理点评
                if (tblMatchedAnswerEntity.getFeedback() == null
                        || tblMatchedAnswerEntity.getFeedback().trim().isEmpty()) {
                    strEvaluation = "已生成评分，但暂无详细点评。";
                } else {
                    strEvaluation = tblMatchedAnswerEntity.getFeedback();
                }
            } else {
                // 未生成评分
                intScore = 60;
                strEvaluation = "已作答，但暂未生成评估结果，建议稍后重新查看报告。";
            }

            intTotalScore += intScore;// 总分数增加

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
        Integer intUnansweredQuestions = tblInterviewSessionEntity.getTotalQuestions() - intAnsweredQuestions;

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
                InterviewSessionStatus.COMPLETED.equals(tblInterviewSessionEntity.getStatus())); // 是否已完成
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
     * 根据题目索引查找对应的答案记录。
     */
    private InterviewAnswerEntity findAnswerByQuestionIndex(
            List<InterviewAnswerEntity> lstInterviewAnswerEntity,
            Integer intQuestionIndex) {

        for (InterviewAnswerEntity tblInterviewAnswerEntity : lstInterviewAnswerEntity) {
            if (intQuestionIndex.equals(tblInterviewAnswerEntity.getQuestionIndex())) {
                return tblInterviewAnswerEntity;
            }
        }

        return null;
    }
}
