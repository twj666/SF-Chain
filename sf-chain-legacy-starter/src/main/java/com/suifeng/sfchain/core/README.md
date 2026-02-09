# Core Package - 核心包文档

## 概述
`core`包是SF-Chain框架的核心，包含了AI服务、模型管理、操作注册等关键组件。这个包定义了框架的基础架构和核心接口。

## 核心组件详解

### 1. AIService - AI服务核心类
**文件**: `AIService.java`

#### 功能概述
- 统一管理AI操作的执行入口
- 提供同步和异步执行方式
- 支持会话上下文管理
- 提供执行统计和监控

#### 核心方法

##### 同步执行方法
```java
// 基础执行
<INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input)

// 带会话ID的执行
<INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input, String sessionId)

// 完整参数执行
<INPUT, OUTPUT> OUTPUT execute(String operationType, INPUT input, String modelName, String sessionId)
```

##### 异步执行方法
```java
// 异步基础执行
<INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input)

// 异步带模型指定
<INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input, String modelName)

// 异步完整参数
<INPUT, OUTPUT> CompletableFuture<OUTPUT> executeAsync(String operationType, INPUT input, String modelName, String sessionId)
```

##### 会话管理方法
```java
// 设置系统提示词
void setSystemPrompt(String sessionId, String systemPrompt)

// 获取会话上下文
String getSessionContext(String sessionId, boolean includeSystemPrompt)

// 清除会话对话历史
void clearSessionConversation(String sessionId)

// 完全清除会话
void clearSession(String sessionId)

// 检查会话是否存在
boolean sessionExists(String sessionId)
```

#### 执行流程
1. 获取操作实例
2. 检查操作是否启用
3. 记录用户输入到上下文（如果有会话ID）
4. 执行操作
5. 记录AI回复到上下文（如果有会话ID）
6. 记录执行统计

### 2. AIModel - AI模型接口
**文件**: `AIModel.java`

#### 接口定义
```java
public interface AIModel {
    String getName();           // 获取模型名称
    String description();       // 获取模型描述
    String generate(String prompt);  // 生成文本响应
    <T> T generate(String prompt, Class<T> responseType);  // 生成指定类型响应
    boolean isAvailable();      // 检查模型是否可用
}
```

### 3. AIOperationRegistry - 操作注册中心
**文件**: `AIOperationRegistry.java`

#### 功能概述
- 管理AI操作和模型的映射关系
- 提供操作注册和发现功能
- 支持配置化操作参数

#### 核心属性
```java
Map<String, BaseAIOperation<?, ?>> operationMap  // 操作到实例的映射
Map<String, String> modelMapping                // 操作到模型的映射配置
Map<String, OperationConfig> configs              // 操作的默认配置
```

#### 核心方法
```java
// 操作注册
void registerOperation(String operationType, BaseAIOperation<?, ?> operation)

// 获取操作实例
BaseAIOperation<?, ?> getOperation(String operationType)

// 模型映射管理
String getModelForOperation(String operationType)
void setModelForOperation(String operationType, String modelName)

// 配置管理
OperationConfig getOperationConfig(String operationType)
```

#### 配置类: OperationConfig
```java
public static class OperationConfig {
    private boolean enabled = true;         // 是否启用
    private int maxTokens = 4096;           // 最大token数
    private double temperature = 0.7;       // 温度参数
    private boolean requireJsonOutput = true;  // 是否要求JSON输出
    private boolean supportThinking = false;   // 是否支持思考模式
    private int timeoutSeconds = 30;        // 超时时间（秒）
    private int retryCount = 2;             // 重试次数
}
```

### 4. BaseAIOperation - AI操作抽象基类
**文件**: `BaseAIOperation.java`

#### 功能概述
- 提供统一的AI操作接口和实现
- 自动处理泛型类型识别
- 支持注解驱动的配置
- 集成上下文管理和日志记录

#### 核心特性

##### 泛型支持
自动识别输入输出类型：
- 通过反射获取泛型参数类型
- 支持复杂类型的处理

