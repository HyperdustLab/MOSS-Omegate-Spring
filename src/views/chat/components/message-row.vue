<script lang="ts" setup>
import { Picture, Warning, Share } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'
import TextLoading from './text-loading.vue'
import MarkdownMessage from './markdown-message.vue'
import type { AiMessage } from '../store/chat-store'
import ThoughtChain from './ThoughtChain.vue'

const BASE_URL = import.meta.env.VITE_API_HYPERAGI_API
const downloading = ref(false)

const props = defineProps<{
  message: AiMessage
  defAgentAvatar: string
}>()

const userAvatar = 'https://s3.hyperdust.io/upload/20250411/67f8cbcbe4b0bc355fbb060e.png'

const agentAvatar = 'https://s3.hyperdust.io/upload/20250416/67ff421d5bce8066f1e25655.jpg'

const isUser = computed(() => props.message.type === 'USER')

const avatar = computed(() => {
  return isUser.value ? userAvatar : props.defAgentAvatar || agentAvatar
})

const images = computed(() => {
  if (!props.message.medias) return []
  return props.message.medias.filter((media) => media.type === 'image').map((media) => media.data)
})

const handleDownload = async (imageUrl: string) => {
  try {
    downloading.value = true
    const objectId = imageUrl.replace('https://s3.hyperdust.io/', '')

    // 获取图片数据
    const response = await fetch(BASE_URL + '/sys/common/download?objectId=' + objectId)
    const blob = await response.blob()

    // 创建下载链接
    const link = document.createElement('a')
    const url = window.URL.createObjectURL(blob)
    link.href = url

    // 设置下载文件名
    const fileName = imageUrl.split('/').pop() || 'image.png'
    link.download = fileName

    // 添加到文档中并触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Download failed:', error)
  } finally {
    downloading.value = false
  }
}
</script>

<template>
  <div class="message-row" :class="{ 'message-row-user': isUser }">
    <div class="avatar">
      <el-avatar :size="40" :src="message.avatar || avatar" />
    </div>
    <div class="message-content">
      <ThoughtChain v-if="message.thinkingList && message.thinkingList.length > 0" :items="message.thinkingList" />

      <div class="message-text mt-5" v-html="message.textContent"></div>

      <div class="image-container" v-if="images && images.length > 0">
        <div v-for="(image, index) in images" :key="index" class="image-wrapper">
          <el-image class="image" fit="cover" style="width: 50%; height: 50%" :preview-src-list="images" :src="image">
            <template #placeholder>
              <div class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
            <template #error>
              <div class="image-error">
                <el-icon><Warning /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="image-actions">
            <el-button size="small" type="primary" :loading="downloading" @click="handleDownload(image)">
              <template #icon>
                <el-icon><Share /></el-icon>
              </template>
              <span>Download & Share</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.message-row {
  display: flex;
  margin-bottom: 16px;
  padding: 0 12px;

  &.message-row-user {
    flex-direction: row-reverse;
  }

  .avatar {
    margin: 0 8px;
  }

  .message-content {
    max-width: 85%;
    background-color: #2b2b2b;
    padding: 8px 12px;
    border-radius: 8px;
    color: white;
    word-break: break-word;
  }
}

.image-container {
  position: relative;
  display: inline-block;
  margin-top: 8px;
  width: 100%;
}

.image-wrapper {
  position: relative;
  display: inline-block;
  margin-top: 8px;
  width: 100%;
}

.image-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
  z-index: 1;
  display: flex;
  background: rgba(0, 0, 0, 0.5);
  padding: 4px;
  border-radius: 4px;

  .el-button {
    background-color: transparent;
    border: none;
    color: white;
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    padding: 4px 8px;

    &:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }
  }
}

// 响应式样式
@media screen and (max-width: 768px) {
  .message-row {
    margin-bottom: 12px;
    padding: 0 8px;

    .avatar {
      margin: 0 6px;
    }

    .message-content {
      max-width: 90%;
      padding: 6px 10px;
    }
  }

  .el-image {
    width: 100% !important;
    height: auto !important;
  }

  .image-actions {
    .el-button {
      font-size: 11px;
      padding: 3px 6px;
    }
  }
}
</style>
