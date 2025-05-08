package com.sfchain.examples.demo;

import com.sfchain.core.model.AIModel;
import com.sfchain.core.model.ModelParameters;
import com.sfchain.core.registry.ModelRegistry;
import com.sfchain.core.AIService;

import com.sfchain.examples.common.ConsoleUI;
import com.sfchain.examples.common.DemoRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.sfchain.core.constant.AIConstant.SILI_DEEP_SEEK_V3;
import static com.sfchain.examples.common.ConsoleUI.*;


/**
 * 聊天演示模块
 * 展示基本的对话能力
 */
@Component
public class ChatDemo implements DemoRunner {

    @Autowired
    private AIService aiService;
    
    @Autowired
    private ModelRegistry modelRegistry;

    @Override
    public String getName() {
        return "AI聊天助手";
    }

    @Override
    public String getDescription() {
        return "与AI进行自然对话，支持多种模型和参数调整";
    }

    @Override
    public String getIcon() {
        return "💬";
    }
    
    @Override
    public String getCategory() {
        return "基础功能";
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
    
    @Override
    public String[] getTags() {
        return new String[]{"对话", "聊天", "基础"};
    }

    @Override
    public void run(Scanner scanner, ConsoleUI ui) {
        // 获取所有可用模型
        Map<String, AIModel> availableModels = modelRegistry.getAllModels();

        // 当前使用的模型
        AtomicReference<String> currentModel = new AtomicReference<>(SILI_DEEP_SEEK_V3);

        // 当前系统提示词
        AtomicReference<String> systemPrompt = new AtomicReference<>(
                "You are a helpful, respectful and honest assistant. Always answer as helpfully as possible, while being safe."
        );

        // 会话历史
        List<Map<String, String>> conversationHistory = new ArrayList<>();

        ui.showTitle("AI聊天助手");
        System.out.println(ANSI_CYAN + "与AI进行自然对话，体验SFChain的基础对话能力" + ANSI_RESET);
        System.out.println(ANSI_BRIGHT_BLACK + "输入'help'查看可用命令，输入'exit'返回主菜单" + ANSI_RESET + "\n");
        System.out.println(ANSI_GREEN + "当前模型: " + currentModel.get() + ANSI_RESET);

        boolean running = true;
        while (running) {
            // 显示提示符
            System.out.print("\n[" + currentModel.get() + "] > ");
            String input = scanner.nextLine().trim();

            // 处理命令或生成内容
            if (input.isEmpty()) {
                continue;
            } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit") || input.equals("0")) {
                running = false;
                continue;
            } else if (input.equalsIgnoreCase("help")) {
                showHelpMessage();
                continue;
            } else if (input.equalsIgnoreCase("models")) {
                showAvailableModels(availableModels);
                continue;
            } else if (input.equalsIgnoreCase("clear")) {
                clearConversation(conversationHistory);
                continue;
            } else if (input.startsWith("use ")) {
                String modelName = input.substring(4).trim();
                if (switchModel(modelName, availableModels, currentModel)) {
                    System.out.println(ANSI_GREEN + "已切换到模型: " + currentModel.get() + ANSI_RESET);
                }
                continue;
            } else if (input.startsWith("temp ")) {
                try {
                    double temp = Double.parseDouble(input.substring(5).trim());
                    setTemperature(temp, currentModel.get(), availableModels);
                } catch (NumberFormatException e) {
                    System.out.println(ANSI_RED + "无效的温度值，请输入0.0到2.0之间的数字" + ANSI_RESET);
                }
                continue;
            } else if (input.startsWith("system ")) {
                String newSystemPrompt = input.substring(7).trim();
                setSystemPrompt(newSystemPrompt, systemPrompt);
                continue;
            } else if (input.equalsIgnoreCase("info")) {
                showSessionInfo(currentModel.get(), availableModels, systemPrompt.get(), conversationHistory.size());
                continue;
            }

            // 记录用户输入
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", input);
            conversationHistory.add(userMessage);

            try {
                // 准备参数
                Map<String, Object> params = new HashMap<>();
                params.put("prompt", input);
                params.put("systemPrompt", systemPrompt.get());
                params.put("markdown", true);

                // 添加会话历史
                if (!conversationHistory.isEmpty()) {
                    params.put("history", new ArrayList<>(conversationHistory));
                }

                // 执行生成
                ui.showLoading("AI思考中");
                long startTime = System.currentTimeMillis();
                String response = aiService.execute("text-generation", currentModel.get(), params);
                long endTime = System.currentTimeMillis();

                // 记录AI响应
                Map<String, String> aiMessage = new HashMap<>();
                aiMessage.put("role", "assistant");
                aiMessage.put("content", response);
                conversationHistory.add(aiMessage);

                // 打印响应
                System.out.println("\n" + formatResponse(response));
                System.out.printf(ANSI_BRIGHT_BLACK + "(生成耗时: %s，使用模型: %s)" + ANSI_RESET + "\n",
                        ui.formatDuration(endTime - startTime), currentModel.get());

            } catch (Exception e) {
                System.out.println(ANSI_RED + "错误: " + e.getMessage() + ANSI_RESET);
                // 如果出错，移除最后一条用户消息
                if (!conversationHistory.isEmpty()) {
                    conversationHistory.remove(conversationHistory.size() - 1);
                }
            }
        }
        
        ui.clearScreen();
    }

