package com.interview.modules.interview.skill.model;

import lombok.Data;
/**
 * 文件功能说明
 * <p>负责承载面试方向分类信息。</p>
 *
 * @author NobuNo
 * @date 2026-07-17
 */
@Data
public class InterviewSkillCategoryDTO {

    /** 分类标识 */
    private String key;

    /** 分类名称 */
    private String label;

    /** 分类优先级 */
    private String priority;
}
