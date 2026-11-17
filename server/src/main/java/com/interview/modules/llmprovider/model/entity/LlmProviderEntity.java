package com.interview.modules.llmprovider.model.entity;

import com.interview.common.annotation.FieldMeta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件功能说明
 * <p>负责 LLM Provider 配置的持久化映射。</p>
 *
 * @author NobuNo
 * @date 2026-11-12
 */
@Entity
@Table(name = "llm_providers")
@Getter
@Setter
public class LlmProviderEntity {

    private static final String DEFAULT_TEMPERATURE = "0.20";

    /**
     * 配置主键
     * Provider 配置记录主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldMeta(name = "配置主键", desc = "Provider 配置记录主键")
    private Long id;

    /**
     * Provider 唯一标识
     * 程序内引用名，如 qwen，历史会话按该名称关联
     */
    @Column(name = "provider_key", nullable = false, length = 64, unique = true)
    @FieldMeta(name = "Provider 唯一标识", desc = "程序内引用名，如 qwen，历史会话按该名称关联")
    private String providerKey;

    /**
     * 显示名称
     * 展示名称，为空时前端回退显示 providerKey
     */
    @Column(name = "display_name", length = 100)
    @FieldMeta(name = "显示名称", desc = "展示名称，为空时前端回退显示 providerKey")
    private String displayName;

    /**
     * 服务地址
     * 不含结尾的 /v1，注册表负责补全路径
     */
    @Column(name = "base_url", nullable = false, length = 512)
    @FieldMeta(name = "服务地址", desc = "不含结尾的 /v1，注册表负责补全路径")
    private String baseUrl;

    /**
     * API Key 密文
     * AES-GCM 加密后的密文，Base64 编码，禁止存明文
     */
    @Column(name = "api_key_ciphertext", nullable = false, length = 4096)
    @FieldMeta(name = "API Key 密文", desc = "AES-GCM 加密后的密文，Base64 编码")
    private String apiKeyCiphertext;

    /**
     * 加密随机数
     * AES-GCM 每次加密使用的 nonce，Base64 编码，与密文成对使用
     */
    @Column(name = "api_key_nonce", nullable = false, length = 64)
    @FieldMeta(name = "加密随机数", desc = "AES-GCM 每次加密使用的 nonce，与密文成对使用")
    private String apiKeyNonce;

    /**
     * 对话模型名称
     * 如 qwen-plus，传给 OpenAI 兼容接口
     */
    @Column(nullable = false, length = 128)
    @FieldMeta(name = "对话模型名称", desc = "如 qwen-plus，传给 OpenAI 兼容接口")
    private String model;

    /**
     * 向量模型名称
     * 知识库检索使用，可为空
     */
    @Column(name = "embedding_model", length = 128)
    @FieldMeta(name = "向量模型名称", desc = "知识库检索使用，可为空")
    private String embeddingModel;

    /**
     * 向量维度
     * 与向量模型匹配，可为空
     */
    @Column(name = "embedding_dimensions")
    @FieldMeta(name = "向量维度", desc = "与向量模型匹配，可为空")
    private Integer embeddingDimensions;

    /**
     * 是否支持向量化
     * false 时不能用于知识库
     */
    @Column(name = "supports_embedding", nullable = false)
    @FieldMeta(name = "是否支持向量化", desc = "false 时不能用于知识库")
    private boolean supportsEmbedding;

    /**
     * 采样温度
     * 字符串存储如 0.20，取值 0 到 2，读取时由应用转成 double
     */
    @Column(nullable = false, length = 10)
    @FieldMeta(name = "采样温度", desc = "字符串存储如 0.20，取值 0 到 2")
    private String temperature;

    /**
     * 是否启用
     * 停用而不删除，保留历史会话的可读性
     */
    @Column(nullable = false)
    @FieldMeta(name = "是否启用", desc = "停用而不删除，保留历史会话的可读性")
    private boolean enabled;

    /**
     * 是否内置
     * 跟随应用初始化导入的配置，不允许普通用户删除
     */
    @Column(nullable = false)
    @FieldMeta(name = "是否内置", desc = "跟随应用初始化导入的配置，不允许普通用户删除")
    private boolean builtin;

    /**
     * 创建时间
     * 配置创建时间
     */
    @Column(name = "created_at", nullable = false)
    @FieldMeta(name = "创建时间", desc = "配置创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 配置最后更新时间
     */
    @Column(name = "updated_at", nullable = false)
    @FieldMeta(name = "更新时间", desc = "配置最后更新时间")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (temperature == null) {
            temperature = DEFAULT_TEMPERATURE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
