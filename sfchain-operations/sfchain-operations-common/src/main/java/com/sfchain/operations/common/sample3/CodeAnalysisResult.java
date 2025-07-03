package com.sfchain.operations.common.sample3;

/**
 * 代码分析结果实体类
 * 包含结构化的分析数据和原始分析文本
 */
public class CodeAnalysisResult {
    private String rawAnalysis;           // 原始AI分析文本
    private boolean parseSuccess;         // 是否成功解析结构化数据
    private String coreFunction;          // 核心功能描述
    private String keyMethods;            // 关键方法列表
    private String criticalCode;          // 关键代码片段
    private String architecturePattern;   // 架构模式
    private String dependencies;          // 依赖关系
    private String businessValue;         // 业务价值
    private long analysisTime;            // 分析耗时（毫秒）
    private String fileName;              // 文件名
    private String packagePath;           // 包路径

    // 构造函数
    public CodeAnalysisResult() {}

    // Getters and Setters
    public String getRawAnalysis() { return rawAnalysis; }
    public void setRawAnalysis(String rawAnalysis) { this.rawAnalysis = rawAnalysis; }
    
    public boolean isParseSuccess() { return parseSuccess; }
    public void setParseSuccess(boolean parseSuccess) { this.parseSuccess = parseSuccess; }
    
    public String getCoreFunction() { return coreFunction; }
    public void setCoreFunction(String coreFunction) { this.coreFunction = coreFunction; }
    
    public String getKeyMethods() { return keyMethods; }
    public void setKeyMethods(String keyMethods) { this.keyMethods = keyMethods; }
    
    public String getCriticalCode() { return criticalCode; }
    public void setCriticalCode(String criticalCode) { this.criticalCode = criticalCode; }
    
    public String getArchitecturePattern() { return architecturePattern; }
    public void setArchitecturePattern(String architecturePattern) { this.architecturePattern = architecturePattern; }
    
    public String getDependencies() { return dependencies; }
    public void setDependencies(String dependencies) { this.dependencies = dependencies; }
    
    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }
    
    public long getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(long analysisTime) { this.analysisTime = analysisTime; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getPackagePath() { return packagePath; }
    public void setPackagePath(String packagePath) { this.packagePath = packagePath; }

    /**
     * 获取格式化的分析摘要
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== ").append(fileName != null ? fileName : "Unknown").append(" ===\n");
        
        if (packagePath != null && !packagePath.isEmpty()) {
            sb.append("包路径: ").append(packagePath).append("\n");
        }
        
        if (coreFunction != null && !coreFunction.isEmpty()) {
            sb.append("核心功能: ").append(coreFunction).append("\n");
        }
        
        if (keyMethods != null && !keyMethods.isEmpty()) {
            sb.append("关键方法:\n").append(keyMethods).append("\n");
        }
        
        if (architecturePattern != null && !architecturePattern.isEmpty()) {
            sb.append("设计模式: ").append(architecturePattern).append("\n");
        }
        
        if (dependencies != null && !dependencies.isEmpty()) {
            sb.append("依赖关系: ").append(dependencies).append("\n");
        }
        
        if (businessValue != null && !businessValue.isEmpty()) {
            sb.append("价值: ").append(businessValue).append("\n");
        }
        
        if (criticalCode != null && !criticalCode.isEmpty()) {
            sb.append("关键代码:\n").append(criticalCode).append("\n");
        }
        
        if (analysisTime > 0) {
            sb.append("分析耗时: ").append(analysisTime).append("ms\n");
        }
        
        sb.append("\n");
        
        return sb.toString();
    }
}