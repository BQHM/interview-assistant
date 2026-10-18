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

/**
 * 文件功能说明
 * <p>负责面试历史查询和删除业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-06-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewHistoryService {

    private final ResumeRepository resumeRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;
    private final InterviewAnswerRepository interviewAnswerRepository;

    /**
     * 功能说明
     * <p>查询面试历史列表。</p>
     *
     * @return 面试历史列表
     * @author NobuNo
     * @date 2026-06-29
     */
    public List<InterviewSessionListItemDTO> getHistory() {

        // 按创建时间倒序查询所有面试会话
        List<InterviewSessionEntity> interviewSessionEntities = interviewSessionRepository
                .findAllByOrderByCreatedAtDesc();

        List<InterviewSessionListItemDTO> interviewSessionListItemDTOs = new ArrayList<>();

        for (InterviewSessionEntity interviewSessionEntity : interviewSessionEntities) {
            InterviewSessionListItemDTO interviewSessionListItemDTO = InterviewSessionConverter
                    .convertToInterviewSessionListItemDTO(interviewSessionEntity);
            interviewSessionListItemDTOs.add(interviewSessionListItemDTO);
        }

        return interviewSessionListItemDTOs;
    }

    /**
     * 功能说明
     * <p>查询面试历史详情。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试历史详情
     * @author NobuNo
     * @date 2026-06-29
     */
    public InterviewDetailDTO getInterviewDetail(String sessionId) {
        // 根据会话ID查询面试会话
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND, "面试会话不存在");
        }

        // 获取面试会话
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

        List<InterviewAnswerEntity> interviewAnswerEntities = interviewAnswerRepository
                .findBySessionOrderByQuestionIndexAsc(interviewSessionEntity);
        InterviewQuestionAnswerAggregator.fillUserAnswers(interviewQuestionDTOs, interviewAnswerEntities);

        // 输出转换
        InterviewDetailDTO interviewDetailDTO = InterviewSessionConverter.convertToInterviewDetailDTO(
                interviewSessionEntity,
                interviewQuestionDTOs);
        return interviewDetailDTO;
    }

    /**
     * 功能说明
     * <p>删除面试会话。</p>
     *
     * @param sessionId 面试会话编号
     * @author NobuNo
     * @date 2026-06-29
     */
    @Transactional
    public void deleteInterview(String sessionId) {
        Optional<InterviewSessionEntity> optInterviewSessionEntity = interviewSessionRepository
                .findBySessionId(sessionId);

        if (optInterviewSessionEntity.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INTERVIEW_SESSION_NOT_FOUND,
                    "面试会话不存在");
        }

        InterviewSessionEntity interviewSessionEntity = optInterviewSessionEntity.get();

        // 删除答案表中的所有记录
        interviewAnswerRepository.deleteBySession(interviewSessionEntity);

        // 删除会话记录
        interviewSessionRepository.delete(interviewSessionEntity);

        log.info("删除面试会话成功: sessionId={}", sessionId);
    }
}
