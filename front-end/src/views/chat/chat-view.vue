<script lang="ts" setup>
import { nextTick, onMounted, ref, Text } from 'vue'
import SessionItem from './components/session-item.vue'
import { ChatRound, Close, Delete, EditPen, Upload } from '@element-plus/icons-vue'
import MessageRow from './components/message-row.vue'
import MessageInput from './components/message-input.vue'
import { storeToRefs } from 'pinia'
import { ElIcon, ElMessage, type UploadProps, type UploadUserFile } from 'element-plus'
import { api } from '@/utils/api-instance'
import { SSE } from 'sse.js'
import { type AiMessage, useChatStore } from './store/chat-store'
import type { AiMessageParams, AiMessageWrapper } from '@/apis/__generated/model/static'
import Login from '@/components/Login/index.vue'

import UploadEmbedding from './components/uploadEmbedding.vue'

import user from '@/assets/user.png'

import { request } from '@/utils/request'

import { useRoute } from 'vue-router'
type ChatResponse = {
  metadata: {
    usage: {
      totalTokens: number
    }
  }
  result: {
    metadata: {
      finishReason: string
    }
    output: {
      messageType: string
      content: string
    }
  }
}
const API_PREFIX = import.meta.env.VITE_API_PREFIX
const chatStore = useChatStore()
const { handleDeleteSession, handleUpdateSession, handleClearMessage } = chatStore
const { activeSession, sessionList, isEdit } = storeToRefs(chatStore)
const messageListRef = ref<InstanceType<typeof HTMLDivElement>>()

const loading = ref(false)

const loginRef = ref<InstanceType<typeof Login>>()

const BASE_URL = import.meta.env.VITE_API_HYPERAGI_API

const systemPrompt = ref('')

const agent = ref(null)

const token = ref(localStorage.getItem('X-Token'))

import logoutPng from '@/assets/image/logout.png?url'

import Substring from '@/components/Substring.vue'

onMounted(async () => {
  // Query user's chat sessions
  api.aiSessionController.findByUser().then((res) => {
    if (!res) {
      return
    }

    // Add sessions to list
    sessionList.value = res.result.map((row) => {
      return { ...row, checked: false }
    })
    // Default select first chat session
    if (sessionList.value.length > 0) {
      activeSession.value = sessionList.value[0]
    } else {
      handleSessionCreate()
    }
    loading.value = false
  })

  getSystemPrompt()
  getAgent()

  const route = await useRoute()

  if (!localStorage.getItem('X-Token')) {
    loginRef.value.show()
  } else {
    getLoginUser()
  }
})

// ChatGPT response
const responseMessage = ref<AiMessage>({
  id: new Date().getTime().toString(),
  type: 'ASSISTANT',
  medias: [],
  textContent: '',
  sessionId: '',
})

const handleSendMessage = async (message: { text: string; image: string }) => {
  if (!activeSession.value) {
    ElMessage.warning('Please create a session')
    return
  }
  // Image/Audio
  const medias: AiMessage['medias'] = []
  if (message.image) {
    medias.push({ type: 'image', data: message.image })
  }
  // User question
  const chatMessage = {
    id: new Date().getTime().toString(),
    sessionId: activeSession.value.id,
    medias,
    textContent: message.text,
    type: 'USER',
  } satisfies AiMessage

  responseMessage.value = {
    id: new Date().getTime().toString(),
    medias: [],
    type: 'ASSISTANT',
    textContent: '',
    sessionId: activeSession.value.id,
  }
  const body: AiMessageWrapper = { message: chatMessage, params: options.value }
  const form = new FormData()
  form.set('input', JSON.stringify(body))
  form.set('content', systemPrompt.value)

  if (fileList.value.length && fileList.value[0].raw) {
    form.append('file', fileList.value[0].raw)
  }
  const evtSource = new SSE(API_PREFIX + '/message/chat', {
    withCredentials: true,
    // Disable auto start, need to call stream() to initiate request
    start: false,
    payload: form as any,
    method: 'POST',
  })
  evtSource.addEventListener('message', async (event: any) => {
    const response = JSON.parse(event.data) as ChatResponse
    const finishReason = response.result.metadata.finishReason
    if (response.result.output.content) {
      responseMessage.value.textContent += response.result.output.content
      // Scroll to bottom
      await nextTick(() => {
        messageListRef.value?.scrollTo(0, messageListRef.value.scrollHeight)
      })
    }
    if (finishReason && finishReason.toLowerCase() == 'stop') {
      evtSource.close()
      // Save user question
      await api.aiMessageController.save({ body: chatMessage })
      // Save AI response
      await api.aiMessageController.save({ body: responseMessage.value })
    }
  })

  // Call stream to initiate request
  evtSource.stream()
  // Display both messages on page
  activeSession.value.messages.push(...[chatMessage, responseMessage.value])
  await nextTick(() => {
    messageListRef.value?.scrollTo(0, messageListRef.value.scrollHeight)
  })
}

