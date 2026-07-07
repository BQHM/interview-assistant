package com.interview.modules.resume.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件功能说明
 * <p>负责简历分析配置绑定。</p>
 *
 * @author NobuNo
 * @since 2026-04-16
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.resume.analysis")
public class ResumeAnalysisProperties {

    private String systemPromptPath = "classpath:prompts/resume-analysis-system.st";
    private String userPromptPath = "classpath:prompts/resume-analysis-user.st";

}
