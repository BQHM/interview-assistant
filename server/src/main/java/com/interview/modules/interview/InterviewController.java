package com.interview.modules.interview;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.common.result.Result;
import com.interview.modules.interview.model.dto.CurrentQuestionResponseDTO;
import com.interview.modules.interview.model.dto.InterviewDetailDTO;
import com.interview.modules.interview.model.dto.InterviewReportDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
import com.interview.modules.interview.model.dto.InterviewSessionListItemDTO;
import com.interview.modules.interview.model.dto.SubmitAnswerResponse;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.interview.model.request.SaveAnswerRequest;
import com.interview.modules.interview.model.request.SubmitAnswerRequest;
import com.interview.modules.interview.service.InterviewHistoryService;
import com.interview.modules.interview.service.InterviewReportService;
import com.interview.modules.interview.service.InterviewSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 文件功能说明
 * <p>负责面试模块接口入口。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService interviewSessionService;
    private final InterviewHistoryService interviewHistoryService;
    private final InterviewReportService interviewReportService;

    /**
     * 功能说明
     * <p>创建面试会话。</p>
     *
     * @param cplCreateInterviewRequest 创建面试请求
     * @return 面试会话信息
     * @author NobuNo
     * @date 2026-04-20
     */
    @PostMapping
    public Result<InterviewSessionDTO> createInterview(
            @Valid @RequestBody CreateInterviewRequest cplCreateInterviewRequest) {

        InterviewSessionDTO cplInterviewSessionDTO = interviewSessionService.createInterview(cplCreateInterviewRequest);
        return Result.success(cplInterviewSessionDTO);
    }

    /**
     * 功能说明
     * <p>查询面试会话详情。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试会话信息
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping("/{sessionId}")
    public Result<InterviewSessionDTO> getInterviewSession(@PathVariable String sessionId) {
        InterviewSessionDTO cplInterviewSessionDTO = interviewSessionService.getInterviewSession(sessionId);
        return Result.success(cplInterviewSessionDTO);
    }

    /**
     * 功能说明
     * <p>提交面试答案。</p>
     *
     * @param cplSubmitAnswerRequest 提交答案请求
     * @return 提交答案结果
     * @author NobuNo
     * @date 2026-04-20
     */
    @PostMapping("/answer")
    public Result<SubmitAnswerResponse> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest cplSubmitAnswerRequest) {

        SubmitAnswerResponse cplSubmitAnswerResponse = interviewSessionService.submitAnswer(cplSubmitAnswerRequest);
        return Result.success(cplSubmitAnswerResponse);
    }

    /**
     * 功能说明
     * <p>获取面试报告。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试报告信息
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping("/{sessionId}/report")
    public Result<InterviewReportDTO> getInterviewReport(@PathVariable String sessionId) {
        InterviewReportDTO cplInterviewReportDTO = interviewReportService.generateReport(sessionId);
        return Result.success(cplInterviewReportDTO);
    }

    /**
     * 功能说明
     * <p>获取当前面试题。</p>
     *
     * @param sessionId 面试会话编号
     * @return 当前面试题信息
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping("/{sessionId}/question")
    public Result<CurrentQuestionResponseDTO> getCurrentQuestion(@PathVariable String sessionId) {
        CurrentQuestionResponseDTO cplCurrentQuestionResponseDTO = interviewSessionService
                .getCurrentQuestion(sessionId);
        return Result.success(cplCurrentQuestionResponseDTO);
    }

    /**
     * 功能说明
     * <p>提前完成面试。</p>
     *
     * @param strSessionId 面试会话编号
     * @return 空结果
     * @author NobuNo
     * @date 2026-04-20
     */
    @PostMapping("/{sessionId}/complete")
    public Result<Void> completeInterview(@PathVariable("sessionId") String strSessionId) {
        interviewSessionService.completeInterview(strSessionId);
        return Result.success(null);
    }

    /**
     * 功能说明
     * <p>查询未完成面试会话。</p>
     *
     * @param lngResumeId 简历编号
     * @return 未完成面试会话信息
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping("/unfinished/{resumeId}")
    public Result<InterviewSessionDTO> findUnfinishedSessionByResumeId(@PathVariable("resumeId") Long lngResumeId) {
        InterviewSessionDTO cplInterviewSessionDTO = interviewSessionService
                .findUnfinishedSessionByResumeId(lngResumeId);
        return Result.success(cplInterviewSessionDTO);
    }

    /**
     * 功能说明
     * <p>暂存面试答案。</p>
     *
     * @param strSessionId 面试会话编号
     * @param cplSaveAnswerRequest 暂存答案请求
     * @return 空结果
     * @author NobuNo
     * @date 2026-04-20
     */
    @PutMapping("/{sessionId}/answers")
    public Result<Void> saveAnswer(@PathVariable("sessionId") String strSessionId,
            @Valid @RequestBody SaveAnswerRequest cplSaveAnswerRequest) {
        interviewSessionService.saveAnswer(strSessionId, cplSaveAnswerRequest);
        return Result.success(null);
    }

    /**
     * 功能说明
     * <p>查询面试历史列表。</p>
     *
     * @return 面试历史列表
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping
    public Result<List<InterviewSessionListItemDTO>> getHistory() {
        List<InterviewSessionListItemDTO> interviewSessionList = interviewHistoryService.getHistory();
        return Result.success(interviewSessionList);
    }

    /**
     * 功能说明
     * <p>查询面试历史详情。</p>
     *
     * @param sessionId 面试会话编号
     * @return 面试历史详情
     * @author NobuNo
     * @date 2026-04-20
     */
    @GetMapping("/{sessionId}/details")
    public Result<InterviewDetailDTO> getInterviewDetail(@PathVariable String sessionId) {
        InterviewDetailDTO interviewDetail = interviewHistoryService.getInterviewDetail(sessionId);
        return Result.success(interviewDetail);
    }

    /**
     * 功能说明
     * <p>删除面试会话。</p>
     *
     * @param sessionId 面试会话编号
     * @return 空结果
     * @author NobuNo
     * @date 2026-04-20
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteInterview(@PathVariable String sessionId) {
        interviewHistoryService.deleteInterview(sessionId);
        return Result.success(null);
    }
}
