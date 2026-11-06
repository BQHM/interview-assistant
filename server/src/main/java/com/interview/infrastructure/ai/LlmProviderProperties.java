package com.interview.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件功能说明
 * <p>负责 LLM Provider 注册表配置绑定。</p>
 *
 * @author NobuNo
 * @date 2026-11-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai.llm")
public class LlmProviderProperties {

    // 建立连接的超时时间（秒），网络不通时快速失败
    private int connectTimeoutSeconds = 10;

    // 读取响应的超时时间（秒），简历分析这类长任务需要放宽
    private int readTimeoutSeconds = 120;

    // 不指定 Provider 时使用的缺省值，必须是 providers 里的某个 key
    private String defaultProvider = "qwen";

    // 模型清单：key 是 Provider 名称（如 qwen、deepseek），value 是该模型的连接配置
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    /**
     * 单个 Provider 的连接配置。
     */
    @Data
    public static class ProviderConfig {

        // 服务地址根路径，不包含 /v1，Spring AI 会自动拼接 /v1/chat/completions
        private String baseUrl;

        // 访问密钥，从 .env 注入，禁止写死在代码里
        private String apiKey;

        // 模型名称，例如 qwen3.8-omni-flash、deepseek-chat
        private String model;

        // 采样温度，越小输出越稳定；为空时使用框架默认值
        private Double temperature;
    }
}