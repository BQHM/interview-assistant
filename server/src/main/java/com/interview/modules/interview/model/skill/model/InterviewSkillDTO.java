package com.interview.modules.interview.model.skill.model;

import java.util.List;

/**
 * 文件功能说明
 * <p>负责承载面试方向信息。</p>
 *
 * @author NobuNo
 * @date 2026-07-17
 */
public record InterviewSkillDTO() {

    private static String id;// 面试方向ID
    private static String name;// 面试方向名称
    private static String description;// 面试方向描述
    private static String persona;// 面试方向对应角色
    private static List<InterviewSkillCategoryDTO> categories;// 面试方向分类列表
}
