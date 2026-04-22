package com.interview.modules.interview;

import com.interview.common.result.Result;
import com.interview.modules.interview.model.dto.InterviewReportDTO;
import com.interview.modules.interview.model.dto.InterviewSessionDTO;
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
     */
    @PostMapping("/answer")
    public Result<InterviewSessionDTO> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest cplSubmitAnswerRequest) {

        InterviewSessionDTO cplInterviewSessionDTO =
                interviewSessionService.submitAnswer(cplSubmitAnswerRequest);
        return Result.success(cplInterviewSessionDTO);
    }

    @GetMapping("/{sessionId}/report")
    public Result<InterviewReportDTO> getInterviewReport(@PathVariable String sessionId) {
        InterviewReportDTO cplInterviewReportDTO =
                interviewSessionService.generateReport(sessionId);
        return Result.success(cplInterviewReportDTO);
    }
}
