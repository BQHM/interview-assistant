package com.interview.modules.resume.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 简历分析相关配置，负责绑定提示词模板路径等参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.resume.analysis")
public class ResumeAnalysisProperties {

    private String systemPromptPath = "classpath:prompts/resume-analysis-system.st";
    private String userPromptPath = "classpath:prompts/resume-analysis-user.st";
}
