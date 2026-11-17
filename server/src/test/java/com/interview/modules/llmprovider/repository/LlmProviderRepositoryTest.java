package com.interview.modules.llmprovider.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.interview.modules.llmprovider.model.entity.LlmProviderEntity;

/**
 * 文件功能说明
 * <p>负责验证 LLM Provider 配置的数据访问方法名解析与查询语义。</p>
 *
 * @author NobuNo
 * @date 2026-11-12
 */
@SpringBootTest
@Transactional
class LlmProviderRepositoryTest {

    @Autowired
    private LlmProviderRepository llmProviderRepository;

    /**
     * 功能说明
     * <p>按唯一标识应能查到已保存的配置。</p>
     */
    @Test
    void findByProviderKey_shouldReturnSavedProvider() {
        llmProviderRepository.save(buildProvider("repo-find", true));

        LlmProviderEntity provider = llmProviderRepository.findByProviderKey("repo-find").orElseThrow();

        assertThat(provider.getModel()).isEqualTo("repo-find-model");
    }

    /**
     * 功能说明
     * <p>存在性判断应随配置保存而变化。</p>
     */
    @Test
    void existsByProviderKey_shouldReflectSavedState() {
        assertThat(llmProviderRepository.existsByProviderKey("repo-exists")).isFalse();

        llmProviderRepository.save(buildProvider("repo-exists", true));

        assertThat(llmProviderRepository.existsByProviderKey("repo-exists")).isTrue();
    }

    /**
     * 功能说明
     * <p>查询全部配置时应按唯一标识升序返回。该用例验证方法名里的 By 生效，排序没有被静默忽略。</p>
     */
    @Test
    void findAllByOrderByProviderKeyAsc_shouldSortByProviderKey() {
        llmProviderRepository.save(buildProvider("repo-sort-c", true));
        llmProviderRepository.save(buildProvider("repo-sort-a", true));
        llmProviderRepository.save(buildProvider("repo-sort-b", true));

        List<String> sortedKeys = llmProviderRepository.findAllByOrderByProviderKeyAsc().stream()
                .map(LlmProviderEntity::getProviderKey)
                .filter(key -> key.startsWith("repo-sort-"))
                .toList();

        assertThat(sortedKeys).containsExactly("repo-sort-a", "repo-sort-b", "repo-sort-c");
    }

    /**
     * 功能说明
     * <p>查询已启用配置时只应返回启用项，且允许多条并存。</p>
     */
    @Test
    void findByEnabledTrueOrderByProviderKeyAsc_shouldReturnAllEnabledProviders() {
        llmProviderRepository.save(buildProvider("repo-enabled-a", true));
        llmProviderRepository.save(buildProvider("repo-enabled-b", true));
        llmProviderRepository.save(buildProvider("repo-disabled-c", false));

        List<String> enabledKeys = llmProviderRepository.findByEnabledTrueOrderByProviderKeyAsc().stream()
                .map(LlmProviderEntity::getProviderKey)
                .filter(key -> key.startsWith("repo-enabled-") || key.startsWith("repo-disabled-"))
                .toList();

        assertThat(enabledKeys).containsExactly("repo-enabled-a", "repo-enabled-b");
    }

    /**
     * 功能说明
     * <p>构造一条满足非空约束的 Provider 配置。</p>
     *
     * @param providerKey Provider 唯一标识
     * @param enabled     是否启用
     * @return Provider 配置实体
     * @author NobuNo
     * @date 2026-11-12
     */
    private LlmProviderEntity buildProvider(String providerKey, boolean enabled) {
        LlmProviderEntity provider = new LlmProviderEntity();
        provider.setProviderKey(providerKey);
        provider.setDisplayName(providerKey);
        provider.setBaseUrl("http://localhost:9999");
        provider.setApiKeyCiphertext("ciphertext-placeholder");
        provider.setApiKeyNonce("nonce-placeholder");
        provider.setModel(providerKey + "-model");
        provider.setTemperature("0.20");
        provider.setEnabled(enabled);
        provider.setBuiltin(false);
        return provider;
    }
}
