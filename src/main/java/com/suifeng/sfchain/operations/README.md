# Operations Package - 操作实现包文档

## 概述
`operations`包包含了SF-Chain框架中所有具体的AI操作实现。这些操作类都继承自`BaseAIOperation`，通过注解驱动的方式注册到框架中，提供特定的AI处理能力。

## 操作设计原则

### 1. 单一职责原则
每个操作类只负责一个特定的AI任务，如JSON修复、代码验证等。

### 2. 可扩展性
通过继承`BaseAIOperation`基类，可以轻松添加新的操作类型。

### 3. 配置驱动
操作的参数可以通过配置文件动态调整，无需修改代码。

## 现有操作详解

### 1. JSONRepairOperation - JSON修复操作
**文件**: `JSONRepairOperation.java`
**操作类型**: `jsonRepair`

#### 功能概述
- 自动检测和修复损坏的JSON字符串
- 支持多种JSON错误类型的修复
- 返回结构化的修复结果

#### 输入输出
- **输入类型**: `String` - 损坏的JSON字符串
- **输出类型**: `JSONRepairResult` - 修复结果对象

#### 修复能力
1. **引号修复**
   - 修复缺失的引号
   - 统一引号格式
   
2. **逗号修复**
   - 修复多余的逗号
   - 添加缺失的逗号
   
3. **括号匹配**
   - 修复不匹配的括号
   - 补全缺失的括号
   
4. **转义字符**
   - 修复错误的转义
   - 添加必要的转义

#### 使用示例
```java
@Autowired
private AIService aiService;

// 修复损坏的JSON
String brokenJson = "{name: 'test', age: 25,}";
JSONRepairResult result = aiService.execute("jsonRepair", brokenJson);

if (result.isSuccess()) {
    String fixedJson = result.getFixedJson();
    System.out.println("修复后的JSON: " + fixedJson);
} else {
    System.out.println("修复失败: " + result.getErrorMessage());
}
```

#### 配置参数
```yaml
sf-chain:
  operations:
    jsonRepair:
      modelName: "gpt-4"
      enabled: true
      maxTokens: 2000
      temperature: 0.3
      requireJsonOutput: true
      timeoutSeconds: 30
```

### 2. ModelValidationOperation - 模型验证操作
**文件**: `ModelValidationOperation.java`
**操作类型**: `modelValidation`

#### 功能概述
- 验证AI模型的输出是否符合预期格式
- 提供详细的验证报告
- 支持自定义验证规则

#### 输入输出
- **输入类型**: `ValidationRequest` - 包含待验证内容和验证规则
- **输出类型**: `ValidationResult` - 验证结果对象

#### 验证类型
1. **格式验证**
   - JSON格式验证
   - XML格式验证
   - 正则表达式匹配
   
2. **内容验证**
   - 必填字段检查
   - 数据类型验证
   - 数值范围验证
   
3. **业务验证**
   - 逻辑一致性检查
   - 业务规则验证
   - 依赖关系验证

#### 使用示例
```java
// 创建验证请求
ValidationRequest request = new ValidationRequest();
request.setContent(aiResponse);
request.setSchema(validationSchema);
request.setRequiredFields(Arrays.asList("id", "name", "status"));

// 执行验证
ValidationResult result = aiService.execute("modelValidation", request);

// 处理结果
if (result.isValid()) {
    System.out.println("验证通过");
} else {
    System.out.println("验证失败: " + result.getErrors());
}
```

#### 验证规则定义
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "pattern": "^[A-Z0-9]+$"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 100
    },
    "status": {
      "type": "string",
      "enum": ["active", "inactive", "pending"]
    }
  },
  "required": ["id", "name", "status"]
}
```

## 操作开发指南

### 1. 创建新操作

#### 步骤1: 创建操作类
```java
@AIOp(
    value = "customOperation",
    defaultModel = "gpt-4",
    defaultMaxTokens = 1000,
    defaultTemperature = 0.7,
    requireJsonOutput = true,
    supportThinking = false
)
public class CustomOperation extends BaseAIOperation<InputType, OutputType> {
    
    @Override
    protected String buildPrompt(InputType input) {
        // 构建提示词逻辑
        return "请处理以下输入: " + input.toString();
    }
    
