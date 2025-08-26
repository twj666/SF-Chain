# SF-Chain Framework - 全局框架文档

## 项目概述

SF-Chain是一个现代化的AI调用框架，专为简化AI模型集成和操作管理而设计。它提供了统一的API接口、灵活的配置管理、强大的操作注册机制，以及完善的监控和持久化支持。

## 架构总览

```
sf-chain/
├── annotation/          # 注解定义 - 框架元数据
├── config/             # 配置管理 - Spring Boot自动配置
├── constants/          # 常量定义 - 统一的标准化常量
├── controller/         # Web端点 - RESTful API
├── core/               # 核心组件 - 业务逻辑核心
├── operations/         # 操作实现 - AI操作扩展
└── persistence/        # 数据持久化 - 配置和日志存储
```

## 快速开始

### 1. 基础集成

#### Maven依赖
```xml
<dependency>
    <groupId>com.suifeng</groupId>
    <artifactId>sf-chain-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 启用框架
```java
@SpringBootApplication
@EnableSfChain
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 配置文件
```yaml
sf-chain:
  models:
    openai:
      gpt-4:
        api-key: ${OPENAI_API_KEY}
        base-url: https://api.openai.com
        timeout: 30
      gpt-3.5-turbo:
        api-key: ${OPENAI_API_KEY}
        base-url: https://api.openai.com
        timeout: 30
  
  operations:
    json-repair:
      model: gpt-4
      max-tokens: 1000
      temperature: 0.3
    
    code-generation:
      model: gpt-4
      max-tokens: 2000
      temperature: 0.7
  
  persistence:
    type: mysql
    url: jdbc:mysql://localhost:3306/sf_chain
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 2. 基础使用示例

#### 执行AI操作
```java
@Service
public class AIService {
    
    @Autowired
    private AIService aiService;
    
    public String processText(String input) {
        return aiService.execute("text-generation", input);
    }
    
    public String repairJson(String brokenJson) {
        return aiService.execute("json-repair", brokenJson);
    }
    
    public String generateCode(String requirements) {
        return aiService.execute("code-generation", requirements);
    }
}
```

#### 自定义操作
```java
@SfChainOperation(
    value = "custom-analysis",
    displayName = "自定义分析",
    description = "执行自定义的AI分析任务",
    defaultModel = "gpt-4"
)
public class CustomAnalysisOperation extends BaseAIOperation<String, AnalysisResult> {
    
    @Override
    public AnalysisResult execute(String input) {
        // 自定义实现
        return new AnalysisResult(analyze(input));
    }
}
```

## 核心功能特性

### 1. 多模型支持
- **OpenAI**: GPT-4, GPT-3.5-turbo, DALL-E 3
- **Anthropic**: Claude 3
- **Google**: Gemini Pro
- **本地模型**: 支持自定义本地部署
- **扩展支持**: 易于添加新的模型提供商

### 2. 操作类型系统
预定义操作类型：
- **文本处理**: 生成、摘要、翻译、分类
- **代码相关**: 生成、审查、解释、重构
- **数据格式**: JSON修复、验证、XML处理
- **创意生成**: 写作、故事、诗歌
- **图像处理**: 生成、分析、编辑
- **文档处理**: 摘要、分析、翻译

### 3. 配置管理
#### 动态配置
```java
// 运行时更新配置
configManager.updateModelConfig("gpt-4", config -> {
    config.setMaxTokens(4000);
    config.setTemperature(0.5);
});

// 热加载配置
configManager.reloadConfiguration();
```

#### 配置验证
```java
@Validated
@ConfigurationProperties(prefix = "sf-chain")
public class SfChainProperties {
    
    @NotEmpty
    private Map<String, ModelConfig> models;
    
    @Valid
    private Map<String, OperationConfig> operations;
    
