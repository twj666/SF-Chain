package com.example.common.ainode;

import com.example.common.ainode.dto.AiAnnotationAnalysisInput;
import com.example.common.ainode.dto.AiAnnotationAnalysisOutput;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@AIOp(
        value = AINodeConstant.ANNOTATION_ANALYSIS_OP,
        description = "AI批注分析操作，分析文章内容并生成批注建议"
)
public class AnnotationAnalysisOperation extends BaseAIOperation<AiAnnotationAnalysisInput, AiAnnotationAnalysisOutput> {

    private static final String LOCAL_PROMPT_TEMPLATE = """
            你是一位资深的{{ input.styleDisplayName }}内容策划师，拥有5年以上的内容创作和优化经验。
            请对以下文案进行专业分析，并提供具体可执行的改进建议。

            === 待分析文案 ===
            {{ input.content ?: '' }}
            === 文案结束 ===

            === 分析任务 ===
            • 平台风格：{{ input.styleDisplayName }}
            • 分析重点：{{ input.analysisTypeDisplayName }}
            • 分析深度：{{ fn.defaultValue(input.analysisDepth, '标准') }}
            • 文章ID：{{ fn.defaultValue(input.articleId, '') }}
            • 覆盖现有批注：{{ fn.defaultValue(input.overrideExisting, false) }}
            {{#if input.styleFallback}}
            • 注意：输入内容风格无法识别，已按“抖音”标准分析
            {{/if}}
            {{#if input.analysisTypeFallback}}
            • 注意：输入分析类型无法识别，已按“全面分析”处理
            {{/if}}

            === 平台特征与标准 ===
            {{ input.styleCharacteristics }}

            === 本次分析重点 ===
            {{ input.analysisTypeFocus }}

            输出 JSON（必须严格遵守）：
            {
              "overallScore": 评分(0-100),
              "analysisSummary": "整体评价",
              "suggestions": [
                {
                  "selectedText": "原文中的连续文字片段",
                  "content": "具体建议（问题 -> 改法 -> 预期效果，必要时给出可替换表达）",
                  "type": "OPTIMIZATION|LOGIC|STYLE",
                  "severity": "HIGH|MEDIUM|LOW",
                  "color": "#FF6B6B|#4ECDC4|#45B7D1"
                }
              ]
            }

            严格执行要求：
            1. selectedText 必须是原文中连续存在的文字片段
               - 长度控制在5-50字之间
               - 优先选择关键句子或短语
               - 避免选择单个字符或过长段落
            2. 每条建议必须具体、可执行，避免空泛建议
            3. 建议数量控制在 5-10 条，优先高价值问题
            4. severity 判定：
               - HIGH：显著影响理解或传播效果
               - MEDIUM：影响体验和质量
               - LOW：可优化但不影响主线
            5. overallScore 评分规则：
               - 90-100：优秀，仅需微调
               - 80-89：良好，有优化空间
               - 70-79：一般，建议重点优化
               - 60-69：较弱，需明显改写
               - <60：较差，建议重构
            6. 只输出 JSON，不得输出任何额外说明文字""";

    @Override
    public String promptTemplate() {
        return LOCAL_PROMPT_TEMPLATE;
    }

    @Override
    protected Map<String, Object> buildPromptInputExtensions(AiAnnotationAnalysisInput input) {
        ParseResult<ContentStyle> styleParse = parseContentStyle(input.getContentStyle());
        ParseResult<AnalysisType> typeParse = parseAnalysisType(input.getAnalysisType());
        ContentStyle contentStyle = styleParse.value;
        AnalysisType analysisType = typeParse.value;

        Map<String, Object> inputExtensions = new LinkedHashMap<>();
        inputExtensions.put("styleDisplayName", contentStyle.getDisplayName());
        inputExtensions.put("analysisTypeDisplayName", analysisType.getDisplayName());
        inputExtensions.put("styleCharacteristics", STYLE_CHARACTERISTICS.getOrDefault(contentStyle, "请根据指定的文案风格进行分析。"));
        inputExtensions.put("analysisTypeFocus", getAnalysisTypeFocus(contentStyle, analysisType));
        inputExtensions.put("styleFallback", styleParse.fallback);
        inputExtensions.put("analysisTypeFallback", typeParse.fallback);
        return inputExtensions;
    }

    public enum ContentStyle {
        DOUYIN("抖音"),
        XIAOHONGSHU("小红书"),
        ZHIHU("知乎"),
        WEIBO("微博");

        private final String displayName;

        ContentStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AnalysisType {
        OPTIMIZATION("优化建议"),
        LOGIC("逻辑问题"),
        STYLE("文风问题"),
        ALL("全面分析");

        private final String displayName;

        AnalysisType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final Map<ContentStyle, String> STYLE_CHARACTERISTICS = Map.of(
            ContentStyle.DOUYIN, """
                    抖音文案核心要求：
                    ✓ 开头3秒抓眼球：用悬念、反转、数据震撼开场
                    ✓ 节奏感强：短句为主，每15-20字一个停顿点
                    ✓ 口语化但不低俗：像朋友聊天，但保持专业度
                    ✓ 互动性设计：设置提问、投票、挑战等互动点
                    ✓ 情绪价值：让人有"学到了""太对了""我也是"的感受
                    ✓ 记忆点设计：金句、口号、重复强调的关键词
                    ✓ 长度控制：2200-2800字，对应5-8分钟视频
                    """,
            ContentStyle.XIAOHONGSHU, """
                    小红书文案核心要求：
                    ✓ 真实感第一：避免过度营销，像真实分享
                    ✓ 视觉化描述：用具体的场景、颜色、质感描述
                    ✓ 情感共鸣：戳中目标用户的痛点和需求
                    ✓ 实用价值：提供可复制的方法、清单、步骤
                    ✓ 标签运用：合理使用话题标签增加曝光
                    ✓ 种草逻辑：需求场景→产品体验→使用效果→推荐理由
                    ✓ 长度控制：800-1500字，配图文展示
                    """,
            ContentStyle.ZHIHU, """
                    知乎文案核心要求：
                    ✓ 专业权威：展示专业知识和深度思考
                    ✓ 逻辑严密：观点→论据→论证→结论的完整链条
                    ✓ 数据支撑：用具体数据、案例、研究支持观点
                    ✓ 结构清晰：使用小标题、序号、分点论述
                    ✓ 客观理性：避免情绪化表达，保持中立客观
                    ✓ 价值输出：解决实际问题，提供新的认知
                    ✓ 长度控制：2000-5000字，深度长文
                    """,
            ContentStyle.WEIBO, """
                    微博文案核心要求：
                    ✓ 简洁有力：核心观点一句话说清楚
                    ✓ 热点结合：结合当下热点话题增加传播
                    ✓ 情绪表达：有明确的态度和立场
                    ✓ 互动设计：引发评论、转发、讨论
                    ✓ 传播性强：容易被转发和二次传播
                    ✓ 话题性：能引发争议或共鸣的话题点
                    ✓ 长度控制：140-280字，精炼表达
                    """
    );

    private static final Map<ContentStyle, Map<AnalysisType, String>> ANALYSIS_FOCUS_MAP = Map.of(
            ContentStyle.DOUYIN, Map.of(
                    AnalysisType.OPTIMIZATION, "重点检查：开头吸引力、节奏控制、互动设计、记忆点打造、情绪价值体现",
                    AnalysisType.LOGIC, "重点检查：论证逻辑、因果关系、信息准确性、前后一致性",
                    AnalysisType.STYLE, "重点检查：口语化程度、平台调性匹配、语言感染力、表达自然度",
                    AnalysisType.ALL, "全面分析：内容价值、表达效果、平台适配、传播潜力四个维度"
            ),
            ContentStyle.XIAOHONGSHU, Map.of(
                    AnalysisType.OPTIMIZATION, "重点检查：真实感营造、实用价值体现、种草逻辑、视觉化描述效果",
                    AnalysisType.LOGIC, "重点检查：推荐逻辑、使用场景合理性、效果描述真实性",
                    AnalysisType.STYLE, "重点检查：分享语调、亲和力、标签使用、情感共鸣度",
                    AnalysisType.ALL, "全面分析：内容真实性、实用价值、情感连接、传播效果四个维度"
            ),
            ContentStyle.ZHIHU, Map.of(
                    AnalysisType.OPTIMIZATION, "重点检查：专业深度、论证充分性、结构清晰度、价值输出",
                    AnalysisType.LOGIC, "重点检查：逻辑严密性、论证链条完整性、数据支撑有效性",
                    AnalysisType.STYLE, "重点检查：客观理性度、专业表达、语言准确性",
                    AnalysisType.ALL, "全面分析：专业权威性、逻辑严密性、表达质量、价值贡献四个维度"
            ),
            ContentStyle.WEIBO, Map.of(
                    AnalysisType.OPTIMIZATION, "重点检查：观点鲜明度、热点结合、传播潜力、互动设计",
                    AnalysisType.LOGIC, "重点检查：观点支撑、论据充分性、逻辑自洽性",
                    AnalysisType.STYLE, "重点检查：情绪表达、态度鲜明度、语言冲击力",
                    AnalysisType.ALL, "全面分析：观点价值、表达力度、传播效果、互动潜力四个维度"
            )
    );

    private String getAnalysisTypeFocus(ContentStyle contentStyle, AnalysisType analysisType) {
        return ANALYSIS_FOCUS_MAP
                .getOrDefault(contentStyle, Map.of())
                .getOrDefault(analysisType, "请进行相应的分析。");
    }

    private ParseResult<ContentStyle> parseContentStyle(String styleStr) {
        try {
            return ParseResult.of(ContentStyle.valueOf(styleStr.toUpperCase()), false);
        } catch (Exception e) {
            log.warn("未知的内容风格: {}, 使用默认风格", styleStr);
            return ParseResult.of(ContentStyle.DOUYIN, true);
        }
    }

    private ParseResult<AnalysisType> parseAnalysisType(String typeStr) {
        try {
            return ParseResult.of(AnalysisType.valueOf(typeStr.toUpperCase()), false);
        } catch (Exception e) {
            log.warn("未知的分析类型: {}, 使用默认类型", typeStr);
            return ParseResult.of(AnalysisType.ALL, true);
        }
    }

    private static final class ParseResult<T> {
        private final T value;
        private final boolean fallback;

        private ParseResult(T value, boolean fallback) {
            this.value = value;
            this.fallback = fallback;
        }

        private static <T> ParseResult<T> of(T value, boolean fallback) {
            return new ParseResult<>(value, fallback);
        }
    }
}
