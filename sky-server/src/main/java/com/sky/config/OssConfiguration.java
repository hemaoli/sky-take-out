package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类用于创建AliOssUtil对象
 */
@Configuration
@Slf4j
public class OssConfiguration {

    @Autowired
    private AliOssProperties aliOssProperties;

    @Bean
    public AliOssUtil aliOssUtil() {
        log.info("开始创建阿里云文件上传工具对象:");
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
