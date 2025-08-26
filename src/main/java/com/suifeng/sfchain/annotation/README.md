# Annotation Package - 注解包文档

## 概述
`annotation`包定义了SF-Chain框架中使用的所有自定义注解，这些注解用于配置管理、操作定义、模型注册和运行时行为控制。通过注解驱动的方式简化了框架的配置和使用。

## 包结构
```
annotation/
├── SfChainOperation.java      # 操作定义注解
├── SfChainModel.java        # 模型定义注解
├── SfChainConfig.java       # 配置注解
├── EnableSfChain.java       # 框架启用注解
├── OperationConfig.java     # 操作配置注解
└── ModelConfig.java        # 模型配置注解
```

## 核心注解详解

### 1. SfChainOperation - 操作定义注解
**文件**: `SfChainOperation.java`

#### 功能概述
用于标记一个类为SF-Chain的操作实现，定义操作的基本信息和配置。

#### 注解定义
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SfChainOperation {
    
    /**
     * 操作类型名称
     */
    String value() default "";
    
    /**
     * 操作显示名称
     */
    String displayName() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 操作分类
     */
    String category() default "general";
    
    /**
     * 默认使用的模型名称
     */
    String defaultModel() default "gpt-4";
    
    /**
     * 是否启用该操作
     */
    boolean enabled() default true;
    
    /**
     * 操作优先级
     */
    int priority() default 0;
    
    /**
     * 支持的输入类型
     */
    Class<?>[] supportedInputTypes() default {String.class};
    
    /**
     * 输出类型
     */
    Class<?> outputType() default String.class;
    
    /**
     * 是否需要JSON输出
     */
    boolean requireJsonOutput() default false;
    
    /**
     * 默认最大token数
     */
    int defaultMaxTokens() default 2000;
    
    /**
     * 默认温度值
     */
    double defaultTemperature() default 0.7;
    
    /**
     * 默认超时时间（秒）
     */
    int defaultTimeout() default 30;
    
    /**
     * 默认重试次数
     */
    int defaultRetryCount() default 3;
    
    /**
     * 操作标签，用于分组和搜索
     */
    String[] tags() default {};
    
    /**
     * 依赖的操作类型
     */
    String[] dependencies() default {};
    
    /**
     * 是否支持流式处理
     */
    boolean supportStreaming() default false;
    
    /**
     * 是否支持批量处理
     */
    boolean supportBatch() default false;
    
    /**
     * 示例输入
     */
    String exampleInput() default "";
    
    /**
     * 示例输出
     */
    String exampleOutput() default "";
    
    /**
     * 配置模板路径
     */
    String configTemplatePath() default "";
    
    /**
     * 图标路径（用于UI显示）
     */
    String iconPath() default "";
}
```

#### 使用示例
```java
@SfChainOperation(
    value = "json-repair",
    displayName = "JSON修复",
    description = "修复格式错误的JSON字符串",
    category = "data-processing",
    defaultModel = "gpt-4",
    supportedInputTypes = {String.class},
    outputType = String.class,
    requireJsonOutput = true,
    defaultMaxTokens = 1000,
    defaultTemperature = 0.3,
    tags = {"json", "repair", "format"},
    exampleInput = "{\"name\":\"test\",\"age\":30",
    exampleOutput = "{\"name\":\"test\",\"age\":30}"
)
public class JSONRepairOperation extends BaseAIOperation<String, String> {
    // 实现代码...
}
```

### 2. SfChainModel - 模型定义注解
**文件**: `SfChainModel.java`

#### 功能概述
用于定义和注册AI模型，配置模型的基本信息和连接参数。

#### 注解定义
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SfChainModel {
    
    /**
     * 模型名称
     */
    String name();
    
    /**
     * 模型提供商
     */
    ModelProvider provider();
    
    /**
     * 模型版本
     */
    String version() default "1.0";
    
    /**
     * 模型描述
     */
    String description() default "";
    
    /**
     * 模型类型（如text, image, audio等）
     */
    String type() default "text";
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 默认API端点
     */
    String defaultEndpoint() default "";
    
    /**
     * 支持的输入格式
     */
    String[] supportedFormats() default {"text", "json"};
    
    /**
     * 最大输入token数
     */
    int maxInputTokens() default 4000;
    
    /**
     * 最大输出token数
     */
    int maxOutputTokens() default 2000;
    
    /**
     * 支持的参数
     */
    String[] supportedParameters() default {"temperature", "max_tokens", "top_p"};
    
    /**
     * 是否需要API密钥
     */
    boolean requireApiKey() default true;
    
    /**
     * 默认请求头
     */
    String[] defaultHeaders() default {"Content-Type: application/json"};
    
    /**
     * 模型能力标签
     */
    String[] capabilities() default {};
    
    /**
     * 价格信息（每1000个token的价格）
     */
    String pricing() default "0.0";
    
    /**
     * 速率限制（每分钟请求数）
     */
    int rateLimit() default 60;
    
    /**
     * 并发限制
     */
    int concurrentLimit() default 10;
    
    /**
     * 缓存TTL（秒）
     */
    int cacheTtl() default 3600;
    
    /**
     * 健康检查端点
     */
    String healthCheckEndpoint() default "/health";
    
    /**
     * 文档链接
     */
    String documentationUrl() default "";
    
    /**
     * 示例用法
     */
    String[] usageExamples() default {};
}
```

