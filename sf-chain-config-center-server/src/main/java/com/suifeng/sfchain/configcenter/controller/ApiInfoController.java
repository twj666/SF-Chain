package com.suifeng.sfchain.configcenter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility endpoint for frontend API bootstrap config.
 */
@RestController
@RequestMapping("${sf-chain.path.api-prefix:/sf-chain}/config")
public class ApiInfoController {

    @Value("${sf-chain.path.api-prefix:/sf-chain}")
    private String apiPrefix;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @GetMapping("/api-info")
    public Map<String, Object> getApiInfo() {
        Map<String, Object> config = new HashMap<>();
        String baseUrl = contextPath == null ? "" : contextPath;

        config.put("baseUrl", baseUrl);
        config.put("contextPath", baseUrl);
        config.put("endpoints", Map.of(
                "AI_MODELS", apiPrefix + "/models",
                "AI_OPERATIONS", apiPrefix + "/operations",
                "AI_CALL_LOGS", apiPrefix + "/ai-logs",
                "AI_SYSTEM", apiPrefix + "/system"
        ));

        return config;
    }
}
