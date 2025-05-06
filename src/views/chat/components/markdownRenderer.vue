<template>
  <div class="markdown-body" v-html="compiledMarkdown"></div>
</template>

<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // 引入代码高亮的样式

// 定义 props
const props = defineProps({
  content: {
    type: String,
    required: true,
  },
})

const md = new MarkdownIt({
  html: true,
  linkify: true, // 将URL转换为可点击的链接
  typographer: true, // 启用一些语言中性的替换和引号美化
  breaks: true, // 将换行符转换为 <br>
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' + hljs.highlight(str, { language: lang, ignoreIllegals: true }).value + '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const compiledMarkdown = computed(() => md.render(props.content))
</script>

<style>
.markdown-body {
  /* 基础样式 */
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
  font-size: 16px;
  line-height: 1.6;
  word-wrap: break-word;
  padding: 16px;

  /* 代码块样式 */
  pre.hljs {
    padding: 16px;
    overflow: auto;
    border-radius: 6px;
    background-color: #f6f8fa;
    margin-bottom: 16px;
  }

  /* 行内代码样式 */
  code:not(.hljs) {
    padding: 0.2em 0.4em;
    margin: 0;
    font-size: 85%;
    background-color: rgba(175, 184, 193, 0.2);
    border-radius: 6px;
  }

  /* 链接样式 */
  a {
    color: #0366d6;
    text-decoration: none;
  }

  a:hover {
    text-decoration: underline;
  }

  /* 标题样式 */
  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin-top: 24px;
    margin-bottom: 16px;
    font-weight: 600;
    line-height: 1.25;
  }

  /* 引用块样式 */
  blockquote {
    padding: 0 1em;
    color: #57606a;
    border-left: 0.25em solid #d0d7de;
    margin: 16px 0;
  }
}
</style>
