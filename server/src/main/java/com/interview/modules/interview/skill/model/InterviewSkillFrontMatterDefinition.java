package com.interview.modules.interview.skill.model;

import lombok.Data;

/**
 * 文件功能说明
 * <p>负责承载 Skill 文件头部配置。</p>
 *
 * @author NobuNo
 * @date 2026-07-18
 */
@Data
public class InterviewSkillFrontMatterDefinition {

    /** 面试方向名称 */
    private String name;

    /** 面试方向描述 */
    private String description;
}