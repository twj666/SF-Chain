package com.suifeng.sfchain.controller;

import com.suifeng.sfchain.core.logging.AICallLog;
import com.suifeng.sfchain.core.logging.AICallLogManager;
import com.suifeng.sfchain.core.logging.AICallLogSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * AI调用日志查询控制器
 */
@RestController
@RequestMapping("/sf-chain/ai-logs")
public class AICallLogController {
    
    @Resource
    private AICallLogManager logManager;
    
    /**
     * 获取所有日志摘要（轻量级）
     */
    @GetMapping
    public ResponseEntity<List<AICallLogSummary>> getAllLogSummaries() {
        return ResponseEntity.ok(logManager.getAllLogSummaries());
    }
    
    /**
     * 根据调用ID获取完整日志详情
     */
    @GetMapping("/{callId}")
    public ResponseEntity<AICallLog> getFullLog(@PathVariable String callId) {
        AICallLog log = logManager.getFullLog(callId);
        return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
    }
    
    /**
     * 根据操作类型获取日志摘要（轻量级）
     */
    @GetMapping("/operation/{operationType}")
    public ResponseEntity<List<AICallLogSummary>> getLogSummariesByOperation(@PathVariable String operationType) {
        return ResponseEntity.ok(logManager.getLogSummariesByOperation(operationType));
    }
    
    /**
     * 根据模型名称获取日志摘要（轻量级）
     */
    @GetMapping("/model/{modelName}")
    public ResponseEntity<List<AICallLogSummary>> getLogSummariesByModel(@PathVariable String modelName) {
        return ResponseEntity.ok(logManager.getLogSummariesByModel(modelName));
    }
    
    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<AICallLogManager.LogStatistics> getStatistics() {
        return ResponseEntity.ok(logManager.getStatistics());
    }
    
    /**
     * 清空所有日志
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearLogs() {
        logManager.clearLogs();
        return ResponseEntity.ok(Map.of("message", "所有日志已清空"));
    }
}