# SF-Chain AI调度框架重构设计文档

## 1. 项目概述

### 1.1 重构目标
- **模块化设计**: 将框架拆分为独立的、可复用的模块
- **高扩展性**: 支持插件化架构，便于第三方扩展
- **高可用性**: 提供容错、降级、监控等企业级特性
- **标准化**: 遵循开源框架最佳实践和设计模式
- **云原生**: 支持容器化部署和微服务架构

### 1.2 当前架构问题分析

#### 1.2.1 耦合度过高
- `AIService`承担过多职责（执行、统计、会话管理）
- `PersistenceManager`直接依赖具体实现类
- 配置管理分散在多个类中

#### 1.2.2 扩展性不足
- 硬编码的OpenAI模型工厂
- 操作注册机制不够灵活
- 缺乏统一的插件接口

#### 1.2.3 可观测性缺失
- 缺乏统一的监控指标
- 日志记录不规范
- 缺乏分布式追踪支持

#### 1.2.4 配置管理混乱
- 配置分散在多个地方
- 缺乏配置验证和热更新
- 环境隔离不清晰

## 2. 新架构设计

### 2.1 整体架构图

```
sf-chain-framework/
├── sf-chain-core/                    # 核心模块
│   ├── sf-chain-api/                 # API定义
│   ├── sf-chain-engine/              # 执行引擎
│   ├── sf-chain-registry/            # 注册中心
│   └── sf-chain-context/             # 上下文管理
├── sf-chain-providers/               # 模型提供商
│   ├── sf-chain-openai/              # OpenAI适配器
│   ├── sf-chain-anthropic/           # Anthropic适配器
│   ├── sf-chain-google/              # Google适配器
│   └── sf-chain-provider-spi/        # 提供商SPI
├── sf-chain-operations/              # 操作模块
│   ├── sf-chain-operation-spi/       # 操作SPI
│   ├── sf-chain-text-operations/     # 文本操作
│   ├── sf-chain-code-operations/     # 代码操作
│   └── sf-chain-json-operations/     # JSON操作
├── sf-chain-persistence/             # 持久化模块
│   ├── sf-chain-persistence-api/     # 持久化API
│   ├── sf-chain-persistence-jpa/     # JPA实现
│   ├── sf-chain-persistence-redis/   # Redis实现
│   └── sf-chain-persistence-memory/  # 内存实现
├── sf-chain-config/                  # 配置管理
│   ├── sf-chain-config-api/          # 配置API
│   ├── sf-chain-config-spring/       # Spring配置
│   └── sf-chain-config-nacos/        # Nacos配置
├── sf-chain-observability/           # 可观测性
│   ├── sf-chain-metrics/             # 指标收集
│   ├── sf-chain-tracing/             # 链路追踪
│   └── sf-chain-logging/             # 日志管理
├── sf-chain-web/                     # Web模块
│   ├── sf-chain-rest-api/            # REST API
│   ├── sf-chain-admin-ui/            # 管理界面
│   └── sf-chain-websocket/           # WebSocket支持
├── sf-chain-security/                # 安全模块
│   ├── sf-chain-auth/                # 认证授权
│   ├── sf-chain-rate-limit/          # 限流
│   └── sf-chain-encryption/          # 加密
├── sf-chain-spring-boot-starter/     # Spring Boot启动器
└── sf-chain-examples/                # 示例项目
```

### 2.2 核心设计原则

#### 2.2.1 单一职责原则
每个模块只负责一个特定的功能领域

#### 2.2.2 依赖倒置原则
高层模块不依赖低层模块，都依赖抽象

#### 2.2.3 开闭原则
对扩展开放，对修改关闭

#### 2.2.4 接口隔离原则
客户端不应该依赖它不需要的接口

## 3. 详细模块设计

### 3.1 sf-chain-core 核心模块

#### 3.1.1 sf-chain-api - API定义模块

**职责**: 定义框架的核心接口和数据模型

