package com.example.common.ainode;

import com.example.domin.dto.AiModificationResponse;
import com.example.common.ainode.dto.ContentModificationInput;
import com.suifeng.sfchain.annotation.AIOp;
import com.suifeng.sfchain.core.BaseAIOperation;
import org.springframework.stereotype.Component;

import static com.example.common.ainode.AINodeConstant.CONTENT_MODIFICATION_OP;

/**
 * 内容修改AI操作
 * 根据批注内容修改文档
 */
@Component
@AIOp(
    value = CONTENT_MODIFICATION_OP,
    description = "根据批注内容修改文档"
)
public class ContentModificationOperation extends BaseAIOperation<ContentModificationInput, AiModificationResponse> {

    private static final String LOCAL_PROMPT_TEMPLATE = """
            你是一个专业的文档编辑助手，擅长根据批注内容精准修改文档。
            你的输出会被程序直接用于替换原文行内容，因此必须严格遵守格式和完整性要求。

            原始文档内容：
            ```
            {{ fn.defaultValue(input.originalText, '') }}
            ```

            批注信息：
            批注内容：{{ fn.defaultValue(input.annotationContent, '') }}
            选中文本：{{ fn.defaultValue(input.selectedText, '') }}
            保持原始风格：{{ fn.defaultValue(input.keepOriginalStyle, false) }}

            修改要求：
            1. 请基于批注内容和选中的文本位置，确定需要修改的行
            2. 保持文章的整体风格和语调
            3. 只修改必要的行，避免过度修改
            4. 重点关注批注选中的文本区域及其相关内容
            {{#if input.keepOriginalStyle}}
            5. 请保持原文的写作风格和表达方式
            {{/if}}

            重要说明：
            • content字段必须包含该行的完整内容，不能只包含修改的部分！
            • 即使只修改了一个词，也要返回整句话的完整内容！
            • 我会用content的内容完全替换原来的行，所以绝对不能缺少任何部分！

            关键要求：
            ✓ reason字段要简洁明了，说明本次修改的整体目标，字数为20~200字
            ✓ content必须是完整的行内容，包含修改前后的所有文字
            ✓ 不要只返回修改的片段，要返回整行的完整句子
            ✓ 确保修改后的内容在语法和逻辑上完整连贯
            ✓ 只返回需要修改的行，不修改的行不要包含在结果中
            ✓ line字段必须对应原文档中真实存在的行号
            ✓ 优先修改包含批注选中文本的行及其上下文相关行

            请返回 JSON：
            {
              "reason": "本次修改的总体原因和目标",
              "changes": [
                {
                  "line": "行号（必须是文档中存在的行号）",
                  "content": "该行修改后的完整内容（必须是完整的一行，不能只是片段）"
                }
              ]
            }""";

    @Override
    public String promptTemplate() {
        return LOCAL_PROMPT_TEMPLATE;
    }
}