#### 使用示例
```java
@SfChainModel(
    name = "gpt-4",
    provider = ModelProvider.OPENAI,
    version = "2024-01-25",
    description = "OpenAI GPT-4是最先进的语言模型",
    type = "text",
    maxInputTokens = 8000,
    maxOutputTokens = 4000,
    supportedFormats = {"text", "json", "markdown"},
    supportedParameters = {"temperature", "max_tokens", "top_p", "frequency_penalty", "presence_penalty"},
    capabilities = {"text-generation", "code-generation", "analysis", "translation"},
    pricing = "0.03",
    rateLimit = 200,
    documentationUrl = "https://platform.openai.com/docs/models/gpt-4"
)
public class GPT4Model implements AIModel {
    // 实现代码...
}
```

### 3. EnableSfChain - 框架启用注解
**文件**: `EnableSfChain.java`

#### 功能概述
用于启用SF-Chain框架，自动配置和扫描相关组件。

#### 注解定义
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SfChainConfiguration.class)
@ComponentScan(basePackages = "com.suifeng.sfchain")
@EnableConfigurationProperties(SfChainProperties.class)
public @interface EnableSfChain {
    
    /**
     * 是否启用自动配置
     */
    boolean autoConfiguration() default true;
    
    /**
     * 要扫描的基础包
     */
    String[] basePackages() default {};
    
    /**
     * 要排除的自动配置类
     */
    Class<?>[] excludeAutoConfiguration() default {};
    
    /**
     * 是否启用Web端点
     */
    boolean enableWebEndpoints() default true;
    
    /**
     * 是否启用缓存
     */
    boolean enableCaching() default true;
    
    /**
     * 是否启用监控
     */
    boolean enableMonitoring() default true;
    
    /**
     * 是否启用持久化
     */
    boolean enablePersistence() default true;
    
    /**
     * 配置文件路径
     */
    String[] configLocations() default {"classpath:sf-chain.yml"};
    
    /**
     * 是否启用调试模式
     */
    boolean debug() default false;
    