```java
// 核心接口定义
public interface AIProvider {
    String getName();
    List<String> getSupportedModels();
    AIModel createModel(String modelName, Map<String, Object> config);
    boolean isAvailable();
}

public interface AIOperation<I, O> {
    String getOperationType();
    Class<I> getInputType();
    Class<O> getOutputType();
    O execute(I input, ExecutionContext context);
    Flux<String> executeStream(I input, ExecutionContext context);
    OperationMetadata getMetadata();
}

public interface ExecutionEngine {
    <I, O> O execute(String operationType, I input, ExecutionOptions options);
    <I, O> CompletableFuture<O> executeAsync(String operationType, I input, ExecutionOptions options);
    <I> Flux<String> executeStream(String operationType, I input, ExecutionOptions options);
}
```

**关键特性**:
- 定义统一的接口规范
- 提供扩展点和SPI接口
- 包含核心数据模型和异常定义

#### 3.1.2 sf-chain-engine - 执行引擎模块

**职责**: 实现AI操作的执行逻辑和生命周期管理

```java
@Component
public class DefaultExecutionEngine implements ExecutionEngine {
    
    private final OperationRegistry operationRegistry;
    private final ProviderRegistry providerRegistry;
    private final ExecutionInterceptorChain interceptorChain;
    private final CircuitBreakerManager circuitBreakerManager;
    
    @Override
    public <I, O> O execute(String operationType, I input, ExecutionOptions options) {
        ExecutionContext context = createExecutionContext(operationType, input, options);
        
        return interceptorChain.execute(context, () -> {
            AIOperation<I, O> operation = operationRegistry.getOperation(operationType);
            return circuitBreakerManager.execute(operationType, () -> 
                operation.execute(input, context)
            );
        });
    }
}
```

**关键特性**:
- 支持拦截器链模式
- 集成熔断器和重试机制
- 提供执行上下文管理
- 支持批量和流式执行

#### 3.1.3 sf-chain-registry - 注册中心模块

**职责**: 管理操作和提供商的注册与发现

```java
public interface OperationRegistry {
    void registerOperation(AIOperation<?, ?> operation);
    <I, O> AIOperation<I, O> getOperation(String operationType);
    List<String> getAvailableOperations();
    OperationMetadata getOperationMetadata(String operationType);
}

public interface ProviderRegistry {
    void registerProvider(AIProvider provider);
    AIProvider getProvider(String providerName);
    List<String> getAvailableProviders();
    AIModel getModel(String modelName);
}
```

**关键特性**:
- 支持动态注册和注销
- 提供健康检查机制
- 支持优先级和负载均衡
- 集成配置管理

### 3.2 sf-chain-providers 模型提供商模块

#### 3.2.1 sf-chain-provider-spi - 提供商SPI

**职责**: 定义模型提供商的标准接口

```java
public interface ModelProvider {
    String getProviderName();
    List<ModelCapability> getSupportedCapabilities();
    AIModel createModel(ModelConfig config);
    HealthStatus getHealthStatus();
}

public interface AIModel {
    String getModelName();
    ModelCapability getCapability();
    CompletableFuture<String> generate(String prompt, GenerationOptions options);
    Flux<String> generateStream(String prompt, GenerationOptions options);
    boolean isAvailable();
}
```

#### 3.2.2 具体提供商实现

每个提供商作为独立模块，实现标准SPI接口：

```java
@Component
public class OpenAIProvider implements ModelProvider {
    
    @Override
    public String getProviderName() {
        return "openai";
    }
    
    @Override
    public AIModel createModel(ModelConfig config) {
        return new OpenAIModel(config);
    }
}
```

### 3.3 sf-chain-operations 操作模块

#### 3.3.1 sf-chain-operation-spi - 操作SPI

```java
public abstract class AbstractAIOperation<I, O> implements AIOperation<I, O> {
    
    protected abstract String buildPrompt(I input, ExecutionContext context);
    protected abstract O parseResponse(String response, I input);
    
    @Override
    public final O execute(I input, ExecutionContext context) {
        String prompt = buildPrompt(input, context);
        AIModel model = context.getModel();
        String response = model.generate(prompt, context.getGenerationOptions()).join();
        return parseResponse(response, input);
    }
}
```

#### 3.3.2 具体操作实现

