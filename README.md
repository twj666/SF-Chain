# SF-Chain 🚀

<div align="center">
  <img src="https://twj666.oss-cn-hangzhou.aliyuncs.com/QQ20250415-235841.png" alt="SF-Chain Logo" width="180" />
  <h3>AI大模型调度框架</h3>
  <p><em>一个框架连接所有AI大模型，让开发者专注于业务逻辑</em></p>
</div>

## ✨ 当前特性

- 🔄 **统一接口** - 使用相同的API调用不同的AI模型
- 🔌 **Spring Boot集成** - 自动配置，零代码即可接入
- 🧩 **多模型支持** - 内置支持DeepSeek、OpenAI GPT、通义千问等主流模型
- ⚙️ **参数调优** - 灵活调整温度、最大token等参数
- 🛡️ **类型安全** - Java强类型设计，在编译时捕获潜在错误
- 🚨 **异常处理** - 统一的异常处理机制，提供清晰的错误信息
- 🧬 **简单扩展** - 通过实现接口快速添加自定义操作

## 🚀 快速入门

### 1️⃣ 添加依赖

将SF-Chain添加到您的Maven项目中：

```xml
<!-- 全功能包（一行依赖搞定所有） -->
<dependency>
    <groupId>com.sfchain</groupId>
    <artifactId>sfchain-starter-all</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 或按需引入 -->
<dependency>
    <groupId>com.sfchain</groupId>
    <artifactId>sfchain-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.sfchain</groupId>
    <artifactId>sfchain-models-deepseek</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2️⃣ 配置模型

在`application.yml`中添加您的配置：

```yaml
sfchain:
  default-model: deepseek-chat  # 设置默认使用的模型
  models:
    deepseek:
      base-url: https://api.deepseek.com/v1
      api-key: ${DEEPSEEK_API_KEY:your-api-key-here}
      temperature: 0.7
      max-tokens: 4096
    
    openai:
      base-url: https://api.openai.com/v1
      api-key: ${OPENAI_API_KEY:your-api-key-here}
      version: gpt-4o
    
    qwen:
      base-url: https://api.qwen.ai/v1
      api-key: ${QWEN_API_KEY:your-api-key-here}
      system-prompt: "你是一个专业的助手"
```

### 3️⃣ 使用AIService

```java
import com.sfchain.core.AIService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MyAIService {

    private final AIService aiService;
    
    public MyAIService(AIService aiService) {
        this.aiService = aiService;
    }
    
    public String generateText(String prompt) {
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        
        // 使用text-generation操作和deepseek-chat模型
        return aiService.execute("text-generation", "deepseek-chat", params);
    }
    
    public String generateWithSpecificModel(String prompt, String modelName) {
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        
        // 使用指定模型生成文本
        return aiService.execute("text-generation", modelName, params);
    }
}
```

### 4️⃣ 运行示例应用

SF-Chain提供了示例应用，帮助您快速上手：

```bash
# 克隆仓库
git clone https://github.com/twj666/SF-Chain.git
cd SF-Chain

# 构建项目
mvn clean install -DskipTests

# 运行示例应用
cd sfchain-examples
mvn spring-boot:run
```

## 🤖 支持的模型

SF-Chain目前支持以下AI大模型：

| 模型名称 | 描述 | 提供商 |
|:-------:|:----:|:------:|
| 🧠 deepseek-chat | DeepSeek Chat对话模型 | DeepSeek |
| 🔮 gpt-4o | GPT-4o多模态模型 | OpenAI |
| 🌐 qwen-plus | 通义千问Plus模型 | 阿里云 |
| 🧪 deepseek-r1 | DeepSeek R1深度思考模型 | SiliconFlow |
| 🔍 deepseek-v3 | DeepSeek V3模型 | SiliconFlow |
| 📚 qwen-72b-preview | 千问72B Preview模型 | SiliconFlow |
| 📝 qwen-72b-instruct | 千问72B Instruct模型 | SiliconFlow |
| 📡 tele-ai | TeleChat2模型 | 中国电信 |
| 🏛️ thudm | GLM-4智谱清言模型 | 清华大学 |

## 📋 使用示例

### 命令行交互式应用

SF-Chain示例应用提供了一个交互式命令行界面，让您可以直接与不同的AI模型对话：

```
┌───────────────────────────────────────────────┐
│             🤖 SFChain AI Assistant            │
└───────────────────────────────────────────────┘
Welcome! I'm your AI assistant powered by SFChain.
Type 'help' to see available commands.
Default model: deepseek-chat