    /**
     * 打印帮助信息
     */
    private void showHelpMessage() {
        System.out.println("\n" + ANSI_YELLOW + "📋 可用命令:" + ANSI_RESET);
        System.out.println("  help    - 显示此帮助信息");
        System.out.println("  models  - 列出所有可用模型");
        System.out.println("  use X   - 切换到模型X (例如: 'use gpt-4o')");
        System.out.println("  temp X  - 设置温度参数为X (例如: 'temp 0.8')");
        System.out.println("  system X- 设置系统提示词 (例如: 'system You are a helpful assistant')");
        System.out.println("  clear   - 清除对话历史");
        System.out.println("  info    - 显示当前会话信息");
        System.out.println("  exit    - 返回主菜单");
    }

    /**
     * 打印可用模型
     */
    private void showAvailableModels(Map<String, AIModel> models) {
        System.out.println("\n" + ANSI_YELLOW + "🤖 可用模型:" + ANSI_RESET);

        // 按类型分组显示模型
        Map<String, List<AIModel>> modelsByType = new HashMap<>();

        // 分组
        models.values().forEach(model -> {
            String type = getModelType(model.getName());
            modelsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(model);
        });

        // 按类型显示
        modelsByType.forEach((type, modelList) -> {
            System.out.println("\n  " + ANSI_BOLD + type + ":" + ANSI_RESET);
            modelList.forEach(model -> {
                ModelParameters params = model.getParameters();
                System.out.printf("    %-20s - %s (温度: %.1f)\n",
                        model.getName(),
                        model.description(),
                        params.temperature());
            });
        });
    }

    /**
     * 获取模型类型
     */
    private String getModelType(String modelName) {
        if (modelName.startsWith("gpt")) {
            return "OpenAI模型";
        } else if (modelName.startsWith("deepseek")) {
            return "DeepSeek模型";
        } else if (modelName.startsWith("qwen")) {
            return "通义千问模型";
        } else if (modelName.startsWith("claude")) {
            return "Anthropic模型";
        } else {
            return "其他模型";
        }
    }

    /**
     * 切换模型
     */
    private boolean switchModel(String modelName, Map<String, AIModel> availableModels, AtomicReference<String> currentModel) {
        if (availableModels.containsKey(modelName)) {
            currentModel.set(modelName);
            return true;
        } else {
            System.out.println(ANSI_RED + "未找到模型: " + modelName + ANSI_RESET);
            System.out.println("使用'models'命令查看可用模型。");
            return false;
        }
    }

