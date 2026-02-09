package com.suifeng.sfchain.configcenter.controller;

import com.suifeng.sfchain.configcenter.bootstrap.DatabaseBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${sf-chain.path.api-prefix:/sf-chain}/bootstrap/database")
public class DatabaseBootstrapController {

    private final DatabaseBootstrapService bootstrapService;

    @GetMapping("/status")
    public DatabaseBootstrapService.DatabaseStatus status() {
        return bootstrapService.status();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> test(@RequestBody DatabaseBootstrapService.DatabaseRequest request) {
        bootstrapService.testConnection(request);
        return ResponseEntity.ok(Map.of("message", "connection ok"));
    }

    @PostMapping("/init")
    public DatabaseBootstrapService.InitResult init(@RequestBody DatabaseBootstrapService.DatabaseRequest request) {
        return bootstrapService.initialize(request);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