[deepseek-chat] > 你好，请介绍一下自己

我是基于DeepSeek Chat模型的AI助手，由SF-Chain框架提供支持。我可以回答问题、生成文本、
提供建议等。有什么我可以帮助您的吗？

(Generated in 1.2 seconds with deepseek-chat)

[deepseek-chat] > help

📋 Available Commands:
  help    - Show this help message
  models  - List all available models
  use X   - Switch to model X (e.g., 'use gpt-4o')
  temp X  - Set temperature to X (e.g., 'temp 0.8')
  clear   - Clear conversation history
  exit    - Exit the application
```

### 自定义AI操作

您可以通过实现`AIOperation`接口来创建自定义操作：

```java
import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.operation.AIOperation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@AIOp("code-generation")
@Component
public class CodeGenerationOperation implements AIOperation<Map<String, Object>, String> {
    
    @Override
    public List<String> supportedModels() {
        return List.of("deepseek-chat", "gpt-4o");
    }
    
    @Override
    public String buildPrompt(Map<String, Object> params) {
        String language = (String) params.get("language");
        String task = (String) params.get("task");
        
        return String.format(
            "请使用%s语言实现以下功能：\n\n%s\n\n" +
            "只返回代码，不要解释。",
            language, task
        );
    }
    
    @Override
    public String parseResponse(String aiResponse) {
        // 提取代码块
        if (aiResponse.contains("```")) {
            int start = aiResponse.indexOf("```") + 3;
            int langEnd = aiResponse.indexOf("\n", start);
            int end = aiResponse.lastIndexOf("```");
            
            if (start < end && langEnd < end) {
                return aiResponse.substring(langEnd + 1, end).trim();
            }
        }
        return aiResponse;
    }
    
    @Override
    public void validate(Map<String, Object> params) {
        if (!params.containsKey("language")) {
            throw new IllegalArgumentException("Missing required parameter: language");
        }
        if (!params.containsKey("task")) {
            throw new IllegalArgumentException("Missing required parameter: task");
        }
    }
}
```

## 🔮 未来规划

SF-Chain正在积极开发中，未来计划添加以下功能：

- 📊 **流式响应** - 支持流式输出，实时显示AI生成内容
- 🔄 **会话管理** - 增强的对话历史管理，支持复杂的多轮对话
- 🖼️ **多模态支持** - 支持图像、音频等多模态输入和输出
- 🔌 **更多模型集成** - 集成更多国内外主流大模型
- 🛠️ **开发者工具** - 提供更丰富的调试和开发工具
- 📈 **性能优化** - 提高并发处理能力和响应速度
- 🔍 **向量检索** - 集成向量数据库，支持知识库问答
- 🧪 **模型评估** - 提供模型性能和质量评估工具

## 🤝 贡献指南

我们欢迎各种形式的贡献，包括但不限于：

- 🐛 提交bug报告和功能请求
- 💻 提交代码改进和新功能
- 📝 改进文档和示例
- 🧪 添加新的模型实现

## 📄 许可证

SF-Chain使用Apache License 2.0许可证开源。

## 📬 联系方式

- 📮 GitHub Issues: [https://github.com/twj666/SF-Chain/issues](https://github.com/twj666/SF-Chain/issues)
- 📧 Email: suifeng@example.com

---

<p align="center">
  <em>SF-Chain - 让AI大模型调用变得简单而强大 ✨</em>
</p>
