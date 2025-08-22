# SF-Chain 🚀

<div align="center">
  <img src="https://twj666.oss-cn-hangzhou.aliyuncs.com/SF_CHAIN.png" alt="SF-Chain Logo" width="200" />
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

## 🚀 快速开始
### 1️⃣ 添加依赖
在您的 pom.xml 中添加SF-Chain依赖：

```
<dependency>
    <groupId>com.suifeng</groupId>
    <artifactId>sf-chain</artifactId>
    <version>1.0.0</version>
</dependency>
```
### 2️⃣ 配置文件
在 application.yml 中添加配置：

```
# ===========================================
# SF-Chain AI框架配置
# ===========================================
sf-chain:
  # 授权配置
  auth-token: "suifeng666" # 访问秘钥
  authEnabled: true
  persistence:
    database-type: mysql # 支持: mysql, postgresql
```

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
