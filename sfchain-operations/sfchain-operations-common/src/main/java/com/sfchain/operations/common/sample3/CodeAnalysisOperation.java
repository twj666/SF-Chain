package com.sfchain.operations.common.codeanalysis;

import com.sfchain.core.annotation.AIOp;
import com.sfchain.core.exception.OperationException;
import com.sfchain.core.operation.BaseAIOperation;
import com.sfchain.core.operation.AIPromptBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static com.sfchain.core.constant.AIConstant.*;

/**
 * 代码分析Operation - 项目上下文构建专用
 * 专注于为AI提供项目理解的核心信息，极致压缩冗余
 *
 * @author suifeng
 * 日期: 2025/4/30
 */
@AIOp("code-analysis")
@Component
public class CodeAnalysisOperation extends BaseAIOperation<Map<String, Object>, String> {

    private static final String ANALYSIS_ROLE =
            "你是代码理解专家。提取类的核心信息用于构建项目上下文。" +
                    "输出必须极致简洁，每个词都有价值，专注于功能职责和关键实现。";

    @Override
    public List<String> supportedModels() {
        return List.of(SILI_QWEN, SILI_DEEP_SEEK_V3, SILI_THUDM);
    }

    @Override
    public String buildPrompt(Map<String, Object> params) {
        String javaCode = getStringValue(params, "javaCode", "");
        String fileName = getStringValue(params, "fileName", "Unknown.java");
        String packagePath = getStringValue(params, "packagePath", "");

        if (javaCode.trim().isEmpty()) {
            throw new OperationException("code-analysis", "Java代码内容不能为空");
        }

        AIPromptBuilder builder = createPromptBuilder("项目上下文构建");
        builder.addRole(ANALYSIS_ROLE);

        // 核心任务描述
        builder.addSection("任务",
                "分析Java类，输出项目上下文信息。目标是让陌生人快速理解类的作用和在项目中的位置。");

        // 代码元信息
        CodeMetadata metadata = extractMetadata(javaCode);
        builder.addSection("代码元信息", formatMetadata(metadata, fileName, packagePath));

        // 源代码
        builder.addSection("源代码", javaCode);

        // 输出格式严格定义
        builder.addSection("输出格式", buildStrictOutputFormat());

        return builder.build();
    }

    @Override
    public String parseResponse(String aiResponse) {
        // 直接返回AI的结构化响应，用于构建项目上下文
        return aiResponse.trim();
    }

    @Override
    public void validate(Map<String, Object> params) {
        if (params == null) {
            throw new OperationException("code-analysis", "参数不能为空");
        }

        String javaCode = getStringValue(params, "javaCode", "");
        if (javaCode.trim().isEmpty()) {
            throw new OperationException("code-analysis", "Java代码内容不能为空");
        }

        if (!isValidJavaCode(javaCode)) {
            throw new OperationException("code-analysis", "不是有效的Java代码");
        }
    }

    /**
     * 构建严格的输出格式 - 专为上下文构建优化
     */
    private String buildStrictOutputFormat() {
        return """
            严格按照以下格式输出，每部分控制在指定字数内：
            
            PURPOSE: [一句话说明类的核心作用，30字内]
            
            RESPONSIBILITY: [主要职责，30字内]
            
            KEY_METHODS: [核心方法名:功能，每个15字内，最多3个]
            - method1: 功能描述
            - method2: 功能描述
            - method3: 功能描述
            
            DEPENDENCIES: [主要依赖，20字内]
            
            PATTERN: [设计模式或架构特征，15字内]
            
            CONTEXT_VALUE: [在项目中的价值定位，20字内]
            
            注意：
            1. 严格控制字数，超出无效
            2. 使用技术术语，避免冗长描述
            3. 突出最核心的信息点
            4. 如果某部分不适用，写"无"
            """;
    }

    /**
     * 提取代码元数据
     */
    private CodeMetadata extractMetadata(String javaCode) {
        CodeMetadata metadata = new CodeMetadata();

        // 提取类型和名称
        extractClassInfo(javaCode, metadata);

        // 统计方法数量
        metadata.methodCount = countMethods(javaCode);

        // 检测技术特征
        detectTechnicalFeatures(javaCode, metadata);

        // 评估复杂度
        metadata.complexity = assessComplexity(javaCode);

        return metadata;
    }