const handleSessionCreate = () => {
  chatStore.handleCreateSession({ name: 'New Chat' })
}
const options = ref<AiMessageParams>({
  enableVectorStore: false,
  enableAgent: false,
})
const embeddingLoading = ref(false)

const loginUser = ref(null)

const onUploadSuccess = () => {
  embeddingLoading.value = false
  ElMessage.success('Upload successful')
}
const beforeUpload: UploadProps['beforeUpload'] = () => {
  embeddingLoading.value = true
  return true
}

async function getSystemPrompt() {
  const res = await request({
    url: '/user/getSystemPrompt',
    method: 'GET',
  })

  if (res.code === 10012) {
    localStorage.removeItem('X-Token')
    location.reload()
    return
  }

  systemPrompt.value = res.result
}

async function getAgent() {
  const res = await request({
    url: '/user/getAgent',
    method: 'GET',
  })

  if (res.code === 10012) {
    localStorage.removeItem('X-Token')
    location.reload()
    return
  }

  agent.value = res.result
}

async function getLoginUser() {
  const res = await request({
    url: BASE_URL + '/sys/getCurrUser',
    headers: {
      'X-Access-Token': token.value,
    },
    method: 'GET',
  }).catch((error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('X-Token')
      location.reload()
    }
    throw error
  })

  loginUser.value = res.result
}

const disconnect = () => {
  localStorage.removeItem('X-Token')
  location.reload()
}

const goHome = () => {
  location.href = `https://mossai.com/`
}

function goUser() {
  location.href = `https://user.hyperagi.network/login?token=${token.value}`
}

