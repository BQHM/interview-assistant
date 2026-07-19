package com.interview.modules.interview.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.util.List;

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
    void getAllSkills_shouldReturnLoadedSkills() {
        List<InterviewSkillDTO> skillDTOList = interviewSkillService.getAllSkills();

        assertThat(skillDTOList).isNotEmpty();
        assertThat(skillDTOList)
                .extracting(InterviewSkillDTO::getId)
                .contains("java-backend");
    }

    @Test
    void getSkill_shouldThrowWhenSkillNotFound() {
        BusinessException exception = catchThrowableOfType(
                () -> interviewSkillService.getSkill("unknown"),
                BusinessException.class);

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).contains("面试方向不存在: unknown");
    }
}
