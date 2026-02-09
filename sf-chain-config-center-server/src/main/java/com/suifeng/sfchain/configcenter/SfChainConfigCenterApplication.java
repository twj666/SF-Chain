package com.suifeng.sfchain.configcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * SF-Chain 配置中心独立启动入口。
 */
@SpringBootApplication
@EntityScan(basePackages = {
        "com.suifeng.sfchain.persistence.entity",
        "com.suifeng.sfchain.configcenter.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.suifeng.sfchain.configcenter.repository"
})
public class SfChainConfigCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(SfChainConfigCenterApplication.class, args);
    }
}
