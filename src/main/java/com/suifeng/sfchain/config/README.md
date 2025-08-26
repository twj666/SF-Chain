# Config Package - 配置包文档

## 概述
`config`包包含了SF-Chain框架的所有配置类，负责系统的初始化、安全配置、Web配置以及与OpenAI的集成配置。这些配置类确保框架能够正确启动并与各种外部服务集成。

## 配置类详解

### 1. SfChainAutoConfiguration - 主自动配置类
**文件**: `SfChainAutoConfiguration.java`

#### 功能概述
- 框架的主自动配置类
- 负责初始化所有核心组件
- 条件化配置，确保依赖存在时才加载

#### 核心配置
```java
@Configuration
@EnableConfigurationProperties({
    SfChainProperties.class,
    OpenAIModelsConfig.class
})
@ConditionalOnProperty(prefix = "sf-chain", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SfChainAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public AIService aiService() {
        return new AIService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ModelRegistry modelRegistry() {
        return new ModelRegistry();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public AIOperationRegistry operationRegistry() {
        return new AIOperationRegistry();
    }
}
```

#### 条件化配置
- `@ConditionalOnProperty`: 基于配置属性的条件加载
- `@ConditionalOnMissingBean`: 当容器中不存在指定Bean时才创建
- `@ConditionalOnClass`: 当类路径中存在指定类时才加载

### 2. OpenAIAutoConfiguration - OpenAI自动配置类
**文件**: `OpenAIAutoConfiguration.java`

#### 功能概述
- 配置OpenAI相关的所有组件
- 管理OpenAI模型的初始化和注册
- 处理API密钥和基础URL的配置

#### 配置属性
```yaml
sf-chain:
  openai:
    models:
      - name: "gpt-4"
        api-key: "${OPENAI_API_KEY}"
        base-url: "https://api.openai.com"
        description: "GPT-4模型"
        enabled: true
      - name: "gpt-3.5-turbo"
        api-key: "${OPENAI_API_KEY}"
        base-url: "https://api.openai.com"
        description: "GPT-3.5 Turbo模型"
        enabled: true
```

#### 核心方法
```java
@Bean
public OpenAIModelFactory openAIModelFactory() {
    return new OpenAIModelFactory();
}

@Bean
public OpenAIHttpClient openAIHttpClient() {
    return new OpenAIHttpClient();
}

@PostConstruct
public void initializeModels() {
    // 初始化所有配置的OpenAI模型
}
```

### 3. OpenAIModelsConfig - OpenAI模型配置
**文件**: `OpenAIModelsConfig.java`

#### 功能概述
- 定义OpenAI模型的配置结构
- 支持多模型配置
- 提供模型配置的验证和默认值

#### 配置结构
```java
@ConfigurationProperties(prefix = "sf-chain.openai")
public class OpenAIModelsConfig {
    
    private List<ModelConfig> models = new ArrayList<>();
    
    @Data
    public static class ModelConfig {
        private String name;           // 模型名称
        private String apiKey;         // API密钥
        private String baseUrl;        // 基础URL
        private String description;    // 描述
        private boolean enabled = true; // 是否启用
        private Map<String, String> headers = new HashMap<>(); // 自定义请求头
        private Integer timeout = 30;   // 超时时间（秒）
        private Integer maxRetries = 3;   // 最大重试次数
    }
}
```

#### 使用示例
```yaml
sf-chain:
  openai:
    models:
      - name: "custom-gpt"
        api-key: "sk-xxx"
        base-url: "https://custom-api.com"
        description: "自定义GPT模型"
        enabled: true
        headers:
          "X-Custom-Header": "value"
        timeout: 60
        max-retries: 5
```

### 4. SfChainWebConfig - Web配置类
**文件**: `SfChainWebConfig.java`

#### 功能概述
- 配置Web相关的设置
- 定义拦截器和过滤器
- 设置跨域资源共享（CORS）

#### 核心配置
```java
@Configuration
public class SfChainWebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/sf-chain/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor())
                .addPathPatterns("/sf-chain/**");
    }
    
    @Bean
    public AuthorizationInterceptor authorizationInterceptor() {
        return new AuthorizationInterceptor();
    }
}
```

#### CORS配置详解
- `allowedOriginPatterns`: 允许的来源，支持通配符
- `allowedMethods`: 允许的HTTP方法
- `allowedHeaders`: 允许的请求头
- `allowCredentials`: 允许发送cookie
- `maxAge`: 预检请求缓存时间（秒）

### 5. WebConfig - Web基础配置
**文件**: `WebConfig.java`

#### 功能概述
- 配置Web基础设置
- 设置消息转换器
- 配置异常处理

#### 核心配置
```java
@Configuration
public class WebConfig {
    
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToEnable(SerializationFeature.INDENT_OUTPUT);
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.modules(new JavaTimeModule());
        return builder;
    }
    
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        return new MappingJackson2HttpMessageConverter(jacksonBuilder().build());
    }
}
```

### 6. AuthorizationInterceptor - 授权拦截器
**文件**: `AuthorizationInterceptor.java`

