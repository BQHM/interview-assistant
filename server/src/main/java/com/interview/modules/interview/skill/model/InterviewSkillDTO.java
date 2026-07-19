package com.interview.modules.interview.skill.model;

import lombok.Data;

import java.util.List;

/**
 * 文件功能说明
 * <p>负责承载面试方向信息。</p>
 *
 * @author NobuNo
 * @date 2026-07-17
 */
@Data
public class InterviewSkillDTO {

    /** 面试方向编号 */
    private String id;

    /** 面试方向名称 */
    private String name;

    /** 面试方向描述 */
    private String description;

    /** 面试官角色说明 */
    private String persona;

    /** 面试方向分类列表 */
    private List<InterviewSkillCategoryDTO> categories;
}