##### 注解驱动
使用`@AIOp`注解配置操作：
```java
@AIOp(
    value = "operationType",           // 操作类型
    defaultModel = "gpt-4",            // 默认模型
    defaultMaxTokens = 2000,             // 默认最大token
    defaultTemperature = 0.8,            // 默认温度
    requireJsonOutput = true,            // 要求JSON输出
    supportThinking = true               // 支持思考模式
)
```

##### 生命周期管理
- `@PostConstruct`自动初始化
- 自动注册到操作注册中心
- 自动设置默认模型映射

#### 流式执行支持
```java
// 流式执行方法
Flux<String> executeStream(INPUT input)
Flux<String> executeStream(INPUT input, String modelName)
Flux<String> executeStream(INPUT input, String modelName, String sessionId)
```

#### 模板方法模式
子类需要实现的核心方法：
```java
// 构建提示词（必须实现）
protected abstract String buildPrompt(INPUT input)

// 解析响应（必须实现）
protected abstract OUTPUT parseResponse(String response)

// 可选的验证方法
protected void validateInput(INPUT input) throws IllegalArgumentException
```

### 5. ModelRegistry - 模型注册中心
**文件**: `ModelRegistry.java`

#### 功能概述
- 管理所有可用的AI模型
- 提供模型注册和发现功能

#### 核心方法
```java
// 模型注册
void registerModel(String name, AIModel model)

// 模型获取
AIModel getModel(String name)

// 模型检查
boolean isModelRegistered(String name)

// 获取所有模型
List<String> getAllModels()
```

### 6. AIPromptBuilder - 提示词构建器
**文件**: `AIPromptBuilder.java`

#### 功能概述
- 提供提示词模板管理
- 支持动态提示词构建
- 集成上下文信息

## 子包结构

### logging 子包
**功能**: AI调用日志管理
- `AICallLog`: 日志实体类
- `AICallLogAspect`: 日志切面
- `AICallLogManager`: 日志管理器
- `AICallLogSummary`: 日志统计汇总

### openai 子包
**功能**: OpenAI兼容模型支持
- `OpenAICompatibleModel`: OpenAI兼容模型实现
- `OpenAIHttpClient`: HTTP客户端
- `OpenAIModelConfig`: 模型配置
- `OpenAIModelFactory`: 模型工厂
- `OpenAIRequest`: 请求对象
- `OpenAIResponse`: 响应对象
- `OpenAIStreamResponse`: 流式响应对象

## 使用示例

### 1. 定义自定义操作
```java
@AIOp(value = "codeReview", defaultModel = "gpt-4")
public class CodeReviewOperation extends BaseAIOperation<String, ReviewResult> {
    
    @Override
    protected String buildPrompt(String input) {
        return "请审查以下代码：\n" + input;
    }
    
    @Override
    protected ReviewResult parseResponse(String response) {
        return objectMapper.readValue(response, ReviewResult.class);
    }
}
```

### 2. 使用AIService执行操作
```java
@Autowired
private AIService aiService;

// 同步执行
ReviewResult result = aiService.execute("codeReview", sourceCode);

// 异步执行
CompletableFuture<ReviewResult> future = aiService.executeAsync("codeReview", sourceCode);

// 带上下文执行
String sessionId = "user123";
aiService.setSystemPrompt(sessionId, "你是一个代码审查专家");
ReviewResult result = aiService.execute("codeReview", sourceCode, sessionId);
```

## 设计模式

### 1. 注册器模式 (Registry Pattern)
- `AIOperationRegistry`: 操作注册管理
- `ModelRegistry`: 模型注册管理

### 2. 模板方法模式 (Template Method Pattern)
- `BaseAIOperation`: 定义算法骨架，子类实现具体步骤

### 3. 策略模式 (Strategy Pattern)
- `AIModel`: 不同的模型实现不同的策略

### 4. 工厂模式 (Factory Pattern)
- `OpenAIModelFactory`: 创建OpenAI模型实例

## 扩展点

### 1. 添加新模型
实现`AIModel`接口并注册到`ModelRegistry`

### 2. 添加新操作
继承`BaseAIOperation`并添加`@AIOp`注解

### 3. 自定义提示词构建
扩展`AIPromptBuilder`类