```java
@Operation(type = "json-repair", description = "修复损坏的JSON")
public class JsonRepairOperation extends AbstractAIOperation<String, String> {
    
    @Override
    protected String buildPrompt(String input, ExecutionContext context) {
        return "请修复以下JSON: " + input;
    }
    
    @Override
    protected String parseResponse(String response, String input) {
        return extractJsonFromResponse(response);
    }
}
```

### 3.4 sf-chain-config 配置管理模块

#### 3.4.1 统一配置接口

```java
public interface ConfigurationManager {
    <T> T getConfig(String key, Class<T> type);
    <T> T getConfig(String key, Class<T> type, T defaultValue);
    void setConfig(String key, Object value);
    void addConfigChangeListener(String key, ConfigChangeListener listener);
    Map<String, Object> getAllConfigs();
}

public interface ConfigurationSource {
    Map<String, Object> loadConfigs();
    void saveConfigs(Map<String, Object> configs);
    boolean supportsHotReload();
}
```

#### 3.4.2 配置结构设计

```yaml
sf-chain:
  # 核心配置
  core:
    execution:
      timeout: 30s
      retry-count: 3
      circuit-breaker:
        enabled: true
        failure-threshold: 5
        recovery-timeout: 30s
    
  # 提供商配置
  providers:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com/v1
      models:
        gpt-4:
          max-tokens: 4096
          temperature: 0.7
        gpt-3.5-turbo:
          max-tokens: 2048
          temperature: 0.5
    
  # 操作配置
  operations:
    json-repair:
      provider: openai
      model: gpt-4
      enabled: true
      timeout: 15s
    
  # 持久化配置
  persistence:
    type: jpa
    datasource:
      url: jdbc:mysql://localhost:3306/sf_chain
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
    
  # 可观测性配置
  observability:
    metrics:
      enabled: true
      export-interval: 30s
    tracing:
      enabled: true
      sampling-rate: 0.1
    logging:
      level: INFO
      pattern: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 3.5 sf-chain-observability 可观测性模块

#### 3.5.1 指标收集

```java
@Component
public class ExecutionMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public void recordExecution(String operationType, String provider, 
                               Duration duration, boolean success) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("sf.chain.execution.duration")
            .tag("operation", operationType)
            .tag("provider", provider)
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
    }
}
```

#### 3.5.2 链路追踪

```java
@Component
public class TracingInterceptor implements ExecutionInterceptor {
    
    private final Tracer tracer;
    
