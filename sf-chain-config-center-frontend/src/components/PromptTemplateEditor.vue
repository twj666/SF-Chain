<template>
  <div class="prompt-template-editor" :class="{ disabled }">
    <div v-if="badgeText" class="editor-badge">{{ badgeText }}</div>
    <div ref="editorRoot" class="editor-root"></div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { EditorState, RangeSetBuilder, type Extension } from '@codemirror/state'
import { EditorView, Decoration, ViewPlugin, type ViewUpdate } from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { keymap } from '@codemirror/view'
import { PROMPT_TEMPLATE_FUNCTION_NAMES } from '@/constants/promptTemplateBuiltins'

const props = withDefaults(defineProps<{
  modelValue: string
  disabled?: boolean
  badgeText?: string
}>(), {
  disabled: false,
  badgeText: 'Template Editor'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editorRoot = ref<HTMLDivElement | null>(null)
let editorView: EditorView | null = null

const BUILTIN_FUNCTIONS = new Set(PROMPT_TEMPLATE_FUNCTION_NAMES)

const templateHighlightPlugin = ViewPlugin.fromClass(class {
  decorations

  constructor(view: EditorView) {
    this.decorations = buildDecorations(view)
  }

  update(update: ViewUpdate) {
    if (update.docChanged || update.viewportChanged) {
      this.decorations = buildDecorations(update.view)
    }
  }
}, {
  decorations: (value) => value.decorations
})

function buildDecorations(view: EditorView) {
  const builder = new RangeSetBuilder<Decoration>()
  const text = view.state.doc.toString()
  const regex = /\{\{([\s\S]*?)}}/g
  let match: RegExpExecArray | null

  while ((match = regex.exec(text)) !== null) {
    const fullMatch = match[0]
    const rawExpression = match[1] ?? ''
    const start = match.index
    const end = start + fullMatch.length
    const exprStart = start + 2
    const exprEnd = end - 2

    builder.add(start, start + 2, Decoration.mark({ class: 'cm-template-brace' }))
    if (rawExpression.trim()) {
      builder.add(exprStart, exprEnd, Decoration.mark({ class: 'cm-template-expression' }))
    }

    const trimmed = rawExpression.trim()
    const leftPadding = rawExpression.indexOf(trimmed)
    const expressionOffset = exprStart + (leftPadding < 0 ? 0 : leftPadding)

    if (/^#(if|each)\b/.test(trimmed) || /^\/(if|each)\b/.test(trimmed) || trimmed === 'else') {
      builder.add(expressionOffset, expressionOffset + trimmed.length, Decoration.mark({ class: 'cm-template-block' }))
    }

    const fnRegex = /\bfn\.([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g
    let fnMatch: RegExpExecArray | null
    while ((fnMatch = fnRegex.exec(rawExpression)) !== null) {
      const fnName = fnMatch[1]
      const fnPrefixStart = exprStart + fnMatch.index
      const fnPrefixEnd = fnPrefixStart + 3
      const fnCallOffset = fnMatch.index + fnMatch[0].indexOf(fnName)
      const fnStart = exprStart + fnCallOffset
      const fnEnd = fnStart + fnName.length
      const className = BUILTIN_FUNCTIONS.has(fnName) ? 'cm-template-fn' : 'cm-template-fn-error'
      builder.add(fnPrefixStart, fnPrefixEnd, Decoration.mark({ class: 'cm-template-fn-prefix' }))
      builder.add(fnStart, fnEnd, Decoration.mark({ class: className }))
    }

    builder.add(end - 2, end, Decoration.mark({ class: 'cm-template-brace' }))
  }

  return builder.finish()
}

const editorTheme = EditorView.theme({
  '&': {
    fontSize: '13px',
    border: '1px solid rgba(209, 213, 219, 0.8)',
    borderRadius: '10px',
    backgroundColor: '#ffffff',
    height: '100%'
  },
  '&.cm-focused': {
    outline: 'none',
    borderColor: '#3b82f6',
    boxShadow: '0 0 0 3px rgba(59, 130, 246, 0.12)'
  },
  '.cm-scroller': {
    fontFamily: '"JetBrains Mono", "SFMono-Regular", "Consolas", monospace',
    lineHeight: '1.58'
  },
  '.cm-content': {
    padding: '12px 14px',
    minHeight: '100%'
  },
  '.cm-line': {
    padding: '0'
  },
  '.cm-template-brace': {
    color: '#16a34a',
    fontWeight: '700'
  },
  '.cm-template-expression': {
    color: '#14532d',
    backgroundColor: 'rgba(34, 197, 94, 0.16)',
    boxShadow: 'inset 0 -1px 0 rgba(22, 163, 74, 0.35)',
    borderRadius: '4px'
  },
  '.cm-template-block': {
    color: '#0f766e',
    fontWeight: '700'
  },
  '.cm-template-fn': {
    color: '#075985',
    backgroundColor: 'rgba(14, 165, 233, 0.22)',
    boxShadow: 'inset 0 -1px 0 rgba(2, 132, 199, 0.45)',
    borderRadius: '4px',
    fontWeight: '800',
    textDecoration: 'underline rgba(7, 89, 133, 0.9) 1.5px'
  },
  '.cm-template-fn-prefix': {
    color: '#0c4a6e',
    fontWeight: '700',
    opacity: '0.95'
  },
  '.cm-template-fn-error': {
    color: '#dc2626',
    backgroundColor: 'rgba(239, 68, 68, 0.3)',
    borderRadius: '4px',
    fontWeight: '800',
    textDecoration: 'underline wavy #dc2626 2px'
  },
  '.cm-selectionBackground': {
    backgroundColor: 'rgba(59, 130, 246, 0.2) !important'
  }
})

function createExtensions(): Extension[] {
  const extensions: Extension[] = [
    EditorView.lineWrapping,
    history(),
    keymap.of([...historyKeymap, ...defaultKeymap]),
    templateHighlightPlugin,
    editorTheme,
    EditorView.updateListener.of((update) => {
      if (!update.docChanged) {
        return
      }
      emit('update:modelValue', update.state.doc.toString())
    })
  ]

  if (props.disabled) {
    extensions.push(EditorState.readOnly.of(true))
    extensions.push(EditorView.editable.of(false))
  }

  return extensions
}

onMounted(() => {
  if (!editorRoot.value) {
    return
  }
  editorView = new EditorView({
    parent: editorRoot.value,
    state: EditorState.create({
      doc: props.modelValue ?? '',
      extensions: createExtensions()
    })
  })
})

watch(
  () => props.modelValue,
  (nextValue) => {
    if (!editorView) {
      return
    }
    const current = editorView.state.doc.toString()
    if (nextValue === current) {
      return
    }
    editorView.dispatch({
      changes: {
        from: 0,
        to: current.length,
        insert: nextValue ?? ''
      }
    })
  }
)

watch(
  () => props.disabled,
  () => {
    if (!editorView) {
      return
    }
    const currentDoc = editorView.state.doc.toString()
    editorView.setState(EditorState.create({
      doc: currentDoc,
      extensions: createExtensions()
    }))
  }
)

onBeforeUnmount(() => {
  editorView?.destroy()
  editorView = null
})
</script>

<style scoped>
.prompt-template-editor {
  width: 100%;
  position: relative;
  height: 100%;
  min-height: 0;
}

.prompt-template-editor.disabled {
  opacity: 0.75;
}

.editor-root {
  width: 100%;
  height: 100%;
  min-height: 0;
}

:deep(.cm-scroller) {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.62) transparent;
}

:deep(.cm-scroller::-webkit-scrollbar) {
  width: 7px;
  height: 7px;
}

:deep(.cm-scroller::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb) {
  background: rgba(148, 163, 184, 0.52);
  border-radius: 999px;
}

:deep(.cm-scroller::-webkit-scrollbar-thumb:hover) {
  background: rgba(100, 116, 139, 0.72);
}

.editor-badge {
  position: absolute;
  top: -8px;
  right: 10px;
  z-index: 2;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  color: #0f766e;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}
</style>