    /**
     * 是否启用实验功能
     */
    boolean enableExperimental() default false;
}
```

#### 使用示例
```java
@SpringBootApplication
@EnableSfChain(
    basePackages = {"com.example.ai", "com.suifeng.sfchain"},
    enableWebEndpoints = true,
    enableCaching = true,
    enableMonitoring = true,
    debug = true
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 4. OperationConfig - 操作配置注解
**文件**: `OperationConfig.java`

#### 功能概述
用于在运行时动态配置操作参数。

#### 注解定义
```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationConfig {
    
    /**
     * 操作类型
     */
    String operationType();
    
    /**
     * 使用的模型
     */
    String model() default "";
    
    /**
     * 最大token数
     */
    int maxTokens() default -1;
    
    /**
     * 温度值
     */
    double temperature() default -1;
    
    /**
     * 超时时间（秒）
     */
    int timeout() default -1;
    
    /**
     * 重试次数
     */
    int retryCount() default -1;
    
    /**
     * 是否需要JSON输出
     */
    boolean requireJsonOutput() default false;
    
    /**
     * 自定义参数
     */
    String[] customParams() default {};
    
    /**
     * 系统提示词
     */
    String systemPrompt() default "";
    
    /**
     * 是否启用缓存
     */
    boolean enableCache() default true;
    
    /**
     * 缓存键前缀
     */
    String cacheKeyPrefix() default "";
    
    /**
     * 缓存TTL（秒）
     */
    int cacheTtl() default 3600;
    
    /**
     * 是否记录日志
     */
    boolean enableLogging() default true;
    
    /**
     * 日志级别
     */
    String logLevel() default "INFO";
    
    /**
     * 是否启用监控
     */
    boolean enableMetrics() default true;
    
    /**
     * 指标名称
     */
    String metricsName() default "";
    
    /**
     * 错误处理策略
     */
    String errorHandling() default "throw";
    
    /**
     * 回退策略
     */
    String fallbackStrategy() default "default";
    
    /**
     * 限流配置
     */
    String rateLimit() default "";
    
    /**
     * 优先级
     */
    int priority() default 0;
    
    /**
     * 异步执行
     */
    boolean async() default false;
    
    /**
     * 超时回调
     */
    String timeoutCallback() default "";
    
    /**
     * 成功回调
     */
    String successCallback() default "";
    
    /**
     * 失败回调
     */
    String failureCallback() default "";
}
```

#### 使用示例
```java
@Service
public class AIService {
    
    @OperationConfig(
        operationType = "text-generation",
        model = "gpt-4",
        maxTokens = 2000,
        temperature = 0.7,
        systemPrompt = "你是一个专业的AI助手",
        enableCache = true,
        cacheTtl = 1800,
        async = false,
        enableMetrics = true,
        metricsName = "text_generation_requests"
    )
    public String generateText(String prompt) {
        // 实现代码...
    }
}
```

### 5. ModelConfig - 模型配置注解
**文件**: `ModelConfig.java`

#### 功能概述
用于在运行时动态配置模型参数。

#### 注解定义
```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelConfig {
    
    /**
     * 模型名称
     */
    String modelName();
    
    /**
     * API密钥
     */
    String apiKey() default "";
    
    /**
     * 基础URL
     */
    String baseUrl() default "";
    
    /**
     * 超时时间（秒）
     */
    int timeout() default 30;
    
    /**
     * 最大重试次数
     */
    int maxRetries() default 3;
    
    /**
     * 请求头
     */
    String[] headers() default {};
    
    /**
     * 代理配置
     */
    String proxy() default "";
    
    /**
     * 是否启用SSL验证
     */
    boolean sslVerify() default true;
    
    /**
     * 连接池配置
     */
    String connectionPool() default "";
    
    /**
     * 自定义参数
     */
    String[] customParameters() default {};
    
    /**
     * 健康检查配置
     */
    String healthCheck() default "";
    
    /**
     * 监控配置
     */
    String monitoring() default "";
    
    /**
     * 日志配置
     */
    String logging() default "";
    
    /**
     * 缓存配置
     */
    String caching() default "";
    
    /**
     * 限流配置
     */
    String rateLimiting() default "";
    
    /**
     * 错误处理
     */
    String errorHandling() default "";
    
    /**
     * 重试策略
     */
    String retryStrategy() default "exponential";
    
    /**
     * 断路器配置
     */
    String circuitBreaker() default "";
    
    /**
     * 负载均衡
     */
    String loadBalancing() default "";
    
    /**
     * 服务发现
     */
    String serviceDiscovery() default "";
}
```

#### 使用示例
```java
@Configuration
public class ModelConfiguration {
    
    @Bean
    @ModelConfig(
        modelName = "gpt-4",
        apiKey = "${OPENAI_API_KEY}",
        baseUrl = "https://api.openai.com",
        timeout = 30,
        maxRetries = 3,
        headers = {"Content-Type: application/json", "Accept: application/json"},
        sslVerify = true,
        retryStrategy = "exponential"
    )
    public AIModel gpt4Model() {
        return new OpenAIModel("gpt-4");
    }
}
```

## 注解处理器

### 1. 操作注解处理器
```java
@Component
public class OperationAnnotationProcessor {
    
    @Autowired
    private AIOperationRegistry operationRegistry;
    
    @PostConstruct
    public void processOperationAnnotations() {
        // 扫描所有带有@SfChainOperation的类
        Map<String, Object> operationBeans = applicationContext.getBeansWithAnnotation(SfChainOperation.class);
        
        for (Map.Entry<String, Object> entry : operationBeans.entrySet()) {
            SfChainOperation annotation = entry.getValue().getClass().getAnnotation(SfChainOperation.class);
            registerOperation(annotation, entry.getValue());
        }
    }
    
    private void registerOperation(SfChainOperation annotation, Object operationBean) {
        OperationConfigData config = new OperationConfigData();
        config.setOperationType(annotation.value());
        config.setModelName(annotation.defaultModel());
        config.setEnabled(annotation.enabled());
        config.setMaxTokens(annotation.defaultMaxTokens());
        config.setTemperature(annotation.defaultTemperature());
        config.setRequireJsonOutput(annotation.requireJsonOutput());
        
        operationRegistry.registerOperation(annotation.value(), operationBean, config);
    }
}
```

### 2. 模型注解处理器
```java
@Component
public class ModelAnnotationProcessor {
    
    @Autowired
    private ModelRegistry modelRegistry;
    
    @PostConstruct
    public void processModelAnnotations() {
        // 扫描所有带有@SfChainModel的类
        Map<String, Object> modelBeans = applicationContext.getBeansWithAnnotation(SfChainModel.class);
        
        for (Map.Entry<String, Object> entry : modelBeans.entrySet()) {
            SfChainModel annotation = entry.getValue().getClass().getAnnotation(SfChainModel.class);
            registerModel(annotation, (AIModel) entry.getValue());
        }
    }
    
    private void registerModel(SfChainModel annotation, AIModel model) {
        ModelConfigData config = new ModelConfigData();
        config.setModelName(annotation.name());
        config.setDescription(annotation.description());
        config.setEnabled(annotation.enabled());
        
        modelRegistry.registerModel(annotation.name(), model, config);
    }
}
```

## 最佳实践

### 1. 注解使用原则
- 使用注解简化配置，避免重复代码
- 注解值应该清晰明确，避免歧义
- 合理使用默认值，减少配置复杂度

### 2. 注解组合使用
```java
@SfChainOperation(
    value = "advanced-code-generation",
    displayName = "高级代码生成",
    category = "development"
)
@OperationConfig(
    operationType = "advanced-code-generation",
    model = "gpt-4",
    maxTokens = 4000,
    temperature = 0.2,
    systemPrompt = "你是一个专业的软件开发专家"
)
public class AdvancedCodeGenerationOperation extends BaseAIOperation<String, String> {
    // 实现代码...
}
```

### 3. 动态配置
```java
@Service
public class DynamicConfigurationService {
    
    @OperationConfig(
        operationType = "dynamic-text-processing",
        model = "${dynamic.model:gpt-3.5-turbo}",
        maxTokens = "${dynamic.max-tokens:1000}",
        temperature = "${dynamic.temperature:0.7}"
    )
    public String processText(String input) {
        // 实现代码...
    }
}
```

## 扩展指南

### 1. 创建自定义注解
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomOperation {
    String value() default "";
    String[] parameters() default {};
    boolean trace() default false;
}
```

### 2. 注解验证器
```java
@Component
public class AnnotationValidator {
    
    public void validateOperationAnnotation(Class<?> clazz) {
        SfChainOperation annotation = clazz.getAnnotation(SfChainOperation.class);
        if (annotation != null) {
            validateOperationConfig(annotation);
        }
    }
    
    private void validateOperationConfig(SfChainOperation annotation) {
        Assert.notNull(annotation.value(), "Operation value cannot be null");
        Assert.isTrue(annotation.defaultMaxTokens() > 0, "Max tokens must be positive");
        Assert.isTrue(annotation.defaultTemperature() >= 0 && annotation.defaultTemperature() <= 2, 
            "Temperature must be between 0 and 2");
    }
}
```