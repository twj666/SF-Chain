package com.example.common.ainode;

import com.example.common.ainode.dto.CopywritingGenerationInput;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.common.ainode.AINodeConstant.COPYWRITING_GENERATION_OP;

@Component
@AIOp(
        value = COPYWRITING_GENERATION_OP,
        description = "根据主题、描述和素材生成抖音科普文案",
        requireJsonOutput = false
)
public class CopywritingGenerationOperation extends BaseAIOperation<CopywritingGenerationInput, String> {

    private static final String LOCAL_PROMPT_TEMPLATE = """
            抖音科普文案创作

            你是一位擅长抖音科普的内容创作者，请为“{{ fn.defaultValue(input.topic, '未命名主题') }}”写一篇适合口播的科普文案。

            {{#if fn.present(input.description)}}
            项目描述：
            {{ input.description }}

            描述要求：
            1. 充分理解项目描述中的需求和背景信息
            2. 将描述中的要点自然融入到文案创作中
            3. 确保文案内容与项目描述的目标一致
            4. 保持描述信息的准确性，不要随意修改
            {{/if}}

            {{#if fn.present(input.materials)}}
            参考素材：
            {{ input.materials }}
            素材条目数：{{ fn.len(input.materialLines) }}

            素材清单：
            {{#each input.materialLines}}
            - {{ item }}
            {{/each}}

            素材使用要求：
            1. 充分利用上述素材中的信息、数据和观点
            2. 将素材内容自然融入到文案中，不要生硬堆砌
            3. 可以引用素材中的具体案例、数据或专家观点
            4. 保持素材信息的准确性，不要随意修改
            5. 巧妙引用素材中的视频内容、UP主观点或数据支撑
            {{/if}}

            {{#if input.wordLimit}}
            字数要求：
            • 目标字数：{{ input.wordLimit }}字（{{ input.wordLimitMin }}~{{ input.wordLimitMax }}字）
            • 在保证内容质量的前提下，精确控制篇幅
            • 如果内容不足，可以适当增加细节描述或案例
            • 如果内容过多，请优化表达，去除冗余信息
            • 每段字数要均匀分布，避免某段过长或过短
            {{/if}}

            {{#if fn.present(input.additionalRequirements)}}
            特殊要求：
            {{ input.additionalRequirements }}

            要求执行说明：
            1. 严格按照用户的特殊要求执行，这是最高优先级
            2. 在满足特殊要求的同时，保持文案的科学性和可读性
            3. 如果特殊要求与基本创作理念冲突，优先满足特殊要求
            4. 将特殊要求自然融入到文案中，不要生硬执行
            5. 确保特殊要求的执行不影响整体文案的流畅性
            {{/if}}

            创作理念：
            1. 像聊天一样 - 想象你在和朋友面对面聊天，语调轻松自然
            2. 科学严谨 - 保证所有科学内容准确可靠，不能为了有趣而牺牲准确性
            3. 抓人眼球 - 每个转折都要让观众想继续听下去
            4. 口语化表达 - 符合说话习惯，朗读起来顺畅自然

            内容设计：
            • 开头直接抛出让人意外的问题或现象，瞬间抓住注意力
            • 用“你知道吗”“其实”“但是”等词汇自然过渡段落
            • 每段都要有新信息或新角度，避免重复和冗余
            • 穿插“想象一下”“比如说”等引导性词语增强代入感
            • 适当设置“那么问题来了”“更神奇的是”等悬念点
            • 结尾要有惊喜感或启发性，让人有分享的冲动
            {{#if fn.present(input.materials)}}
            • 巧妙结合素材中的具体案例和数据，增强说服力
            {{/if}}

            语言风格：
            • 多用“你”来称呼观众，营造对话感
            • 句式灵活变化：长短句结合，疑问句、感叹句穿插使用
            • 用数字和对比制造冲击感
            • 减少书面语，多用口语表达
            • 关键信息用简短有力的句子表达
            • 每段最后一句要为下段做铺垫，确保衔接自然

            抖音特色：
            • 节奏感强，信息密度高但不烧脑
            • 有互动性，让观众产生“原来如此”的感觉
            • 制造小高潮，每隔几句话就有一个让人眼前一亮的点
            • 语言生动但不夸张，保持可信度
            记住：你的目标是让观众感觉在听一个有趣的朋友分享知识，而不是在上课。

            段落要求：
            {{#if input.wordLimit}}
            • 建议分为{{ input.paragraphMin }}-{{ input.paragraphMax }}个段落，每段约{{ input.wordsPerParagraphMin }}-{{ input.wordsPerParagraphMax }}字
            {{else}}
            • 每个段落应包含6~10个句子，形成完整的表达单元
            • 控制段落数量在8-12段之间，每段150~300字
            {{/if}}
            • 避免单句成段，确保每段都有充实的内容
            • 每段应围绕一个核心观点或话题展开
            • 段落内句子要逻辑连贯，层层递进
            • 每段结尾要自然引出下段内容，避免生硬跳转
            {{#if input.wordLimit}}
            • 篇幅控制：严格控制在{{ input.wordLimit }}字左右，适合{{ input.estimatedMinutesMin }}-{{ input.estimatedMinutesMax }}分钟口播
            {{else}}
            • 篇幅控制：2000~2500字，适合5~8分钟口播
            {{/if}}

            格式要求：
            • 文章只需要一个主标题，使用“# 标题”格式
            • 正文内容为纯文本段落，不使用任何Markdown格式
            • 不要使用加粗、斜体、列表、表格等任何格式化元素
            • 每段内容要完整流畅，适合朗读
            • 整篇文章只包含一个标题和若干段落的纯文本
            • 严格控制段落数量，避免过度分段

            输出要求：
            1. 只输出最终文案，不要解释创作过程
            2. 保持科学性、连贯性和可读性""";