    // 自动验证和提示
}
```

### 4. 监控和度量
#### 内置监控端点
- **健康检查**: `/actuator/health`
- **指标**: `/actuator/metrics`
- **配置**: `/sf-chain/config`
- **统计**: `/sf-chain/stats`

#### 自定义指标
```java
@Component
public class CustomMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void recordOperation(String type, long duration) {
        meterRegistry.timer("sf.chain.operation.duration", "type", type)
            .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### 5. 持久化支持
#### 配置持久化
- **MySQL**: 生产环境推荐
- **PostgreSQL**: 企业级应用
- **内存存储**: 测试和开发

#### 数据模型
```sql
-- 模型配置表
CREATE TABLE sf_model_config (
    model_name VARCHAR(100) PRIMARY KEY,
    config_data JSON,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作配置表
CREATE TABLE sf_operation_config (
    operation_type VARCHAR(100) PRIMARY KEY,
    config_data JSON,
    model_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 调用日志表
CREATE TABLE sf_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(100),
    model_name VARCHAR(100),
    request_data TEXT,
    response_data TEXT,
    duration_ms BIGINT,
    success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 高级功能

### 1. 流式处理
```java
public Flux<String> streamGenerate(String prompt) {
    return aiService.streamExecute("text-generation", prompt)
        .map(chunk -> chunk.getContent())
        .onErrorContinue((error, item) -> {
            log.error("Stream error: {}", error.getMessage());
        });
}
```

### 2. 批量处理
```java
public List<String> batchProcess(List<String> inputs) {
    return aiService.batchExecute("text-processing", inputs, batchConfig -> {
        batchConfig.setBatchSize(10);
        batchConfig.setParallelism(3);
        batchConfig.setTimeout(Duration.ofMinutes(5));
    });
}
```

### 3. 缓存策略
#### 多级缓存
```java
@Cacheable(value = "ai-responses", key = "#operationType + ':' + #input")
public String executeWithCache(String operationType, String input) {
    return execute(operationType, input);
}

@CacheEvict(value = "ai-responses", allEntries = true)
public void clearCache() {
    // 清除缓存
}
```

### 4. 错误处理和重试
#### 重试策略
```java
@Retryable(
    value = {AIException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public String executeWithRetry(String operationType, String input) {
    return execute(operationType, input);
}

@Recover
public String recover(AIException e, String operationType, String input) {
    log.error("Failed to execute operation after retries: {}", operationType);
    return fallbackResponse(operationType, input);
}
```

### 5. 限流和熔断
#### 限流配置
```java
@RateLimiter(name = "ai-service", fallbackMethod = "rateLimitFallback")
public String executeWithRateLimit(String operationType, String input) {
    return execute(operationType, input);
}

@CircuitBreaker(name = "ai-service", fallbackMethod = "circuitBreakerFallback")
public String executeWithCircuitBreaker(String operationType, String input) {
    return execute(operationType, input);
}
```

## 扩展和定制

### 1. 添加新模型提供商
```java
public class CustomModelProvider implements AIModel {
    
    @Override
    public String generate(String prompt, ModelConfig config) {
        // 自定义实现
    }
    
    @Override
    public boolean isAvailable() {
        return checkHealth();
    }
}

// 注册新模型
@Configuration
public class CustomModelConfig {
    
    @Bean
    public AIModel customModel() {
        return new CustomModelProvider();
    }
}
```

### 2. 创建自定义操作
```java
@SfChainOperation(
    value = "sentiment-analysis",
    displayName = "情感分析",
    description = "分析文本的情感倾向",
    category = "text-analysis"
)
public class SentimentAnalysisOperation extends BaseAIOperation<String, SentimentResult> {
    
    @Override
    protected String buildPrompt(String input) {
        return "分析以下文本的情感倾向: " + input;
    }
    
    @Override
    protected SentimentResult parseResponse(String response) {
        return SentimentResult.fromJson(response);
    }
}
```

### 3. 插件系统
#### 插件接口定义
```java
public interface SfChainPlugin {
    String getName();
    void initialize(Map<String, Object> config);
    List<OperationType> getSupportedOperations();
}

// 插件实现
@Component
public class TranslationPlugin implements SfChainPlugin {
    
    @Override
    public String getName() {
        return "translation-plugin";
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        // 初始化插件
    }
    
    @Override
    public List<OperationType> getSupportedOperations() {
        return Arrays.asList(
            OperationType.TEXT_TRANSLATION,
            OperationType.DOCUMENT_TRANSLATION
        );
    }
}
```

## 性能优化

### 1. 连接池优化
```yaml
sf-chain:
  connection-pool:
    max-connections: 100
    max-per-route: 20
    connection-timeout: 5000
    socket-timeout: 30000
    keep-alive: 30000
```

### 2. 缓存优化
```yaml
sf-chain:
  cache:
    enabled: true
    type: caffeine
    specs:
      maximum-size: 1000
      expire-after-write: 1h
      record-stats: true
```

### 3. 异步处理
```java
@Async("aiTaskExecutor")\public CompletableFuture<String> asyncExecute(String operationType, String input) {
    return CompletableFuture.completedFuture(execute(operationType, input));
}

@Bean
public TaskExecutor aiTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("ai-task-");
    executor.initialize();
    return executor;
}
```

## 安全配置

### 1. API密钥管理
```yaml
sf-chain:
  security:
    api-key-header: X-API-Key
    rate-limit:
      enabled: true
      requests-per-minute: 100
    encryption:
      enabled: true
      algorithm: AES-256
```

### 2. 访问控制
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/sf-chain/**").hasRole("AI_USER")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

## 部署指南

### 1. Docker部署
```dockerfile
FROM openjdk:17-jre-slim

COPY target/sf-chain-app.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx2g -Xms1g"

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 2. Kubernetes部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sf-chain-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sf-chain
  template:
    metadata:
      labels:
        app: sf-chain
    spec:
      containers:
      - name: sf-chain
        image: sf-chain:latest
        ports:
        - containerPort: 8080
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secrets
              key: openai-key
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
```

### 3. 环境变量配置
```bash
# 数据库配置
export DB_URL=jdbc:mysql://localhost:3306/sf_chain
export DB_USERNAME=sf_user
export DB_PASSWORD=sf_password

# AI模型配置
export OPENAI_API_KEY=your-openai-key
export ANTHROPIC_API_KEY=your-anthropic-key

# 应用配置
export SF_CHAIN_PROFILE=production
export SF_CHAIN_LOG_LEVEL=INFO
```

## 故障排除

### 1. 常见问题

#### 连接超时
```yaml
# 增加超时时间
sf-chain:
  models:
    openai:
      timeout: 60
      max-retries: 5
```

#### 内存不足
```bash
# 增加JVM内存
java -Xmx4g -Xms2g -jar sf-chain-app.jar
```

#### 数据库连接问题
```yaml
# 检查数据库配置
sf-chain:
  persistence:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### 2. 调试工具

#### 日志配置
```yaml
logging:
  level:
    com.suifeng.sfchain: DEBUG
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 健康检查
```bash
# 检查应用状态
curl http://localhost:8080/actuator/health

# 检查配置
curl http://localhost:8080/sf-chain/config

# 检查统计
curl http://localhost:8080/sf-chain/stats
```

## 版本历史

### v1.0.0 (当前版本)
- ✅ 基础AI操作框架
- ✅ 多模型支持
- ✅ 配置管理
- ✅ 监控和度量
- ✅ 持久化支持
- ✅ Web端点
- ✅ 缓存机制
- ✅ 错误处理

### v1.1.0 (计划中)
- 🔄 流式处理优化
- 🔄 批量处理增强
- 🔄 插件系统完善
- 🔄 更多模型提供商

### v2.0.0 (未来版本)
- 🔮 分布式支持
- 🔮 机器学习集成
- 🔮 高级分析功能
- 🔮 企业级特性

## 社区和支持

### 1. 获取帮助
- 📖 **文档**: [SF-Chain文档](https://docs.sf-chain.com)
- 🐛 **问题**: [GitHub Issues](https://github.com/suifeng/sf-chain/issues)
- 💬 **讨论**: [GitHub Discussions](https://github.com/suifeng/sf-chain/discussions)
- 📧 **邮件**: support@sf-chain.com

### 2. 贡献指南
- 🎯 **功能请求**: 提交Issue
- 🔧 **Bug修复**: 创建Pull Request
- 📚 **文档**: 改进文档和示例
- 🧪 **测试**: 添加测试用例

### 3. 许可证
本项目采用Apache 2.0许可证，详见[LICENSE](LICENSE)文件。

## 致谢

感谢所有为SF-Chain框架做出贡献的开发者和用户。特别感谢以下项目：
- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAI API](https://openai.com)
- [Anthropic Claude](https://anthropic.com)
- [Google Gemini](https://ai.google.dev)

---

**SF-Chain Framework** - 让AI集成变得简单而强大

*最后更新: 2024-01-01*
*版本: 1.0.0*