    @Override
    public <T> T intercept(ExecutionContext context, Supplier<T> execution) {
        Span span = tracer.nextSpan()
            .name("sf-chain-execution")
            .tag("operation.type", context.getOperationType())
            .tag("provider", context.getProvider())
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return execution.get();
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 3.6 sf-chain-security 安全模块

#### 3.6.1 认证授权

```java
public interface AuthenticationProvider {
    AuthenticationResult authenticate(AuthenticationRequest request);
    boolean supports(AuthenticationType type);
}

public interface AuthorizationManager {
    boolean hasPermission(Principal principal, String resource, String action);
    List<String> getPermissions(Principal principal);
}
```

#### 3.6.2 限流控制

```java
@Component
public class RateLimitInterceptor implements ExecutionInterceptor {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public <T> T intercept(ExecutionContext context, Supplier<T> execution) {
        String key = buildRateLimitKey(context);
        if (!rateLimiter.tryAcquire(key)) {
            throw new RateLimitExceededException("Rate limit exceeded for: " + key);
        }
        return execution.get();
    }
}
```

## 4. 数据模型重构

### 4.1 核心实体设计

```java
// 执行上下文
public class ExecutionContext {
    private String executionId;
    private String operationType;
    private String provider;
    private String modelName;
    private Map<String, Object> parameters;
    private String sessionId;
    private Principal principal;
    private Instant startTime;
    private Map<String, Object> attributes;
}

// 操作元数据
public class OperationMetadata {
    private String operationType;
    private String description;
    private Class<?> inputType;
    private Class<?> outputType;
    private List<String> supportedProviders;
    private Map<String, Object> defaultParameters;
    private List<String> requiredPermissions;
}

// 提供商配置
public class ProviderConfig {
    private String providerName;
    private String displayName;
    private Map<String, Object> globalConfig;
    private List<ModelConfig> models;
    private HealthCheckConfig healthCheck;
}
```

### 4.2 配置实体

```java
@Entity
@Table(name = "sf_operation_configs")
public class OperationConfigEntity {
    @Id
    private String operationType;
    
    @Column(nullable = false)
    private String provider;
    
    @Column(nullable = false)
    private String modelName;
    
    @Column(columnDefinition = "JSON")
    private String parameters;
    
    @Column(nullable = false)
    private Boolean enabled;
    
    @Column
    private Integer priority;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}
```

## 5. 部署架构

### 5.1 单体部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  sf-chain-app:
    image: sf-chain:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SF_CHAIN_PERSISTENCE_TYPE=mysql
    depends_on:
      - mysql
      - redis
  
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: sf_chain
      MYSQL_ROOT_PASSWORD: password
  
  redis:
    image: redis:7-alpine
```

### 5.2 微服务部署

```yaml
# sf-chain-gateway
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sf-chain-gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: sf-chain-gateway
  template:
    metadata:
      labels:
        app: sf-chain-gateway
    spec:
      containers:
      - name: gateway
        image: sf-chain-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
---
# sf-chain-core
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sf-chain-core
spec:
  replicas: 3
  selector:
    matchLabels:
      app: sf-chain-core
  template:
    metadata:
      labels:
        app: sf-chain-core
    spec:
      containers:
      - name: core
        image: sf-chain-core:latest
        ports:
        - containerPort: 8081
```

## 6. 迁移策略

### 6.1 分阶段迁移计划

#### 阶段1: 基础重构（2-3周）
1. 创建新的模块结构
2. 定义核心接口和SPI
3. 重构配置管理
4. 实现基础的执行引擎

#### 阶段2: 功能迁移（3-4周）
1. 迁移现有操作到新架构
2. 重构模型提供商
3. 实现新的持久化层
4. 添加可观测性支持

#### 阶段3: 增强功能（2-3周）
1. 实现安全模块
2. 添加管理界面
3. 完善监控和告警
4. 性能优化

#### 阶段4: 测试和部署（1-2周）
1. 全面测试
2. 文档更新
3. 部署验证
4. 性能基准测试

### 6.2 兼容性保证

```java
// 提供兼容性适配器
@Component
@ConditionalOnProperty("sf-chain.compatibility.legacy-api.enabled")
public class LegacyAPIAdapter {
    
    private final ExecutionEngine executionEngine;
    
    // 保持旧API兼容
    public <I, O> O execute(String operationType, I input) {
        return executionEngine.execute(operationType, input, ExecutionOptions.defaults());
    }
}
```

### 6.3 数据迁移

```sql
-- 数据迁移脚本
CREATE TABLE sf_operation_configs_new AS 
SELECT 
    operation_type,
    'openai' as provider,
    model_name,
    JSON_OBJECT(
        'maxTokens', max_tokens,
        'temperature', temperature,
        'timeout', timeout_seconds
    ) as parameters,
    enabled,
    1 as priority,
    created_at,
    updated_at
FROM sf_operation_configs_old;
```

## 7. 性能优化

### 7.1 缓存策略

```java
@Component
public class CachingExecutionEngine implements ExecutionEngine {
    
    private final ExecutionEngine delegate;
    private final Cache<String, Object> resultCache;
    
    @Override
    public <I, O> O execute(String operationType, I input, ExecutionOptions options) {
        if (options.isCacheEnabled()) {
            String cacheKey = buildCacheKey(operationType, input, options);
            return (O) resultCache.get(cacheKey, () -> 
                delegate.execute(operationType, input, options)
            );
        }
        return delegate.execute(operationType, input, options);
    }
}
```

### 7.2 连接池优化

```java
@Configuration
public class HttpClientConfig {
    
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newFixedThreadPool(50))
            .build();
    }
}
```

### 7.3 异步处理

```java
@Component
public class AsyncExecutionEngine {
    