    /**
     * 设置温度参数
     */
    private void setTemperature(double temperature, String modelName, Map<String, AIModel> availableModels) {
        if (temperature < 0.0 || temperature > 2.0) {
            System.out.println(ANSI_RED + "温度参数必须在0.0到2.0之间" + ANSI_RESET);
            return;
        }

        AIModel model = availableModels.get(modelName);
        if (model != null) {
            ModelParameters params = model.getParameters();
            params.temperature(temperature);
            model.withParameters(params);
            System.out.printf(ANSI_GREEN + "已将%s的温度参数设置为%.1f" + ANSI_RESET + "\n", modelName, temperature);
        } else {
            System.out.println(ANSI_RED + "未找到模型: " + modelName + ANSI_RESET);
        }
    }

    /**
     * 设置系统提示词
     */
    private void setSystemPrompt(String newPrompt, AtomicReference<String> systemPrompt) {
        if (newPrompt == null || newPrompt.trim().isEmpty()) {
            System.out.println(ANSI_RED + "系统提示词不能为空" + ANSI_RESET);
            return;
        }

        systemPrompt.set(newPrompt);
        System.out.println(ANSI_GREEN + "系统提示词已更新。" + ANSI_RESET);
    }

    /**
     * 打印会话信息
     */
    private void showSessionInfo(String modelName, Map<String, AIModel> models, String systemPrompt, int messageCount) {
        AIModel model = models.get(modelName);
        if (model == null) {
            System.out.println(ANSI_RED + "无法获取模型信息" + ANSI_RESET);
            return;
        }

        ModelParameters params = model.getParameters();

        System.out.println("\n" + ANSI_YELLOW + "📊 当前会话信息:" + ANSI_RESET);
        System.out.println("  模型: " + modelName);
        System.out.println("  描述: " + model.description());
        System.out.println("  温度参数: " + params.temperature());
        System.out.println("  对话消息数: " + messageCount);
        System.out.println("  系统提示词: " + systemPrompt);
    }

    /**
     * 清除会话历史
     */
    private void clearConversation(List<Map<String, String>> history) {
        history.clear();
        System.out.println(ANSI_GREEN + "对话历史已清除。" + ANSI_RESET);
    }

    /**
     * 格式化响应文本，支持Markdown格式
     */
    private String formatResponse(String response) {
        // 添加一些简单的格式化，例如代码块
        StringBuilder formatted = new StringBuilder();
        String[] lines = response.split("\n");
        boolean inCodeBlock = false;
        String codeBlockType = "";

        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                // 提取代码块类型
                if (!inCodeBlock && line.trim().length() > 3) {
                    codeBlockType = line.trim().substring(3);
                }

                inCodeBlock = !inCodeBlock;
                if (inCodeBlock) {
                    formatted.append("\n┌─────────── Code");
                    if (!codeBlockType.isEmpty()) {
                        formatted.append(" (").append(codeBlockType).append(")");
                    }
                    formatted.append(" ───────────┐\n");
                } else {
                    formatted.append("\n└─────────────────────────────┘\n");
                    codeBlockType = "";
                }
            } else if (line.trim().startsWith("#") && !inCodeBlock) {
                // 处理Markdown标题
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') {
                    level++;
                }

                String title = line.substring(level).trim();
                String underline = level == 1 ? "=" : "-";

                formatted.append("\n").append(ANSI_BOLD).append(title).append(ANSI_RESET).append("\n");
                formatted.append(underline.repeat(title.length())).append("\n");
            } else if (line.trim().startsWith(">") && !inCodeBlock) {
                // 处理Markdown引用
                formatted.append("│ ").append(ANSI_ITALIC).append(line.substring(1)).append(ANSI_RESET).append("\n");
            } else if (line.trim().startsWith("- ") && !inCodeBlock) {
                // 处理Markdown列表
                formatted.append("• ").append(line.substring(2)).append("\n");
            } else if (line.trim().startsWith("* ") && !inCodeBlock) {
                // 处理Markdown列表（星号）
                formatted.append("• ").append(line.substring(2)).append("\n");
            } else if (inCodeBlock) {
                // 在代码块内
                formatted.append("│ ").append(line).append("\n");
            } else {
                // 普通文本
                formatted.append(line).append("\n");
            }
        }

        return formatted.toString();
    }
}