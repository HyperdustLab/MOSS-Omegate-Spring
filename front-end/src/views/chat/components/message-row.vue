<script lang="tsx" setup>
import TextLoading from './text-loading.vue'
import logo from '@/assets/logo.png'
import MarkdownMessage from './markdown-message.vue'
import type { AiMessage } from '../store/chat-store'
import { computed } from 'vue'
// message: Accepts message object, displays message content and avatar, and adjusts message position based on role.
// avatar: User avatar, if role is Assistant then use logo.
const props = defineProps<{
  message: AiMessage
  avatar?: string
  agentAvatar?: string
}>()

const images = computed(() => {
  const medias = props.message.medias || []
  return medias.filter((media) => media.type === 'image').map((media) => media.data)
})
</script>

<!-- The entire div is used to adjust the position of internal messages, each message takes up a full row, then adjusts internal messages to right or left based on right/left class -->
<template>
  <div :class="['message-row', message.type === 'USER' ? 'right' : 'left']">
    <!-- Message display, divided into top and bottom, avatar on top, message below -->
    <div class="row">
      <!-- Avatar -->
      <div class="avatar-wrapper">
        <el-avatar :src="avatar" class="avatar" shape="square" v-if="message.type === 'USER'" />
        <el-avatar :src="agentAvatar || logo" class="avatar" shape="square" v-else />
      </div>
      <!-- Sent message or reply message -->
      <div class="message">
        <!-- If message is text, display as markdown -->
        <markdown-message :type="message.type" :message="message.textContent" v-if="message.textContent"></markdown-message>
        <!-- If message content is image, display image -->
        <el-image v-for="image in images" :key="image" class="image" fit="cover" :preview-src-list="images" :src="image"></el-image>
        <!-- If message content is empty show loading animation -->
        <TextLoading v-if="!message.textContent && !images.length" style="color: white"></TextLoading>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.message-row {
  display: flex;

  &.right {
    // Message displays on right side
    justify-content: flex-end;

    .row {
      // Avatar also aligns to right
      .avatar-wrapper {
        display: flex;
        justify-content: flex-end;
      }

      // Distinguish background color between user replies and ChatGPT replies
      .message {
        background-color: #1e1e1e;
        :deep(.md-editor) {
          background-color: #1e1e1e;
        }
      }
    }
  }

  // Default left alignment
  .row {
    .avatar-wrapper {
      .avatar {
        box-shadow: 20px 20px 20px 3px rgba(0, 0, 0, 0.01);
        margin-bottom: 20px;
      }
    }

    .message {
      font-size: 15px;
      padding: 1.5px;
      // Limit maximum width of message display
      max-width: 800px;
      // More rounded corners
      border-radius: 7px;
      // Add border to message box to make it look more solid, otherwise too flat and light

      // Background color
      background-color: #1a1a1a;

      color: #ffffff;

      .image {
        width: 600px;
        height: 600px;
      }

      &.user {
        background-color: #213d5b;
      }
    }
  }
}
</style>
