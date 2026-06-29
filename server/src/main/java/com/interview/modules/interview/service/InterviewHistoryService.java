package com.interview.modules.interview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.dto.InterviewDetailDTO;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;
import com.interview.modules.interview.model.entity.InterviewSessionEntity;
import com.interview.modules.interview.repository.InterviewAnswerRepository;
import com.interview.modules.interview.repository.InterviewSessionRepository;
import com.interview.modules.interview.service.comm.InterviewQuestionAnswerAggregator;
import com.interview.modules.interview.service.convert.InterviewSessionConverter;
import com.interview.modules.resume.repository.ResumeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;



@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewHistoryService {

    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;
    private final InterviewAnswerRepository interviewAnswerRepository;

    /**
     * 查询面试历史列表。
     * 当前只组装列表页需要的摘要字段，避免把 questionsJson 暴露给列表接口。
     */
    public List<InterviewSessionListItemDTO> getHistory() {

        // 按创建时间倒序查询所有面试会话
        List<InterviewSessionEntity> lstInterviewSessionEntity = interviewSessionRepository
                .findAllByOrderByCreatedAtDesc();

        List<InterviewSessionListItemDTO> lstInterviewSessionListItemDTO = new ArrayList<>();

        for (InterviewSessionEntity tblInterviewSessionEntity : lstInterviewSessionEntity) {
            InterviewSessionListItemDTO cplInterviewSessionListItemDTO = InterviewSessionConverter
                    .convertToInterviewSessionListItemDTO(tblInterviewSessionEntity);
            lstInterviewSessionListItemDTO.add(cplInterviewSessionListItemDTO);
        }

        return lstInterviewSessionListItemDTO;
    }

        /**
     * 查询面试历史详情。
     * questionsJson 保存题目快照，interview_answers 保存用户答案。
     * 这里会把题目和答案按 questionIndex 聚合成详情返回。
     */
    public InterviewDetailDTO getInterviewDetail(String strSessionId) {
        // 根据会话ID查询面试会话
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        // 获取面试会话
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

        List<InterviewAnswerEntity> lstInterviewAnswerEntity = interviewAnswerRepository
                .findBySessionOrderByQuestionIndexAsc(tblInterviewSessionEntity);
        InterviewQuestionAnswerAggregator.fillUserAnswers(lstInterviewQuestionDTO, lstInterviewAnswerEntity);

        // 输出转换
        InterviewDetailDTO cplInterviewDetailDTO = InterviewSessionConverter.convertToInterviewDetailDTO(
                tblInterviewSessionEntity,
                lstInterviewQuestionDTO);
        return cplInterviewDetailDTO;
    }

        /**
     * 删除面试会话。
     * 当前需要先删除 interview_answers 中的答案记录，再删除 interview_sessions 中的会话记录。
     */
    @Transactional
    public void deleteInterview(String strSessionId) {
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(strSessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INTERVIEW_SESSION_NOT_FOUND,
                    "面试会话不存在");
        }

        InterviewSessionEntity tblInterviewSessionEntity = optInterviewSessionEntity.get();

        // 删除答案表中的所有记录
        interviewAnswerRepository.deleteBySession(tblInterviewSessionEntity);

        // 删除会话记录
        interviewSessionRepository.delete(tblInterviewSessionEntity);

        log.info("删除面试会话成功: sessionId={}", strSessionId);
    }
    
}
