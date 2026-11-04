package com.interview.infrastructure.ai;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件功能说明
 * <p>负责按名称创建并缓存 LLM 的 ChatClient。</p>
 *
 * @author NobuNo
 * @date 2026-11-04
 */
@Slf4j
@Component
public class LlmProviderRegistry {

    /**
     * Spring AI 会在 baseUrl 后面自动拼接该路径，配置的 baseUrl 不能再带 /v1。
     */
    private static final String VERSION_SUFFIX = "/v1";

    private final LlmProviderProperties properties;

    /**
     * ChatClient 内部持有 HTTP 客户端，创建成本高且线程安全，因此按名称缓存复用。
     */
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    /**
     * 功能说明
     * <p>注入 Provider 配置。</p>
     *
     * @param properties Provider 配置
     * @author NobuNo
     * @date 2026-11-04
     */
    public LlmProviderRegistry(LlmProviderProperties properties) {
        this.properties = properties;
    }

    /**
     * 功能说明
     * <p>启动时校验默认 Provider 是否存在，避免运行期才发现配置错误。</p>
     *
     * @author NobuNo
     * @date 2026-11-04
     */
    @PostConstruct
    void validateOnStartup() {
        if (getAvailableProviderIds().isEmpty()) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND, "未配置任何 LLM Provider，请检查 app.ai.llm.providers");
        }
        String defaultProviderId = getDefaultProviderId();
        if (!containsProvider(defaultProviderId)) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND,
                    "默认 Provider 不存在: " + defaultProviderId + "，当前可用: " + getAvailableProviderIds());
        }
        log.info("LLM Provider 已加载: {}，默认: {}", getAvailableProviderIds(), defaultProviderId);
    }

    /**
     * 功能说明
     * <p>按名称获取 ChatClient，名称为空时返回默认 Provider。</p>
     *
     * @param providerId Provider 名称，可为空
     * @return 对应的 ChatClient
     * @throws BusinessException 当名称不存在或配置不完整时抛出
     * @author NobuNo
     * @date 2026-11-04
     */
    public ChatClient getChatClient(String providerId) {
        String resolvedProviderId = resolveProviderId(providerId);
        // 同一个 Provider 只创建一次，之后直接复用缓存中的实例。
        return clientCache.computeIfAbsent(resolvedProviderId, this::createChatClient);
    }

    /**
     * 功能说明
     * <p>获取默认 Provider 的 ChatClient。</p>
     *
     * @return 默认 Provider 的 ChatClient
     * @throws BusinessException 当默认 Provider 配置不完整时抛出
     * @author NobuNo
     * @date 2026-11-04
     */
    public ChatClient getDefaultChatClient() {
        return getChatClient(null);
    }

    /**
     * 功能说明
     * <p>获取默认 Provider 名称，用于会话落库记录实际使用的模型。</p>
     *
     * @return 默认 Provider 名称
     * @author NobuNo
     * @date 2026-11-04
     */
    public String getDefaultProviderId() {
        return properties.getDefaultProvider();
    }

    /**
     * 功能说明
     * <p>获取全部可用 Provider 名称，按字典序排列。</p>
     *
     * @return 可用 Provider 名称集合
     * @author NobuNo
     * @date 2026-11-04
     */
    public Set<String> getAvailableProviderIds() {
        Map<String, LlmProviderProperties.ProviderConfig> providers = properties.getProviders();
        if (providers == null || providers.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(providers.keySet()));
    }

    /**
     * 功能说明
     * <p>判断指定 Provider 是否已配置。</p>
     *
     * @param providerId Provider 名称
     * @return 已配置返回 true
     * @author NobuNo
     * @date 2026-11-04
     */
    public boolean containsProvider(String providerId) {
        return providerId != null && !providerId.isBlank() && properties.getProviders() != null
                && properties.getProviders().containsKey(providerId);
    }

    /**
     * 功能说明
     * <p>名称缺失时回退到默认 Provider，明确指定时沿用指定值。</p>
     *
     * @param providerId 调用方传入的 Provider 名称
     * @return 实际使用的 Provider 名称
     * @author NobuNo
     * @date 2026-11-04
     */
    private String resolveProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return getDefaultProviderId();
        }
        return providerId;
    }

    /**
     * 功能说明
     * <p>根据配置创建 ChatClient，未知名称不静默回退到默认 Provider。</p>
     *
     * @param providerId Provider 名称
     * @return 新建的 ChatClient
     * @throws BusinessException 当名称不存在或缺少必要配置时抛出
     * @author NobuNo
     * @date 2026-11-04
     */
    private ChatClient createChatClient(String providerId) {
        LlmProviderProperties.ProviderConfig config = requireConfig(providerId);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(providerId, config.getBaseUrl()))
                .apiKey(config.getApiKey())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(config.getModel());
        // 温度未配置时不设置，交给框架默认值，避免强行覆盖。
        if (config.getTemperature() != null) {
            optionsBuilder.temperature(config.getTemperature());
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(optionsBuilder.build())
                .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
                .build();

        log.info("已创建 LLM ChatClient: provider={}, model={}", providerId, config.getModel());
        return ChatClient.builder(chatModel).build();
    }

    /**
     * 功能说明
     * <p>取出并校验 Provider 配置，缺少必要字段时直接失败。</p>
     *
     * @param providerId Provider 名称
     * @return 校验通过的 Provider 配置
     * @throws BusinessException 当名称不存在或必要字段缺失时抛出
     * @author NobuNo
     * @date 2026-11-04
     */
    private LlmProviderProperties.ProviderConfig requireConfig(String providerId) {
        LlmProviderProperties.ProviderConfig config = properties.getProviders() == null
                ? null
                : properties.getProviders().get(providerId);
        if (config == null) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND,
                    "未找到模型 " + providerId + "，当前可用: " + getAvailableProviderIds());
        }
        if (isBlank(config.getBaseUrl())) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND, "模型 " + providerId + " 缺少 base-url");
        }
        if (isBlank(config.getApiKey())) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND, "模型 " + providerId + " 缺少 api-key");
        }
        if (isBlank(config.getModel())) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND, "模型 " + providerId + " 缺少 model");
        }
        return config;
    }

    /**
     * 功能说明
     * <p>规范化 baseUrl，去掉尾部斜杠和多余 /v1，避免拼出 /v1/v1/chat/completions。</p>
     *
     * @param providerId Provider 名称，用于错误提示
     * @param rawBaseUrl 配置中的原始地址
     * @return 规范化后的地址
     * @throws BusinessException 当地址为空时抛出
     * @author NobuNo
     * @date 2026-11-04
     */
    private String normalizeBaseUrl(String providerId, String rawBaseUrl) {
        String normalized = rawBaseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.toLowerCase().endsWith(VERSION_SUFFIX)) {
            normalized = normalized.substring(0, normalized.length() - VERSION_SUFFIX.length());
        }
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.LLM_PROVIDER_NOT_FOUND, "模型 " + providerId + " 的 base-url 无效");
        }
        return normalized;
    }

    /**
     * 功能说明
     * <p>判断字符串是否为空或纯空白。</p>
     *
     * @param value 待判断字符串
     * @return 为空返回 true
     * @author NobuNo
     * @date 2026-11-04
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