const fileList = ref<UploadUserFile[]>([])
</script>
<template>
  <!-- Outer page same width as window, center chat panel -->
  <div class="home-view">
    <!-- Entire chat panel -->
    <div class="chat-panel" v-loading="loading">
      <!-- Left session list -->
      <div class="session-panel">
        <div class="flex flex-col items-center gap-4" @click="goHome">
          <img src="../../assets/logo1.gif" loading="lazy" class="w-20" alt="logo" style="margin-right: 80%" />
        </div>

        <div class="button-wrapper mt-10">
          <div class="create-session-btn cursor-pointer flex flex-col items-center justify-center px-4 py-2 text-sm hover:bg-gray-700 rounded" @click="handleSessionCreate">
            <img style="width: 30px" src="../../assets/create.png" class="create-icon" />
          </div>
        </div>

        <div class="session-list" v-if="activeSession">
          <!-- Loop through session list using session component, listen for click and delete events. Switch to clicked session on click, remove deleted session from list on delete. -->
          <session-item v-for="session in sessionList" :key="session.id" :active="session.id === activeSession.id" :session="session" class="session" @click="activeSession = session" @delete="handleDeleteSession"></session-item>
        </div>

        <div class="option-panel">
          <el-form size="small" v-if="agent && agent.agentId">
            <el-form-item label="RAG Knowledge">
              <el-upload v-loading="embeddingLoading" multiple name="files" :action="`${API_PREFIX}/document/embedding`" :show-file-list="false" :on-success="onUploadSuccess" :before-upload="beforeUpload">
                <el-button class="ml-50" type="primary">
                  <p style="color: white">Upload</p>
                </el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="Enable Knowledge Base">
              <el-switch class="ml-43" v-model="options.enableVectorStore" style="--el-switch-on-color: #13ce66"></el-switch>
            </el-form-item>
            <!-- <el-form-item label="agent">
              <el-switch v-model="options.enableAgent"></el-switch>
            </el-form-item>
            <el-form-item label="File">
              <div class="upload">
                <el-upload v-model:file-list="fileList" :auto-upload="false" :limit="1">
                  <el-button type="primary">Upload Text File</el-button>
                </el-upload>
              </div>
            </el-form-item> -->
          </el-form>
        </div>
      </div>
      <!-- Right message history -->
      <div class="message-panel">
        <!-- Session name -->
        <div class="header" v-if="activeSession">
          <div class="front">
            <!-- Show input box for editing when in edit mode -->
            <div v-if="isEdit" class="title">
              <!-- Press enter to confirm edit -->
              <el-input v-model="activeSession.name" @keydown.enter="handleUpdateSession"></el-input>
            </div>
            <!-- Otherwise show normal title -->
            <div v-else class="title">{{ activeSession.name }}</div>
            <div class="description">{{ activeSession.messages.length }} messages</div>
          </div>
          <!-- Edit buttons at end -->
          <div class="rear">
            <el-icon :size="20" style="margin-right: 10px; color: white">
              <Delete @click="handleClearMessage(activeSession.id)" />
            </el-icon>
            <el-icon :size="20" style="color: white">
              <!-- Show edit button when not in edit mode -->
              <EditPen v-if="!isEdit" @click="isEdit = true" />
              <!-- Show cancel edit button when in edit mode -->
              <Close v-else @click="isEdit = false"></Close>
            </el-icon>
          </div>
        </div>
        <el-divider :border-style="'solid'" border-color="#666666" />
        <div ref="messageListRef" class="message-list">
          <!-- Transition effect -->
          <transition-group name="list" v-if="activeSession && agent">
            <message-row v-for="message in activeSession.messages" :agent-avatar="agent.agentAvatar" :avatar="agent.avatar || user" :key="message.id" :message="message"></message-row>
          </transition-group>
        </div>
        <!-- Listen for send event -->
        <message-input @send="handleSendMessage" v-if="activeSession"></message-input>
        <el-dropdown v-if="loginUser" class="bg-[#303133] rounded-full fixed top-2 right-25 h-7 w-40">
          <span class="el-dropdown-link mt-[-5px] flex items-center">
            <el-avatar :size="16" :src="loginUser.avatar" style="border: none" />

            <span class="ml-1.25">
              <Substring :copys="false" color="#ffffff" fontSize="13px" :value="loginUser.walletAddress || loginUser.email"></Substring>
            </span>

            <el-icon size="13" class="el-icon--right">
              <arrow-down />
            </el-icon>
          </span>
          <template #dropdown>
            <el-card class="w-50 bg-[#303133] text-white">
              <template #header>
                <div class="card-header flex items-center">
                  <el-avatar :size="25" :src="loginUser.avatar" />

                  <span class="ml-3.75">
                    <Substring fontSize="12px" :value="loginUser.walletAddress || loginUser.email"></Substring>
                  </span>
                </div>
              </template>

              <p class="ml-1.25 flex items-center">
                <el-icon size="20" class="text-white">
                  <User />
                </el-icon>

                <el-button type="plain" @click="goUser" style="color: white" class="text-xs text-white" link>Dashboard</el-button>
              </p>

              <p class="ml-1.25 flex items-center mt-7.5">
                <el-image :src="logoutPng" class="w-5 h-5"></el-image>

                <el-button type="plain" @click="disconnect" style="color: white" class="text-xs text-white" link>Disconnect</el-button>
              </p>
            </el-card>
          </template>
        </el-dropdown>
        <Login ref="loginRef" />
      </div>
    </div>
  </div>
</template>
<style lang="scss" scoped>
:deep .el-card {
  --el-card-border-color: #303133;
  --el-card-border-radius: 4px;
  --el-card-padding: 20px;
  --el-card-bg-color: var(--el-fill-color-blank);
  background-color: var(--el-card-bg-color);
  border: 1px solid var(--el-card-border-color);
  border-radius: var(--el-card-border-radius);
  color: var(--el-text-color-primary);
  overflow: hidden;
  transition: var(--el-transition-duration);
}

