package com.interview.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件功能说明
 * <p>负责结构化输出重试配置绑定。</p>
 *
 * @author NobuNo
 * @date 2026-10-10
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class StructuredOutputProperties {

    private int structuredMaxAttempts = 2;
    private boolean structuredIncludeLastError = true;
    private boolean structuredRetryUseRepairPrompt = true;
    private boolean structuredRetryAppendStrictJsonInstruction = true;
    private int structuredErrorMessageMaxLength = 200;
}
