<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { Position } from '@element-plus/icons-vue'
import ImageUpload from '@/components/image/image-upload.vue'
import { ElMessage } from 'element-plus'
type Message = {
  text: string
  image: string
}
const props = defineProps<{
  functionStatus: string
  loading: boolean
}>()

// Send message event
const emit = defineEmits<{
  send: [message: Message]
  search: [message: boolean]
}>()
// Message in input box
const message = ref<Message>({ text: '', image: '' })

// Prevent page scrolling when input is focused
const handleFocus = () => {
  document.body.style.overflow = 'hidden'
  document.body.style.position = 'fixed'
}

const handleBlur = () => {
  document.body.style.overflow = ''
  document.body.style.position = ''
}

const sendMessage = () => {
  if (!message.value.text) {
    ElMessage.warning('Please enter a message')
    return
  }
  emit('send', message.value)
  // Clear after sending
  message.value = { text: '', image: '' }
}

const buttonActive = reactive({
  search: false,
  upload: false,
})

const searchToggleButton = () => {
  buttonActive.search = !buttonActive.search
  emit('search', buttonActive.search)
}

const uploadToggleButton = () => {
  buttonActive.upload = !buttonActive.upload
}
</script>

<template>
  <div class="message-input">
    <div class="input-wrapper">
      <div class="input-container">
        <!-- Press enter to send, input box height is 3 lines -->
        <el-input v-model="message.text" :autosize="false" :rows="1" class="input" resize="none" type="textarea" @keydown.enter.prevent="sendMessage" @touchmove.prevent @focus="handleFocus" @blur="handleBlur"></el-input>
        <el-button :loading="props.loading" round type="primary" class="send-button" @click="sendMessage">
          <el-icon class="el-icon--left">
            <Position />
          </el-icon>
          Send
        </el-button>
      </div>

      <div class="mt-10">
        <div style="display: flex; align-items: center">
          <el-button v-if="props.functionStatus === 'Y'" :style="{ backgroundColor: buttonActive.search ? '#39b35752' : '#2d2736', color: buttonActive.search ? '#39b357' : 'white', border: 'aliceblue' }" round @click="searchToggleButton" class="toggle-button">Web Search</el-button>

          <el-button :style="{ backgroundColor: '#2d2736', color: 'white', border: 'aliceblue' }" icon="Plus" round disabled class="toggle-button">
            <img style="width: 20px; height: 20px" src="../../../assets/upload.svg" alt="upload" />
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.message-input {
  padding: 20px 20px 0 20px;
  height: 7rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  border-top-right-radius: 5px;
  border-top-left-radius: 5px;
  background-color: #1e1e1e;
  margin-bottom: 10px;

  .el-form-item {
    align-items: center;
  }

  :deep(.el-textarea__inner) {
    background-color: #2b2b2b;
    border-color: rgba(255, 255, 255, 0.1);
    color: #ffffff;
    box-shadow: 0 0 0 1px #282c34 inset;
    padding-right: 120px;
    padding-bottom: 50px;
    font-size: 16px;
    touch-action: pan-y; // Allow vertical panning for text selection

    &:hover,
    &:focus {
      border-color: #409eff;
    }
  }

  @media screen and (max-width: 768px) {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    margin-bottom: 0;
    z-index: 1000;
    border-radius: 0;
    padding: 10px;
    height: auto;
    -webkit-overflow-scrolling: touch;
    overflow-y: auto;
  }
}

.input-container {
  position: relative;
  width: 100%;
}

.send-button {
  position: absolute;
  right: 16px;
  bottom: 16px;
  height: 32px;
  z-index: 1;
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