    @Override
    public String promptTemplate() {
        return LOCAL_PROMPT_TEMPLATE;
    }

    @Override
    protected Map<String, Object> buildPromptInputExtensions(CopywritingGenerationInput input) {
        if (!StringUtils.hasText(input.getTopic())) {
            throw new IllegalArgumentException("主题不能为空");
        }
        String materials = input.getMaterials();
        Integer wordLimit = input.getWordLimit();
        boolean hasWordLimit = wordLimit != null && wordLimit > 0;

        int safeWordLimit = hasWordLimit ? wordLimit : 2200;
        int suggestedParagraphs = Math.max(6, Math.min(12, safeWordLimit / 200));
        int wordsPerParagraph = safeWordLimit / suggestedParagraphs;
        int estimatedMinutes = Math.max(3, Math.min(10, safeWordLimit / 300));

        Map<String, Object> inputExtensions = new LinkedHashMap<>();
        inputExtensions.put("materialLines", toMaterialLines(materials));
        inputExtensions.put("wordLimit", safeWordLimit);
        inputExtensions.put("wordLimitMin", Math.max(100, safeWordLimit - 50));
        inputExtensions.put("wordLimitMax", safeWordLimit + 50);
        inputExtensions.put("paragraphMin", Math.max(1, suggestedParagraphs - 2));
        inputExtensions.put("paragraphMax", suggestedParagraphs + 2);
        inputExtensions.put("wordsPerParagraphMin", Math.max(20, wordsPerParagraph - 30));
        inputExtensions.put("wordsPerParagraphMax", wordsPerParagraph + 30);
        inputExtensions.put("estimatedMinutesMin", Math.max(1, estimatedMinutes - 1));
        inputExtensions.put("estimatedMinutesMax", estimatedMinutes + 1);
        return inputExtensions;
    }

    private List<String> toMaterialLines(String materials) {
        if (!StringUtils.hasText(materials)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String raw : materials.split("\\r?\\n")) {
            String line = raw == null ? "" : raw.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            lines.add(materials.trim());
        }
        return lines;
    }
}