    @Override
    protected OutputType parseResponse(String response) {
        // 解析响应逻辑
        return objectMapper.readValue(response, OutputType.class);
    }
    
    @Override
    protected void validateInput(InputType input) throws IllegalArgumentException {
        // 输入验证逻辑
        if (input == null) {
            throw new IllegalArgumentException("输入不能为空");
        }
    }
}
```

#### 步骤2: 定义输入输出类型
```java
@Data
public class InputType {
    private String field1;
    private int field2;
    private List<String> field3;
}

@Data
public class OutputType {
    private boolean success;
    private String result;
    private List<String> errors;
}
```

#### 步骤3: 配置操作参数
```yaml
sf-chain:
  operations:
    customOperation:
      modelName: "gpt-4"
      enabled: true
      maxTokens: 1000
      temperature: 0.7
      requireJsonOutput: true
      timeoutSeconds: 30
      retryCount: 2
```

### 2. 操作最佳实践

#### 提示词设计
1. **清晰明确**: 提示词应该清晰描述任务要求
2. **结构化**: 使用结构化的提示词模板
3. **上下文**: 充分利用上下文信息
4. **示例**: 提供输入输出示例

#### 错误处理
1. **输入验证**: 在`validateInput`方法中验证输入
2. **异常处理**: 在`parseResponse`中处理解析异常
3. **错误信息**: 提供详细的错误描述
4. **重试机制**: 利用框架的重试功能

#### 性能优化
1. **token限制**: 合理设置maxTokens避免浪费
2. **缓存策略**: 对可缓存的结果进行缓存
3. **批量处理**: 支持批量输入的处理
4. **异步执行**: 利用异步API提高并发

### 3. 操作测试

#### 单元测试
```java
@SpringBootTest
public class JSONRepairOperationTest {
    
    @Autowired
    private AIService aiService;
    
    @Test
    public void testRepairValidJson() {
        String brokenJson = "{\"name\": \"test", \"age\": 25}";
        JSONRepairResult result = aiService.execute("jsonRepair", brokenJson);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getFixedJson());
    }
    
    @Test
    public void testRepairInvalidJson() {
        String invalidJson = "invalid json string";
        JSONRepairResult result = aiService.execute("jsonRepair", invalidJson);
        
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }
}
```

#### 集成测试
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OperationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void testOperationViaApi() {
        String requestBody = "{\"content\": \"{name: test}\"}";
        
        ResponseEntity<JSONRepairResult> response = restTemplate.postForEntity(
            "/sf-chain/operations/jsonRepair",
            requestBody,
            JSONRepairResult.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }
}
```

## 操作注册机制

### 自动注册
所有继承`BaseAIOperation`的类都会通过`@PostConstruct`自动注册：

```java
@PostConstruct
public void init() {
    // 自动注册到操作注册中心
    operationRegistry.registerOperation(annotation.value(), this);
    
    // 自动设置默认模型映射
    if (!annotation.defaultModel().isEmpty()) {
        operationRegistry.setModelForOperation(annotation.value(), annotation.defaultModel());
    }
}
```

### 手动注册
```java
@Component
public class OperationRegistrar {
    
    @Autowired
    private AIOperationRegistry registry;
    
    @PostConstruct
    public void registerCustomOperations() {
        registry.registerOperation("customOp", new CustomOperation());
    }
}
```

## 操作配置管理

### 动态配置
操作的所有参数都可以通过配置文件动态调整，无需重启应用：

#### 配置优先级
1. 运行时配置（通过API设置）
2. 配置文件中的配置
3. 注解中的默认值

#### 配置热更新
```java
@EventListener
public void handleConfigChange(ConfigurationChangeEvent event) {
    if (event.affectsOperation("jsonRepair")) {
        // 重新加载配置
        reloadOperationConfig();
    }
}
```

## 操作监控

### 执行统计
每个操作都会自动记录执行统计：
- 执行次数
- 成功/失败次数
- 平均执行时间
- 错误率

### 监控指标
```java
public class OperationMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordOperationExecution(String operationType, boolean success, long duration) {
        meterRegistry.counter("sfchain.operation.executions", 
            "operation", operationType,
            "status", success ? "success" : "failure"
        ).increment();
        
        meterRegistry.timer("sfchain.operation.duration", 
            "operation", operationType
        ).record(duration, TimeUnit.MILLISECONDS);
    }
}
```