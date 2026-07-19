package com.interview.modules.interview.skill;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;
import com.interview.modules.interview.skill.model.InterviewSkillFrontMatterDefinition;
import com.interview.modules.interview.skill.model.InterviewSkillMetaDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件功能说明
 * <p>负责加载和查询面试方向配置。</p>
 *
 * @author NobuNo
 * @date 2026-07-17
 */
@Service
@RequiredArgsConstructor
public class InterviewSkillService {

    private static final Pattern SKILL_ID_PATTERN = Pattern.compile(".*/skills/([^/]+)/SKILL\\.md$");
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("(?s)^---\\s*\\n(.*?)\\n---\\s*\\n?(.*)$");
    private final ResourceLoader resourceLoader;
    private final Map<String, InterviewSkillDTO> skillMap = new TreeMap<>();

    /**
     * 功能说明
     * <p>查询全部面试方向配置。</p>
     *
     * @return 面试方向配置列表
     * @author NobuNo
     * @date 2026-07-17
     */
    public List<InterviewSkillDTO> getAllSkills() {
        return List.copyOf(skillMap.values());
    }

    /**
     * 功能说明
     * <p>根据面试方向编号查询配置。</p>
     *
     * @param skillId 面试方向编号
     * @return 面试方向配置
     * @throws BusinessException 当面试方向编号为空或不存在时抛出
     * @author NobuNo
     * @date 2026-07-17
     */
    public InterviewSkillDTO getSkill(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试方向编号不能为空");
        }

        InterviewSkillDTO skillDTO = skillMap.get(skillId);
        if (skillDTO == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "面试方向不存在: " + skillId);
        }

        return skillDTO;
    }

    /**
     * 功能说明
     * <p>扫描面试方向资源文件。</p>
     *
     * @throws IOException 当资源文件扫描失败时抛出
     * @author NobuNo
     * @date 2026-07-17
     */
    @PostConstruct
    public void loadSkills() throws IOException {
        // 创建资源解析器
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:skills/*/SKILL.md");

        // 创建 Yaml 对象
        Yaml yaml = new Yaml();

        // 遍历资源文件
        for (Resource resource : resources) {
            // 提取面试方向编号
            String skillId = extractSkillId(resource);
            if (skillId == null) {
                continue;
            }

            String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
            Matcher frontMatterMatcher = FRONT_MATTER_PATTERN.matcher(markdown);

            if (!frontMatterMatcher.matches()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 文件格式错误，缺少 front matter: " + resource.getDescription());
            }
            String frontMatter = frontMatterMatcher.group(1).trim();
            String persona = frontMatterMatcher.group(2).trim();

            InterviewSkillFrontMatterDefinition frontMatterDefinition = yaml.loadAs(frontMatter, InterviewSkillFrontMatterDefinition.class);
            if (frontMatterDefinition == null || frontMatterDefinition.getName() == null || frontMatterDefinition.getName().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 文件缺少 name: " + resource.getDescription());
            }

            InterviewSkillMetaDefinition metaDefinition = loadSkillMetaDefinition(skillId, yaml);

            if (metaDefinition.getDisplayName() == null || metaDefinition.getDisplayName().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 扩展配置缺少 displayName: " + skillId);
            }

            if (metaDefinition.getCategories() == null) {
                metaDefinition.setCategories(List.of());
            }

            InterviewSkillDTO skillDTO = new InterviewSkillDTO();
            skillDTO.setId(skillId);
            skillDTO.setName(metaDefinition.getDisplayName());
            skillDTO.setDescription(frontMatterDefinition.getDescription());
            skillDTO.setPersona(persona);
            skillDTO.setCategories(List.copyOf(metaDefinition.getCategories()));

            skillMap.put(skillId, skillDTO);
        }
    }

    /**
     * 功能说明
     * <p>从资源路径中提取面试方向编号。</p>
     *
     * @param resource Skill 资源文件
     * @return 面试方向编号，路径不匹配时返回 null
     * @throws IOException 当资源路径读取失败时抛出
     * @author NobuNo
     * @date 2026-07-17
     */
    private String extractSkillId(Resource resource) throws IOException {
        // 获取资源路径并规范化
        String normalizedPath = resource.getURL().toString().replace('\\', '/');

        // 使用正则表达式匹配面试方向编号
        Matcher matcher = SKILL_ID_PATTERN.matcher(normalizedPath);
        // 判断是否匹配成功
        if (!matcher.matches()) {
            return null;
        }
        // 返回匹配到的面试方向编号
        return matcher.group(1);
    }

    /**
     * 功能说明
     * <p>加载面试方向扩展配置。</p>
     *
     * @param skillId 面试方向编号
     * @param yaml    YAML 解析器
     * @return 面试方向扩展配置
     * @throws IOException       当配置文件读取失败时抛出
     * @throws BusinessException 当配置文件不存在或内容为空时抛出
     * @author NobuNo
     * @date 2026-07-17
     */
    private InterviewSkillMetaDefinition loadSkillMetaDefinition(String skillId, Yaml yaml) throws IOException {
        String location = "classpath:skills/" + skillId + "/skill.meta.yml";

        Resource metaResource = resourceLoader.getResource(location);
        if (!metaResource.exists()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 扩展配置不存在: " + location);
        }

        String metaContent = metaResource.getContentAsString(StandardCharsets.UTF_8);

        InterviewSkillMetaDefinition metaDefinition = yaml.loadAs(metaContent, InterviewSkillMetaDefinition.class);

        if (metaDefinition == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 扩展配置内容为空: " + location);
        }

        return metaDefinition;
    }
}
