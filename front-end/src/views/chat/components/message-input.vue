<script lang="ts" setup>
import { ref } from 'vue'
import { Position } from '@element-plus/icons-vue'
import ImageUpload from '@/components/image/image-upload.vue'
import { ElMessage } from 'element-plus'
type Message = {
  text: string
  image: string
}
// Send message event
const emit = defineEmits<{
  send: [message: Message]
}>()
// Message in input box
const message = ref<Message>({ text: '', image: '' })
const sendMessage = () => {
  if (!message.value.text) {
    ElMessage.warning('Please enter a message')
    return
  }
  emit('send', message.value)
  // Clear after sending
  message.value = { text: '', image: '' }
}
</script>

<template>
  <div class="message-input">
    <div class="input-wrapper">
      <!-- Press enter to send, input box height is 3 lines -->
      <el-input v-model="message.text" :autosize="false" :rows="3" class="input" resize="none" type="textarea" @keydown.enter.prevent="sendMessage"> </el-input>
      <div class="button-wrapper">
        <image-upload class="image" :size="40" v-model="message.image" @click.prevent disabled></image-upload>

        <el-button type="primary" @click="sendMessage">
          <el-icon class="el-icon--left">
            <Position />
          </el-icon>
          Send
        </el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.message-input {
  padding: 20px 20px 0 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  border-top-right-radius: 5px;
  border-top-left-radius: 5px;
  background-color: #1e1e1e;
  .el-form-item {
    align-items: center;
  }

  :deep(.el-textarea__inner) {
    background-color: #2b2b2b;
    border-color: rgba(255, 255, 255, 0.1);
    color: #ffffff;
    box-shadow: 0 0 0 1px #282c34 inset;

    &:hover,
    &:focus {
      border-color: #409eff;
    }
  }
}

.button-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px;
  .image {
    margin-right: 20px;
  }
}
</style>
