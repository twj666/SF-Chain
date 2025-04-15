package com.sfchain.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 描述: SFChain Spring配置类
 * @author suifeng
 * 日期: 2025/4/15
 */
@Configuration
@ComponentScan(basePackages = {
        "com.sfchain.core",
        "com.sfchain.spring",
        "com.sfchain.models",
        "com.sfchain.operations"
})
public class SFChainConfiguration {
    // 确保扫描所有相关包
}