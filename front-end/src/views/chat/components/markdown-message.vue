<script setup lang="ts">
import { MdPreview } from 'md-editor-v3'

defineProps<{ message: string }>()
</script>

<template>
  <!--  markdown显示消息-->
  <md-preview id="preview-only" :model-value="message" preview-theme="vuepress"></md-preview>
</template>

<style scoped lang="scss">
// 调整markdown组件的一些样式
:deep(.md-editor-preview-wrapper) {
  padding: 0 10px;
  background-color: #1e1e1e;
  color: #ffffff;

  .vuepress-theme {
    color: #ffffff;

    p,
    h1,
    h2,
    h3,
    h4,
    h5,
    h6,
    li,
    table {
      color: #ffffff;
    }

    a {
      color: #58a6ff;
    }

    blockquote {
      color: #ffffff;
      border-left-color: #ffffff;
    }

    p {
      line-height: unset;
    }

    code {
      background-color: #2d2d2d;
      color: #e6e6e6;
    }

    pre {
      background-color: #2d2d2d;
    }
  }
}
</style>
