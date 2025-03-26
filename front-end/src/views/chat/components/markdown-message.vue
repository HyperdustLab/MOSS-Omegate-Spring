<script setup lang="ts">
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'

import MarkdownRender from 'vue-markdown-render'

defineProps<{ message: string }>()

const markdownStyle = {
  '--md-font-family': '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  '--md-color': '#ffffff',
  '--md-font-size': '14px',
  '--md-line-height': '1.8',
}
</script>

<template>
  <div class="custom-markdown">
    <MarkdownRender :source="message" />
    <!-- <md-preview id="preview-only" :model-value="message" preview-theme="vuepress" :style="markdownStyle" /> -->
  </div>
</template>

<style>
/* 使用全局样式确保覆盖 */
.custom-markdown {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
}

.custom-markdown .md-editor-preview-wrapper {
  background-color: #1e1e1e !important;
  color: #ffffff !important;
}

.custom-markdown .md-editor-preview-wrapper * {
  font-family: inherit !important;
  line-height: 1.8 !important;
}

.custom-markdown .md-editor-preview-wrapper p {
  font-size: 14px !important;
  margin: 1em 0 !important;
  color: #ffffff !important;
  word-spacing: 0.05em !important;
  letter-spacing: 0.01em !important;
}

.custom-markdown .md-editor-preview-wrapper code {
  font-family: Consolas, Monaco, 'Courier New', monospace !important;
  background-color: #2d2d2d !important;
  padding: 0.2em 0.4em !important;
  border-radius: 3px !important;
  font-size: 0.9em !important;
}

/* 确保链接颜色正确 */
.custom-markdown .md-editor-preview-wrapper a {
  color: #58a6ff !important;
}

/* 确保列表样式正确 */
.custom-markdown .md-editor-preview-wrapper ul,
.custom-markdown .md-editor-preview-wrapper ol {
  padding-left: 2em !important;
  margin: 1em 0 !important;
}

/* 确保引用样式正确 */
.custom-markdown .md-editor-preview-wrapper blockquote {
  border-left: 4px solid #666 !important;
  margin: 1em 0 !important;
  padding: 0.5em 1em !important;
  background-color: rgba(255, 255, 255, 0.05) !important;
}

/* 添加文本平滑 */
.custom-markdown * {
  -webkit-font-smoothing: antialiased !important;
  -moz-osx-font-smoothing: grayscale !important;
}
</style>
