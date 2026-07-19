package com.interview.modules.interview.skill.model;

import lombok.Data;

import java.util.List;

/**
 * 文件功能说明
 * <p>负责承载 Skill 扩展配置。</p>
 *
 * @author NobuNo
 * @date 2026-07-18
 */
@Data
public class InterviewSkillMetaDefinition {

    /** 面试方向显示名称 */
    private String displayName;

    /** 面试方向分类列表 */
    private List<InterviewSkillCategoryDTO> categories;
}