    /**
     * 提取类信息
     */
    private void extractClassInfo(String javaCode, CodeMetadata metadata) {
        // 类
        Pattern classPattern = Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(javaCode);
        if (classMatcher.find()) {
            metadata.className = classMatcher.group(1);
            metadata.type = "Class";
        }

        // 接口
        Pattern interfacePattern = Pattern.compile("(?:public\\s+)?interface\\s+(\\w+)");
        Matcher interfaceMatcher = interfacePattern.matcher(javaCode);
        if (interfaceMatcher.find()) {
            metadata.interfaceName = interfaceMatcher.group(1);
            metadata.type = "Interface";
        }

        // 枚举
        Pattern enumPattern = Pattern.compile("(?:public\\s+)?enum\\s+(\\w+)");
        Matcher enumMatcher = enumPattern.matcher(javaCode);
        if (enumMatcher.find()) {
            metadata.enumName = enumMatcher.group(1);
            metadata.type = "Enum";
        }

        // 抽象类
        if (javaCode.contains("abstract class")) {
            metadata.isAbstract = true;
        }
    }

    /**
     * 统计方法数量
     */
    private int countMethods(String javaCode) {
        Pattern methodPattern = Pattern.compile("(?:public|private|protected)\\s+(?:static\\s+)?(?:\\w+\\s+)*\\w+\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = methodPattern.matcher(javaCode);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 检测技术特征
     */
    private void detectTechnicalFeatures(String javaCode, CodeMetadata metadata) {
        // Spring注解
        metadata.isSpringComponent = javaCode.contains("@Component") ||
                javaCode.contains("@Service") ||
                javaCode.contains("@Repository") ||
                javaCode.contains("@Controller");

        // 设计模式标识
        metadata.hasBuilder = javaCode.contains("Builder") || javaCode.contains("build()");
        metadata.hasFactory = javaCode.contains("Factory") || javaCode.contains("create");
        metadata.hasObserver = javaCode.contains("Observer") || javaCode.contains("Listener");
        metadata.hasStrategy = javaCode.contains("Strategy") || javaCode.contains("execute(");

        // 框架特征
        metadata.isDemo = javaCode.contains("Demo") || javaCode.contains("Example");
        metadata.isOperation = javaCode.contains("Operation") || javaCode.contains("@AIOp");
        metadata.isConfig = javaCode.contains("@Configuration") || javaCode.contains("Config");

        // 并发相关
        metadata.hasConcurrency = javaCode.contains("ExecutorService") ||
                javaCode.contains("CompletableFuture") ||
                javaCode.contains("@Async");
    }

    /**
     * 评估复杂度
     */
    private String assessComplexity(String javaCode) {
        int lines = javaCode.split("\n").length;
        if (lines < 50) return "简单";
        if (lines < 150) return "中等";
        if (lines < 300) return "复杂";
        return "高复杂";
    }

    /**
     * 格式化元数据
     */
    private String formatMetadata(CodeMetadata metadata, String fileName, String packagePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("文件: ").append(fileName);

        if (!packagePath.isEmpty()) {
            sb.append(" | 包: ").append(packagePath);
        }

        sb.append(" | 类型: ").append(metadata.type);

        if (metadata.className != null) {
            sb.append(" | 类名: ").append(metadata.className);
        }

        sb.append(" | 方法数: ").append(metadata.methodCount);
        sb.append(" | 复杂度: ").append(metadata.complexity);

        // 技术特征
        if (metadata.isSpringComponent) sb.append(" | Spring组件");
        if (metadata.isOperation) sb.append(" | AI操作");
        if (metadata.isDemo) sb.append(" | 演示类");
        if (metadata.hasConcurrency) sb.append(" | 并发处理");

        return sb.toString();
    }

    /**
     * 验证Java代码有效性
     */
    private boolean isValidJavaCode(String code) {
        return code.contains("class ") ||
                code.contains("interface ") ||
                code.contains("enum ") ||
                (code.contains("public ") && code.contains("{"));
    }

    /**
     * 代码元数据类
     */
    private static class CodeMetadata {
        String type = "Unknown";
        String className;
        String interfaceName;
        String enumName;
        int methodCount = 0;
        String complexity = "未知";
        boolean isAbstract = false;
        boolean isSpringComponent = false;
        boolean hasBuilder = false;
        boolean hasFactory = false;
        boolean hasObserver = false;
        boolean hasStrategy = false;
        boolean isDemo = false;
        boolean isOperation = false;
        boolean isConfig = false;
        boolean hasConcurrency = false;
    }
}