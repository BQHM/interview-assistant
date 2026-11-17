package com.interview.modules.llmprovider.repository;

import com.interview.modules.llmprovider.model.entity.LlmProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件功能说明
 * <p>负责 LLM Provider 配置数据访问。</p>
 *
 * @author NobuNo
 * @date 2026-11-12
 */
@Repository
public interface LlmProviderRepository extends JpaRepository<LlmProviderEntity, Long> {

    /**
     * 功能说明
     * <p>按 Provider 唯一标识查询配置。</p>
     *
     * @param providerKey Provider 唯一标识
     * @return Provider 配置实体
     * @author NobuNo
     * @date 2026-11-12
     */
    Optional<LlmProviderEntity> findByProviderKey(String providerKey);

    /**
     * 功能说明
     * <p>判断 Provider 唯一标识是否已存在。</p>
     *
     * @param providerKey Provider 唯一标识
     * @return 是否存在
     * @author NobuNo
     * @date 2026-11-12
     */
    boolean existsByProviderKey(String providerKey);

    /**
     * 功能说明
     * <p>查询全部 Provider 配置，按唯一标识升序。</p>
     *
     * @return Provider 配置列表
     * @author NobuNo
     * @date 2026-11-12
     */
    List<LlmProviderEntity> findAllByOrderByProviderKeyAsc();

    /**
     * 功能说明
     * <p>查询已启用的 Provider 配置，按唯一标识升序。</p>
     *
     * @return 已启用的 Provider 配置列表
     * @author NobuNo
     * @date 2026-11-12
     */
    List<LlmProviderEntity> findByEnabledTrueOrderByProviderKeyAsc();
}