#### 功能概述
- 实现API访问控制
- 支持多种认证方式
- 提供细粒度的权限控制

#### 认证方式

##### 1. API Key认证
```java
private boolean validateApiKey(String apiKey) {
    // 验证API密钥的有效性
    return apiKeyService.isValidApiKey(apiKey);
}
```

##### 2. JWT Token认证
```java
private boolean validateJwtToken(String token) {
    try {
        Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
        return true;
    } catch (JwtException e) {
        return false;
    }
}
```

#### 权限控制
```java
public class AuthorizationInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        String apiKey = request.getHeader("X-API-Key");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return validateJwtToken(authHeader.substring(7));
        } else if (apiKey != null) {
            return validateApiKey(apiKey);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
```

## 配置属性详解

### 1. 主配置属性
```yaml
sf-chain:
  enabled: true                    # 是否启用框架
  
  # 持久化配置
  persistence:
    type: "mysql"                  # 持久化类型：mysql, postgresql, memory
    
  # 日志配置
  logging:
    enabled: true                  # 是否启用调用日志
    max-age-days: 30               # 日志保留天数
    
  # 缓存配置
  cache:
    enabled: true                  # 是否启用缓存
    ttl-minutes: 60                # 缓存过期时间（分钟）
```

### 2. OpenAI配置属性
```yaml
sf-chain:
  openai:
    # 全局配置
    timeout: 30                    # 全局超时时间（秒）
    max-retries: 3                 # 全局最大重试次数
    
    # 模型列表
    models:
      - name: "gpt-4"
        api-key: "${OPENAI_API_KEY}"
        base-url: "https://api.openai.com/v1"
        description: "GPT-4最新版本"
        enabled: true
        timeout: 60
        max-retries: 5
        headers:
          "User-Agent": "SF-Chain/1.0"
          
      - name: "gpt-3.5-turbo"
        api-key: "${OPENAI_API_KEY}"
        base-url: "https://api.openai.com/v1"
        description: "GPT-3.5 Turbo"
        enabled: true
        timeout: 30
        max-retries: 3
```

### 3. 安全配置属性
```yaml
sf-chain:
  security:
    # API密钥配置
    api-key:
      enabled: true                # 是否启用API密钥认证
      keys:
        - "key1"
        - "key2"
        
    # JWT配置
    jwt:
      enabled: false               # 是否启用JWT认证
      secret: "your-jwt-secret"    # JWT密钥
      expiration-hours: 24         # Token过期时间（小时）
      
    # 访问控制
    access-control:
      enabled: true                # 是否启用访问控制
      rules:
        - path: "/sf-chain/admin/**"
          roles: ["ADMIN"]
        - path: "/sf-chain/**"
          roles: ["USER", "ADMIN"]
```

## 配置加载顺序

### 1. 配置优先级
1. 命令行参数
2. 环境变量
3. application-{profile}.yml
4. application.yml
5. 默认值

### 2. 配置合并策略
- 列表配置：完全替换
- 映射配置：深度合并
- 标量配置：后加载的覆盖先加载的

## 配置验证

### 1. 启动时验证
框架启动时会自动验证所有配置：
- 检查必填字段
- 验证数值范围
- 检查依赖关系

### 2. 运行时验证
通过API接口动态验证配置：
```java
@PostConstruct
public void validateConfiguration() {
    // 验证模型配置
    for (ModelConfig config : modelsConfig.getModels()) {
        validateModelConfig(config);
    }
}
```

## 配置热加载

### 1. 支持的配置
- 模型启用/禁用状态
- 操作配置参数
- 日志级别

### 2. 不支持的配置
- 数据库连接配置
- 端口配置
- 安全配置

### 3. 热加载实现
```java
@EventListener
public void handleConfigChange(EnvironmentChangeEvent event) {
    // 重新加载配置
    reloadConfiguration();
    
    // 更新运行中的组件
    updateRunningComponents();
}
```

## 配置最佳实践

### 1. 环境分离
```yaml
# application-dev.yml
sf-chain:
  openai:
    models:
      - name: "gpt-3.5-turbo"
        api-key: "dev-key"
        base-url: "https://api.openai.com"

# application-prod.yml
sf-chain:
  openai:
    models:
      - name: "gpt-4"
        api-key: "${PROD_OPENAI_KEY}"
        base-url: "https://api.openai.com"
```

### 2. 敏感信息保护
```yaml
# 使用环境变量
sf-chain:
  openai:
    models:
      - name: "gpt-4"
        api-key: "${OPENAI_API_KEY}"
        base-url: "${OPENAI_BASE_URL:https://api.openai.com}"
```

### 3. 配置模板
```yaml
# config-template.yml
sf-chain:
  enabled: true
  
  persistence:
    type: "${PERSISTENCE_TYPE:memory}"
    
  openai:
    models:
      - name: "${MODEL_NAME:gpt-3.5-turbo}"
        api-key: "${API_KEY:}"
        base-url: "${BASE_URL:https://api.openai.com}"
        enabled: ${MODEL_ENABLED:true}
        timeout: ${TIMEOUT:30}
```