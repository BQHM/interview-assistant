package com.interview.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data // 自动生成 Getter/Setter
@Configuration // 标识为配置类
@ConfigurationProperties(prefix = "app.storage") // 关键：对应 YAML 里的层级
public class StorageConfigProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "us-east-1";
}