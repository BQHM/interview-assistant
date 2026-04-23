package com.interview.modules.interview;

import com.interview.common.result.Result;
import com.interview.modules.interview.model.dto.CurrentQuestionResponseDTO;
import com.interview.modules.interview.model.dto.InterviewReportDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
import com.interview.modules.interview.model.dto.SubmitAnswerResponse;
import com.interview.modules.interview.model.request.CreateInterviewRequest;
import com.interview.modules.interview.model.request.SubmitAnswerRequest;
import com.interview.modules.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService interviewSessionService;

    /**
     * 创建面试会话。
     * 当前版本基于指定简历创建一场模拟面试。
     */
    @PostMapping
    public Result<InterviewSessionDTO> createInterview(
            @Valid @RequestBody CreateInterviewRequest cplCreateInterviewRequest) {

        InterviewSessionDTO cplInterviewSessionDTO =
                interviewSessionService.createInterview(cplCreateInterviewRequest);
        return Result.success(cplInterviewSessionDTO);
    }

    /**
     * 根据会话ID查询面试会话详情。
     */
    @GetMapping("/{sessionId}")
    public Result<InterviewSessionDTO> getInterviewSession(@PathVariable String sessionId) {
        InterviewSessionDTO cplInterviewSessionDTO =
                interviewSessionService.getInterviewSession(sessionId);
        return Result.success(cplInterviewSessionDTO);
    }

    /**
     * 提交当前面试题答案。
     * 当前返回的是提交动作结果，而不是整场会话快照。
     */
    @PostMapping("/answer")
    public Result<SubmitAnswerResponse> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest cplSubmitAnswerRequest) {

        SubmitAnswerResponse cplSubmitAnswerResponse =
                interviewSessionService.submitAnswer(cplSubmitAnswerRequest);
        return Result.success(cplSubmitAnswerResponse);
    }

    /**
     * 根据会话ID生成面试报告。
     */
    @GetMapping("/{sessionId}/report")
    public Result<InterviewReportDTO> getInterviewReport(@PathVariable String sessionId) {
        InterviewReportDTO cplInterviewReportDTO =
                interviewSessionService.generateReport(sessionId);
        return Result.success(cplInterviewReportDTO);
    }

    /**
     * 获取当前流程中应该展示给用户的题目。
     */
    @GetMapping("/{sessionId}/question")
    public Result<CurrentQuestionResponseDTO> getCurrentQuestion(@PathVariable String sessionId) {
        CurrentQuestionResponseDTO cplCurrentQuestionResponseDTO =
                interviewSessionService.getCurrentQuestion(sessionId);
        return Result.success(cplCurrentQuestionResponseDTO);
    }

    @PostMapping("/api/interviews/{sessionId}/complete")
    public Result<Void> completeInterview(String strSessionId){
        interviewSessionService.completeInterview(strSessionId);
        return Result.success(null);
    }

}
