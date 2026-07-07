package com.interview.infrastructure.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 文件功能说明
 * <p>负责对象存储桶启动初始化。</p>
 *
 * @author NobuNo
 * @since 2026-07-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageBucketInitializer implements ApplicationRunner {

    private final FileStorageService fileStorageService;

    /**
     * 功能说明
     * <p>应用启动后检查并创建对象存储桶。</p>
     *
     * @param args 启动参数
     * @author NobuNo
     * @since 2026-07-03
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            fileStorageService.ensureBucketExists();
        } catch (Exception e) {
            log.warn("对象存储桶初始化失败，上传文件时可能失败", e);
        }
    }
}