    @Async("sf-chain-executor")
    public <I, O> CompletableFuture<O> executeAsync(String operationType, I input, ExecutionOptions options) {
        return CompletableFuture.supplyAsync(() -> 
            syncExecutionEngine.execute(operationType, input, options)
        );
    }
}
```

## 8. 监控和告警

### 8.1 关键指标

- **执行指标**: 成功率、响应时间、吞吐量
- **资源指标**: CPU、内存、网络使用率
- **业务指标**: 操作类型分布、提供商使用情况
- **错误指标**: 错误率、错误类型分布

### 8.2 告警规则

```yaml
# Prometheus告警规则
groups:
- name: sf-chain-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(sf_chain_execution_errors_total[5m]) > 0.1
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "SF-Chain执行错误率过高"
      description: "错误率: {{ $value }}"
  
  - alert: SlowResponse
    expr: histogram_quantile(0.95, rate(sf_chain_execution_duration_seconds_bucket[5m])) > 30
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "SF-Chain响应时间过慢"
```

## 9. 安全考虑

### 9.1 API密钥管理

```java
@Component
public class SecureConfigurationManager {
    
    private final EncryptionService encryptionService;
    
    public void setSecureConfig(String key, String value) {
        String encryptedValue = encryptionService.encrypt(value);
        configurationSource.setConfig(key, encryptedValue);
    }
    
    public String getSecureConfig(String key) {
        String encryptedValue = configurationSource.getConfig(key);
        return encryptionService.decrypt(encryptedValue);
    }
}
```

### 9.2 访问控制

```java
@PreAuthorize("hasPermission(#operationType, 'EXECUTE')")
public <I, O> O execute(String operationType, I input, ExecutionOptions options) {
    // 执行逻辑
}
```

## 10. 测试策略

### 10.1 单元测试

```java
@ExtendWith(MockitoExtension.class)
class ExecutionEngineTest {
    
    @Mock
    private OperationRegistry operationRegistry;
    
    @Mock
    private ProviderRegistry providerRegistry;
    
    @InjectMocks
    private DefaultExecutionEngine executionEngine;
    
    @Test
    void shouldExecuteOperationSuccessfully() {
        // 测试逻辑
    }
}
```

### 10.2 集成测试

```java
@SpringBootTest
@TestPropertySource(properties = {
    "sf-chain.providers.openai.api-key=test-key",
    "sf-chain.persistence.type=memory"
})
class SfChainIntegrationTest {
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    @Test
    void shouldExecuteJsonRepairOperation() {
        String result = executionEngine.execute("json-repair", "{broken json}", ExecutionOptions.defaults());
        assertThat(result).isNotNull();
    }
}
```

### 10.3 性能测试

```java
@Test
void performanceTest() {
    int threadCount = 100;
    int requestsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < requestsPerThread; j++) {
                    executionEngine.execute("json-repair", "{test}", ExecutionOptions.defaults());
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    long duration = System.currentTimeMillis() - startTime;
    
    double tps = (threadCount * requestsPerThread * 1000.0) / duration;
    System.out.println("TPS: " + tps);
}
```

## 11. 文档和示例

### 11.1 快速开始

```java
// 1. 添加依赖
@SpringBootApplication
@EnableSfChain
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 2. 使用框架
@Service
public class MyService {
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    public String repairJson(String brokenJson) {
        return executionEngine.execute("json-repair", brokenJson, ExecutionOptions.defaults());
    }
}
```

### 11.2 自定义操作

```java
@Operation(type = "custom-operation", description = "自定义操作")
public class CustomOperation extends AbstractAIOperation<String, String> {
    
    @Override
    protected String buildPrompt(String input, ExecutionContext context) {
        return "处理: " + input;
    }
    
    @Override
    protected String parseResponse(String response, String input) {
        return response.trim();
    }
}
```

## 12. 总结

这个重构方案将SF-Chain框架转变为一个现代化、模块化、高扩展性的AI调度框架。主要改进包括：

1. **模块化架构**: 清晰的模块边界和职责分离
2. **插件化设计**: 支持第三方扩展和自定义
3. **企业级特性**: 监控、安全、高可用性支持
4. **云原生**: 支持容器化和微服务部署
5. **标准化**: 遵循开源框架最佳实践

通过分阶段的迁移策略，可以平滑地从现有架构过渡到新架构，同时保证向后兼容性和系统稳定性。