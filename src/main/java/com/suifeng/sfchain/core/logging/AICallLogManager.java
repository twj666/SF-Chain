package com.suifeng.sfchain.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * AI调用日志管理器 - 基于LFU算法
 */
@Slf4j
@Component
public class AICallLogManager {
    
    private static final int MAX_CAPACITY = 100;
    
    /** 日志存储 */
    private final Map<String, AICallLog> logStorage = new ConcurrentHashMap<>();
    
    /** 频次计数器 */
    private final Map<String, Integer> frequencyMap = new ConcurrentHashMap<>();
    
    /** 频次分组 - 频次 -> 调用ID集合 */
    private final Map<Integer, LinkedHashSet<String>> frequencyGroups = new ConcurrentHashMap<>();
    
    /** 最小频次 */
    private volatile int minFrequency = 1;
    
    /** 读写锁 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 添加调用日志
     */
    public void addLog(AICallLog callLog) {
        lock.writeLock().lock();
        try {
            String callId = callLog.getCallId();
            
            // 如果已达到容量上限，移除最少使用的日志
            if (logStorage.size() >= MAX_CAPACITY && !logStorage.containsKey(callId)) {
                evictLFU();
            }
            
            // 添加或更新日志
            logStorage.put(callId, callLog);
            updateFrequency(callId);
            
            log.debug("添加AI调用日志: {}", callId);
    
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取完整调用日志（包含所有详细信息）
     */
    public AICallLog getFullLog(String callId) {
        lock.readLock().lock();
        try {
            AICallLog callLog = logStorage.get(callId);
            if (callLog != null) {
                // 更新访问时间和频次
                callLog.setLastAccessTime(LocalDateTime.now());
                updateFrequency(callId);
            }
            return callLog;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有日志摘要(按时间倒序)
     */
    public List<AICallLogSummary> getAllLogSummaries() {
        lock.readLock().lock();
        try {
            return logStorage.values().stream()
                    .sorted((a, b) -> b.getCallTime().compareTo(a.getCallTime()))
                    .map(AICallLogSummary::fromFullLog)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据操作类型获取日志摘要
     */
    public List<AICallLogSummary> getLogSummariesByOperation(String operationType) {
        lock.readLock().lock();
        try {
            return logStorage.values().stream()
                    .filter(log -> operationType.equals(log.getOperationType()))
                    .sorted((a, b) -> b.getCallTime().compareTo(a.getCallTime()))
                    .map(AICallLogSummary::fromFullLog)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据模型名称获取日志摘要
     */
    public List<AICallLogSummary> getLogSummariesByModel(String modelName) {
        lock.readLock().lock();
        try {
            return logStorage.values().stream()
                    .filter(log -> modelName.equals(log.getModelName()))
                    .sorted((a, b) -> b.getCallTime().compareTo(a.getCallTime()))
                    .map(AICallLogSummary::fromFullLog)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取统计信息
     */
    public LogStatistics getStatistics() {
        lock.readLock().lock();
        try {
            long totalCalls = logStorage.size();
            long successCalls = logStorage.values().stream()
                    .mapToLong(log -> log.getStatus() == AICallLog.CallStatus.SUCCESS ? 1 : 0)
                    .sum();
            
            double avgDuration = logStorage.values().stream()
                    .mapToLong(AICallLog::getDuration)
                    .average()
                    .orElse(0.0);
            
            Map<String, Long> operationCounts = logStorage.values().stream()
                    .collect(Collectors.groupingBy(
                            AICallLog::getOperationType,
                            Collectors.counting()
                    ));
            
            Map<String, Long> modelCounts = logStorage.values().stream()
                    .collect(Collectors.groupingBy(
                            AICallLog::getModelName,
                            Collectors.counting()
                    ));
            
            return LogStatistics.builder()
                    .totalCalls(totalCalls)
                    .successCalls(successCalls)
                    .successRate(totalCalls > 0 ? (double) successCalls / totalCalls : 0.0)
                    .averageDuration(avgDuration)
                    .operationCounts(operationCounts)
                    .modelCounts(modelCounts)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空所有日志
     */
    public void clearLogs() {
        lock.writeLock().lock();
        try {
            logStorage.clear();
            frequencyMap.clear();
            frequencyGroups.clear();
            minFrequency = 1;
            log.info("已清空所有AI调用日志");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 更新频次
     */
    private void updateFrequency(String callId) {
        int oldFreq = frequencyMap.getOrDefault(callId, 0);
        int newFreq = oldFreq + 1;
        
        frequencyMap.put(callId, newFreq);
        
        // 从旧频次组中移除
        if (oldFreq > 0) {
            frequencyGroups.get(oldFreq).remove(callId);
            if (frequencyGroups.get(oldFreq).isEmpty() && oldFreq == minFrequency) {
                minFrequency++;
            }
        }
        
        // 添加到新频次组
        frequencyGroups.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(callId);
        
        // 更新最小频次
        if (newFreq < minFrequency) {
            minFrequency = newFreq;
        }
    }
    
    /**
     * 淘汰最少使用的日志
     */
    private void evictLFU() {
        // 找到最小频次组中最早的元素
        LinkedHashSet<String> minFreqGroup = frequencyGroups.get(minFrequency);
        if (minFreqGroup != null && !minFreqGroup.isEmpty()) {
            String evictCallId = minFreqGroup.iterator().next();
            
            // 移除日志
            logStorage.remove(evictCallId);
            frequencyMap.remove(evictCallId);
            minFreqGroup.remove(evictCallId);
            
            log.debug("淘汰AI调用日志: {}", evictCallId);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class LogStatistics {
        private long totalCalls;
        private long successCalls;
        private double successRate;
        private double averageDuration;
        private Map<String, Long> operationCounts;
        private Map<String, Long> modelCounts;
    }
}