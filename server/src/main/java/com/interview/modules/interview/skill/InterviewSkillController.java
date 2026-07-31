package com.interview.modules.interview.skill;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.common.result.Result;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;

import lombok.RequiredArgsConstructor;

/**
 * 文件功能说明
 * <p>负责面试方向查询接口。</p>
 *
 * @author NobuNo
 * @date 2026-07-31
 */
@RestController
@RequestMapping("/api/interview/skills")
@RequiredArgsConstructor
public class InterviewSkillController {

    private final InterviewSkillService interviewSkillService;

    /**
     * 功能说明
     * <p>查询全部面试方向。</p>
     *
     * @return 面试方向列表
     * @author NobuNo
     * @date 2026-07-31
     */
    @GetMapping
    public Result<List<InterviewSkillDTO>> getAllSkills() {
        List<InterviewSkillDTO> skillList = interviewSkillService.getAllSkills();
        return Result.success(skillList);
    }
}