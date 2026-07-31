package com.interview.modules.interview.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.skill.model.InterviewSkillCategoryDTO;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;

/**
 * 面试方向配置服务测试。
 */
class InterviewSkillServiceTest {

    private InterviewSkillService interviewSkillService;

    @BeforeEach
    void setUp() throws IOException {
        interviewSkillService = new InterviewSkillService(new DefaultResourceLoader());
        interviewSkillService.loadSkills();
    }

    @Test
    void getSkill_shouldLoadJavaBackendSkill() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("java-backend");

        assertThat(skillDTO.getId()).isEqualTo("java-backend");
        assertThat(skillDTO.getName()).isEqualTo("Java 后端");
        assertThat(skillDTO.getDescription()).contains("Java 后端");
        assertThat(skillDTO.getPersona()).contains("技术面试官");
        assertThat(skillDTO.getCategories())
                .extracting(InterviewSkillCategoryDTO::getKey)
                .containsExactly("JAVA", "SPRING_BOOT", "MYSQL", "REDIS", "PROJECT");
    }

    @Test
    void getSkill_shouldLoadSystemDesignSkill() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("system-design");

        assertThat(skillDTO.getId()).isEqualTo("system-design");
        assertThat(skillDTO.getName()).isEqualTo("系统设计");
        assertThat(skillDTO.getDescription()).contains("系统设计");
        assertThat(skillDTO.getPersona()).contains("系统设计技术面试官");
        assertThat(skillDTO.getCategories())
                .extracting(InterviewSkillCategoryDTO::getKey)
                .containsExactly(
                        "SYSTEM_DESIGN_SCENARIO",
                        "HIGH_AVAILABILITY",
                        "DISTRIBUTED",
                        "CACHE",
                        "DB_DESIGN");
    }

    @Test
    void getAllSkills_shouldReturnLoadedSkills() {
        List<InterviewSkillDTO> skillDTOList = interviewSkillService.getAllSkills();

        assertThat(skillDTOList).isNotEmpty();
        assertThat(skillDTOList)
                .extracting(InterviewSkillDTO::getId)
                .contains("java-backend", "system-design");
    }

    @Test
    void getSkill_shouldThrowWhenSkillNotFound() {
        BusinessException exception = catchThrowableOfType(
                () -> interviewSkillService.getSkill("unknown"),
                BusinessException.class);

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).contains("面试方向不存在: unknown");
    }

    @Test
    void calculateAllocation_shouldPrioritizeAlwaysOneAndCoreCategories() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("java-backend");

        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(
                skillDTO.getCategories(),
                3);

        assertThat(allocation).containsExactly(
                entry("PROJECT", 1),
                entry("JAVA", 1),
                entry("SPRING_BOOT", 1));
    }

    @Test
    void calculateAllocation_shouldDistributeRemainingQuestionsInRounds() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("java-backend");

        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(
                skillDTO.getCategories(),
                8);

        assertThat(allocation).containsExactly(
                entry("PROJECT", 1),
                entry("JAVA", 2),
                entry("SPRING_BOOT", 2),
                entry("MYSQL", 2),
                entry("REDIS", 1));
        int allocatedQuestionCount = allocation.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        assertThat(allocatedQuestionCount).isEqualTo(8);
    }

    @Test
    void calculateAllocation_shouldPrioritizeSystemDesignScenarioAndCoreCategories() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("system-design");

        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(
                skillDTO.getCategories(),
                3);

        assertThat(allocation).containsExactly(
                entry("SYSTEM_DESIGN_SCENARIO", 1),
                entry("HIGH_AVAILABILITY", 1),
                entry("DISTRIBUTED", 1));
    }

    @Test
    void calculateAllocation_shouldTreatNullPriorityAsNormal() {
        List<InterviewSkillCategoryDTO> categories = List.of(
                createCategory("PROJECT", "项目经验", "ALWAYS_ONE"),
                createCategory("GENERAL", "综合能力", null));

        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(categories, 3);

        assertThat(allocation).containsExactly(
                entry("PROJECT", 1),
                entry("GENERAL", 2));
    }

    @Test
    void calculateAllocation_shouldReturnEmptyMapForInvalidArguments() {
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill("java-backend");

        assertThat(interviewSkillService.calculateAllocation(null, 3)).isEmpty();
        assertThat(interviewSkillService.calculateAllocation(skillDTO.getCategories(), null)).isEmpty();
        assertThat(interviewSkillService.calculateAllocation(skillDTO.getCategories(), 0)).isEmpty();
    }

    private InterviewSkillCategoryDTO createCategory(
            String key,
            String label,
            String priority
    ) {
        InterviewSkillCategoryDTO categoryDTO = new InterviewSkillCategoryDTO();
        categoryDTO.setKey(key);
        categoryDTO.setLabel(label);
        categoryDTO.setPriority(priority);
        return categoryDTO;
    }
}