:deep .el-input-group__append {
  border-left: 0;
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  box-shadow:
    0 0 0 0 var(--el-input-border-color) inset,
    0 0 0 0 var(--el-input-border-color) inset,
    0 0 0 0 var(--el-input-border-color) inset;
}

:deep .el-menu--horizontal {
  display: flex;
  flex-wrap: nowrap;
  border-bottom: solid 0px var(--el-menu-border-color);
  border-right: none;
}

.search-wrapper {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.toolbar-wrapper {
  justify-content: space-between;
  margin-bottom: 20px;
}

.table-wrapper {
  margin-bottom: 20px;
}

.pager-wrapper {
  display: flex;
  justify-content: flex-end;
}

.time {
  font-size: 12px;
  color: #999;
}

.bottom {
  margin-top: 13px;
  line-height: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.button {
  padding: 0;
  min-height: auto;
}

.image {
  width: 400px;
  height: 200px;
  display: block;
}

::v-deep .el-dropdown-link--active,
::v-deep .el-dropdown-link:active,
::v-deep .el-dropdown-link:focus {
  outline: none;
  border: none;
  box-shadow: none;
}

.el-dropdown-link {
  font-size: 14px;

  padding-left: 6%;
}

.loading-indicator {
  text-align: center;
  padding: 10px;
  font-size: 16px;
  color: white;
}

:deep(.el-divider--horizontal) {
  border-top: 1px solid #282c34;
  display: block;
  height: 1px;
  margin: 24px 0;
  width: 100%;
}

:deep(.el-form-item--small .el-form-item__label) {
  height: 24px;
  line-height: 24px;
  color: white;
}

.home-view {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;

  .chat-panel {
    display: flex;
    background-color: #1e1e1e;
    width: 90%;
    height: 90%;
    box-shadow: 0 0 10px rgba(black, 0.1);
    border-radius: 10px;

    .session-panel {
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
      padding: 20px;
      position: relative;
      border-right: 1px solid rgba(255, 255, 255, 0.07);
      background-color: #141414;
      height: 100%;
      /* Title */
      .title {
        margin-top: 20px;
        font-size: 20px;
        color: #ffffff;
      }

      .session-list {
        overflow-y: scroll;
        margin: 20px 0;
        flex: 1;

        .session {
          /* Space between sessions */
          margin-top: 20px;
        }

        .session:first-child {
          margin-top: 0;
        }
      }

      .button-wrapper {
        /* entity-panel is relative layout, button-wrapper is absolute relative to it */
        bottom: 20px;
        left: 0;
        display: flex;
        /* Show buttons on right */
        justify-content: flex-end;
        /* Same width as session-panel */
        width: 100%;

        /* Leave space between button and right edge */
        .new-session {
          margin-right: 20px;
        }
      }
    }

    /* Right message history panel */
    .message-panel {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;

      .header {
        padding: 20px 20px 0 20px;
        display: flex;
        /* Session name and edit button distributed left and right horizontally */
        justify-content: space-between;

        /* Front title and message count */
        .front {
          .title {
            color: rgba(255, 255, 255, 0.7);
            font-size: 20px;
          }

          .description {
            margin-top: 10px;
            color: rgba(255, 255, 255, 0.5);
          }
        }

        /* Edit and cancel edit buttons at end */
        .rear {
          display: flex;
          align-items: center;
        }
      }

      .message-list {
        padding: 15px;
        width: 100%;
        flex: 1;
        box-sizing: border-box;
        // Scroll overflow when too many messages
        overflow-y: scroll;
        // Transition effect when switching chat sessions
        .list-enter-active,
        .list-leave-active {
          transition: all 0.5s ease;
        }

        .list-enter-from,
        .list-leave-to {
          opacity: 0;
          transform: translateX(30px);
        }
      }
    }

    // Options panel
    .option-panel {
      width: 100%;
      padding: 20px;
      border-left: 1px solid rgba(black, 0.07);

      .upload {
        width: 160px;
      }
    }
  }
}
</style>
