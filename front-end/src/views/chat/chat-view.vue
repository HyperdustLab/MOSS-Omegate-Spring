<script lang="ts" setup>
import { nextTick, onMounted, onUnmounted, ref, Text, watch } from 'vue'
import SessionItem from './components/session-item.vue'
import { ChatRound, Close, Delete, EditPen, Upload, Share } from '@element-plus/icons-vue'
import MessageRow from './components/message-row.vue'
import MessageInput from './components/message-input.vue'
import { storeToRefs } from 'pinia'
import { ElIcon, ElMessage, type UploadProps, type UploadUserFile } from 'element-plus'
import { api } from '@/utils/api-instance'
import { SSE } from 'sse.js'
import { type AiMessage, useChatStore } from './store/chat-store'
import type { AiMessageParams, AiMessageWrapper } from '@/apis/__generated/model/static'
import Login from '@/components/Login/index.vue'

import { useWebSocket } from '@vueuse/core'

import { ElMessageBox } from 'element-plus'

import UploadEmbedding from './components/uploadEmbedding.vue'

import user from '@/assets/user.png'

import { request } from '@/utils/request'

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
// const { handleDeleteSession, handleUpdateSession, handleClearMessage } = chatStore
// const { activeSession, sessionList, isEdit } = storeToRefs(chatStore)
const messageListRef = ref<InstanceType<typeof HTMLDivElement>>()

const activeSession = ref(null)
const sessionList = ref([])

import logoutPng from '@/assets/image/logout.png?url'

import Substring from '@/components/Substring.vue'

const loading = ref(false)

const sendLoading = ref(false)

const loginRef = ref<InstanceType<typeof Login>>()

const BASE_URL = import.meta.env.VITE_API_HYPERAGI_API

const wsUserId = generateUUID()

const wsUrl = BASE_URL.replace('http', 'ws').replace('https', 'wss') + '/ws/app/websocket/' + wsUserId

const { status, data, send, open, close } = useWebSocket(wsUrl, {
  autoReconnect: true,
  onMessage: (ws, event) => {
    console.info('event', event)
    try {
      const msg = JSON.parse(event.data)
      if (msg.action === 'callbackAutoReplyTweetsMedia' && msg.data.replyTweetsRecordId === wsUserId) {
        const text = inputText.value
        inputReplyMediaFileUrls.value = msg.data.replyMediaFileUrls
        inputTextReplyStatus.value = true
        inputText.value = msg.data.tweets
        sendLoading.value = true

        handleSendMessage({ text: text, inputText: inputText.value, image: '', mediaFileUrls: inputReplyMediaFileUrls.value })
      }
    } catch (error) {
      console.error('error', error)
    }
  },
})

window.setInterval(() => {
  send('PING')
}, 10000)

const systemPrompt = ref('')

const agent = ref(null)
const myAgent = ref(null)

const messageList = ref([])

const showSessionEdit = ref(false)

const token = ref(localStorage.getItem('X-Token'))

const uploadEmbeddingRef = ref<InstanceType<typeof UploadEmbedding>>()

const agentList = ref([])

// 添加分页相关参数
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const noMore = ref(false)

// 添加选中的agent id
const selectAgent = ref(null)
const selectAgentId = ref(null)

const currSessionId = ref(generateUUID())

// 添加搜索相关的响应式变量
const searchQuery = ref('')

const inputText = ref('')

const inputTextReplyStatus = ref(false)

const inputReplyMediaFileUrls = ref([])

import { useRoute } from 'vue-router'

const route = useRoute()
const sid = route.query.sid

onMounted(async () => {
  await getAgentList()

  if (localStorage.getItem('X-Token')) {
    await getLoginUser()

    await getAgent()
    // loginRef.value.show()
  }

  if (sid) {
    const { result } = await request({
      url: BASE_URL + '/mgn/agent/list',
      params: {
        sid: sid,
      },
      method: 'GET',
    })

    handleSelectAgent(result.records[0])
  } else {
    if (localStorage.getItem('X-Token')) {
      handleSelectAgent(myAgent.value)
    } else {
      handleSelectAgent(agentList.value[0])
    }
  }

  // 添加滚动监听
  contactListRef.value?.addEventListener('scroll', handleScroll)
})

// 组件卸载时移除监听
onUnmounted(() => {
  contactListRef.value?.removeEventListener('scroll', handleScroll)
})

// ChatGPT response
const responseMessage = ref<AiMessage>({
  id: new Date().getTime().toString(),
  type: 'ASSISTANT',
  medias: [],
  textContent: '',
  sessionId: '',
})

function generateUUID() {
  const bytes = new Uint8Array(16)
  window.crypto.getRandomValues(bytes)

  bytes[6] = (bytes[6] & 0x0f) | 0x40 // Set the version to 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80 // Set the variant to 10xx

  const uuid = [...bytes].map((byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${uuid.slice(0, 8)}-${uuid.slice(8, 12)}-${uuid.slice(12, 16)}-${uuid.slice(16, 20)}-${uuid.slice(20)}`
}

function handleCreateMyAgent() {
  window.open('https://www.aipod.fun/create-new-agent')
}

async function handleShareTwitter(sid) {
  const currentUrl = window.location.origin + '?sid=' + (sid || selectAgent.value.sid)
  const shareText = `check out moss AI agent`
  const twitterShareUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(shareText)}&url=${encodeURIComponent(currentUrl)}`
  window.open(twitterShareUrl, '_blank')
}

async function getSessionList() {
  const { result } = await request({
    url: BASE_URL + '/mgn/aiSession/list',
    method: 'GET',
    headers: {
      'X-Access-Token': token.value,
    },

    params: {
      pageNo: 1,
      pageSize: 10,
      column: 'createdTime',
      order: 'desc',
      creatorId: loginUser.value?.id || currSessionId.value,
      agentId: selectAgent.value.sid,
    },
  })

  sessionList.value = result.records

  if (sessionList.value.length === 0) {
    handleSessionCreate()
  } else {
    handleSelectSession(sessionList.value[0])
  }
}

function handleSelectSession(session) {
  activeSession.value = session
  getMessageList()
}

const preHandleSendMessage = async (message: { text: string; image: string }) => {
  sendLoading.value = true
  inputText.value = message.text

  const msg = {
    action: 'autoReplyTweetsMedia',
    data: {
      replyTweetsRecordId: wsUserId,
      tweets: inputText.value,
    },
  }

  const chatMessage = {
    aiSessionId: activeSession.value.id,
    sessionId: activeSession.value.id,
    medias: null,
    textContent: message.text,
    type: 'USER',
    avatar: loginUser.value?.avatar,
    name: loginUser.value?.walletAddress || 'Guest',
    creatorId: loginUser.value?.id || currSessionId.value,
    editorId: loginUser.value?.id || currSessionId.value,
  }

  messageList.value.push(chatMessage)

  await request.post(BASE_URL + '/ws/socketMsg/sendSocketMsg', {
    userId: selectAgent.value.owner,
    msg: JSON.stringify(msg),
  })

  setTimeout(() => {
    console.info('inputTextReplyStatus.value', inputTextReplyStatus.value)
    if (!inputTextReplyStatus.value) {
      handleSendMessage({ text: message.text, inputText: message.text, image: '', mediaFileUrls: inputReplyMediaFileUrls.value })
    }
  }, 30 * 1000)
}

const handleSendMessage = async (message: { text: string; inputText: string; image: string; mediaFileUrls: string[] }) => {
  if (!activeSession.value) {
    ElMessage.warning('Please create a session')
    return
  }

  // Image/Audio
  const medias: AiMessage['medias'] = []
  if (message.image) {
    medias.push({ type: 'image', data: message.image })
  }

  const chatMessage = {
    aiSessionId: activeSession.value.id,
    sessionId: activeSession.value.id,
    medias,
    textContent: message.inputText,
    type: 'USER',
    avatar: loginUser.value?.avatar,
    name: loginUser.value?.walletAddress || 'Guest',
    creatorId: loginUser.value?.id || currSessionId.value,
    editorId: loginUser.value?.id || currSessionId.value,
  }

  responseMessage.value = {
    type: 'ASSISTANT',
    textContent: '',
    aiSessionId: activeSession.value.id,
    sessionId: activeSession.value.id,
    avatar: selectAgent.value.avatar,
    name: selectAgent.value.nickName,
    creatorId: selectAgent.value.sid,
    editorId: selectAgent.value.sid,
  }

  const form = new FormData()

  let content = ''

  let agentName = ''

  if (selectAgent.value) {
    options.value.userId = selectAgent.value.sid
    content = selectAgent.value.personalization
    agentName = selectAgent.value.nickName
  } else {
    content = systemPrompt.value
    options.value.userId = ''
    agentName = 'MOSS'
  }

  content = content.replace('[agent name]', agentName)

  form.set('content', content)

  options.value.model = options.value.enableAgent ? 'qwen2.5:32b' : 'deepseek-r1:32b'

  const res = await request({
    url: BASE_URL + `/mgn/nodePort/getNodePort`,
    method: 'GET',
    params: { serviceName: options.value.model },
    headers: {
      'X-Access-Token': token.value,
    },
  })

  options.value.baseUrl = `http://${res.result.ip}:${res.result.port}`

  const body: AiMessageWrapper = { message: chatMessage, params: options.value }

  form.set('input', JSON.stringify(body))

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

  let isThinking = true
  let buffer = ''

  evtSource.addEventListener('message', async (event: any) => {
    const response = JSON.parse(event.data) as ChatResponse
    const finishReason = response.result.metadata.finishReason
    if (response.result.output.content) {
      if (isThinking && !options.value.enableAgent) {
        buffer = response.result.output.content
        if (buffer.indexOf('</think>') > -1) {
          isThinking = false
        }
      } else {
        responseMessage.value.textContent += response.result.output.content
      }

      // Scroll to bottom
      await nextTick(() => {
        messageListRef.value?.scrollTo(0, messageListRef.value.scrollHeight)
      })
    }
    if (finishReason && finishReason.toLowerCase() == 'stop') {
      sendLoading.value = false
      evtSource.close()

      chatMessage.textContent = message.text
      responseMessage.value.medias = message.mediaFileUrls.map((url) => ({ type: 'image', data: url }))

      await saveMessage(chatMessage)
      await saveMessage(responseMessage.value)

      await addReasoningRecord({
        inputContent: message.inputText,
        outContent: responseMessage.value.textContent,
        agentId: selectAgent.value.sid,
        userId: loginUser.value?.id || '',
        serviceName: options.value.model,
        systemContent: content,
        remark: options.value.baseUrl,
        nodeId: res.result.minerNodeId,
      })
    }
  })

  // Call stream to initiate request
  evtSource.stream()

  // Display both messages on page
  messageList.value.push(...[responseMessage.value])
  await nextTick(() => {
    messageListRef.value?.scrollTo(0, messageListRef.value.scrollHeight)
  })
}

async function addReasoningRecord(reasoningRecord) {
  await request({
    url: BASE_URL + '/mgn/reasoningRecord/add',
    method: 'POST',
    data: reasoningRecord,
    headers: {
      'X-Access-Token': token.value,
    },
  })
}

async function saveMessage(message) {
  await request({
    url: BASE_URL + '/mgn/aiMessage/add',
    method: 'POST',
    data: message,
    headers: {
      'X-Access-Token': token.value,
    },
  })
}

const handleSessionCreate = async () => {
  await request({
    url: BASE_URL + '/mgn/aiSession/add',
    method: 'POST',
    data: {
      agentId: selectAgent.value.sid,
      name: 'New Chat',
      creatorId: currSessionId.value,
    },
    headers: {
      'X-Access-Token': token.value,
    },
  })
  getSessionList()
}

const options = ref<AiMessageParams>({
  enableVectorStore: false,
  enableAgent: false,
  model: '',
  baseUrl: '',
})
const embeddingLoading = ref(false)

const loginUser = ref(null)

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

  if (!systemPrompt.value) {
    getMossaiPrompt()
  }
}

async function getMossaiPrompt() {
  const res = await request({
    url: BASE_URL + '/mgn/agentNpc/list',
    method: 'GET',
    params: {
      name: 'MOSS',
    },
    headers: {
      'X-Access-Token': token.value,
    },
  })

  const records = res.result.records

  systemPrompt.value = records[0].alpacaPrompt
}

async function getAgent() {
  const { result } = await request({
    url: BASE_URL + '/mgn/agent/list',
    method: 'GET',
    headers: {
      'X-Access-Token': token.value,
    },
    params: {
      walletAddress: loginUser.value.walletAddress || loginUser.value.email,
    },
  })

  const records = result.records

  if (records.length > 0) {
    agent.value = records[0]
    myAgent.value = records[0]
  }
}

// 修改获取agent列表的方法，添加搜索参数
async function getAgentList(isLoadMore = false) {
  if (loading.value || noMore.value) return

  try {
    const params = {
      pageNo: pageNum.value,
      pageSize: pageSize.value,
      userOrderNum: true,
    }

    // 只有在搜索关键词不为空时才添加 nickName 参数
    if (searchQuery.value.trim()) {
      params.nickName = `*${searchQuery.value.trim()}*`
    }

    const { result } = await request({
      url: BASE_URL + '/mgn/agent/list',
      params,
      method: 'GET',
      headers: {
        'X-Access-Token': token.value,
      },
    })

    total.value = result.total

    if (isLoadMore) {
      agentList.value = [...agentList.value, ...result.records]
    } else {
      agentList.value = result.records
    }

    noMore.value = agentList.value.length >= total.value
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 添加搜索处理函数
const handleSearch = () => {
  pageNum.value = 1 // 重置页码
  noMore.value = false // 重置 noMore 状态
  agentList.value = [] // 清空现有列表
  getAgentList() // 重新获取数据
}

// 添加防抖处理
let searchTimer = null
const debouncedSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

// 修改 watch 处理
watch(searchQuery, () => {
  if (!searchQuery.value.trim()) {
    // 当搜索框清空时，重置所有状态并重新加载
    pageNum.value = 1
    noMore.value = false
    agentList.value = []
    getAgentList()
  } else {
    debouncedSearch()
  }
})

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

function showUploadEmbedding() {
  uploadEmbeddingRef.value?.show(myAgent.value.id)
}

const handleBindTwitter = () => {
  const data = {
    token: token.value,
    id: myAgent.value.sid,
    type: '1',
  }

  const encodedData = btoa(JSON.stringify(data))

  window.removeEventListener('message', handleMessage, false)

  window.addEventListener('message', handleMessage, false)

  window.open(BASE_URL + '/mgn/x/render?data=' + encodedData, 'googleLogin', 'width=500,height=600')
}

const fileList = ref<UploadUserFile[]>([])

// 添加滚动加载方法
const contactListRef = ref<HTMLElement>()
const handleScroll = () => {
  if (!contactListRef.value) return

  const { scrollTop, scrollHeight, clientHeight } = contactListRef.value
  // 当滚动到距离底部20px时触发加载
  if (scrollHeight - scrollTop - clientHeight < 20) {
    loadMore()
  }
}

// 加载更多
const loadMore = () => {
  if (loading.value || noMore.value) return
  pageNum.value++
  getAgentList(true)
}

// 添加选中方法
const handleSelectAgent = (_agent) => {
  if (_agent) {
    selectAgent.value = _agent
    selectAgentId.value = _agent.id
    agent.value = _agent
  } else {
    selectAgent.value = myAgent.value
    selectAgentId.value = selectAgent.value.id
  }

  console.info('selectAgentId.value', selectAgentId.value)

  getSessionList()

  handleSearchWeb(false)
}

async function getMessageList() {
  const { result } = await request({
    url: BASE_URL + '/mgn/aiMessage/list',
    method: 'GET',
    headers: {
      'X-Access-Token': token.value,
    },
    params: {
      aiSessionId: activeSession.value.id,
      pageSize: -1,
      column: 'createTime',
      order: 'asc',
    },
  })

  messageList.value = result.records
}

const handleEditSession = () => {
  console.log('before:', showSessionEdit.value)
  showSessionEdit.value = true
  console.log('after:', showSessionEdit.value)
}

const handleCancelEdit = () => {
  console.log('before:', showSessionEdit.value)
  showSessionEdit.value = false
  console.log('after:', showSessionEdit.value)
}

async function handleDeleteSession(sessionId: string) {
  try {
    const result = await ElMessageBox.confirm('Are you sure you want to delete this session?', 'Warning', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning',
      customClass: 'dark-message-box',
      buttonSize: 'default',
      confirmButtonClass: 'dark-confirm-button',
      cancelButtonClass: 'dark-cancel-button',
      draggable: true,
      center: true,
      roundButton: true,
    })

    if (result === 'confirm') {
      await request({
        url: BASE_URL + '/mgn/aiSession/delete',
        method: 'DELETE',
        headers: {
          'X-Access-Token': token.value,
        },
        params: {
          id: sessionId,
        },
      })

      ElMessage({
        type: 'success',
        message: 'Deleted successfully',
        customClass: 'dark-message',
      })

      getSessionList()
    }
  } catch {
    // User cancelled deletion
  }
}

function handleMessage(event: any) {
  // 你可以根据 event.origin 判断消息的来源是否是你信任的源
  // 例如: if (event.origin !== "https://your-trusted-domain.com") return;

  // 接收父窗口传递的数据
  var receivedData = event.data

  console.info(BASE_URL)
  console.info(event.origin)
  console.info('receivedData:', receivedData)
  // 判断receivedData是否为JSON字符串
  let isJsonString = false
  try {
    const json = JSON.parse(receivedData)

    if (json.action === 'bindXSuccess') {
      ElMessage({
        type: 'success',
        message: 'Bind X successfully',
        customClass: 'dark-message',
      })
      location.reload()
    }
  } catch (e) {
    isJsonString = false
  }
}

// 添加更新会话名称的方法
const handleUpdateSession = async () => {
  try {
    await request({
      url: BASE_URL + '/mgn/aiSession/edit',
      method: 'POST',
      headers: {
        'X-Access-Token': token.value,
      },
      data: {
        id: activeSession.value.id,
        name: activeSession.value.name,
      },
    })

    // 更新成功后关闭编辑模式
    showSessionEdit.value = false
    // 可选：刷新会话列表
    getSessionList()
  } catch (error) {
    console.error('Update session failed:', error)
  }
}

async function handleSearchWeb(message: boolean) {
  options.value.enableAgent = message
}

function handleLogin() {
  loginRef.value.show()
}

async function unbindX() {
  try {
    const confirmResult = await ElMessageBox.confirm('Are you sure you want to unbind X?', 'Warning', {
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
      type: 'warning',
      customClass: 'dark-message-box',
      buttonSize: 'default',
      confirmButtonClass: 'dark-confirm-button',
      cancelButtonClass: 'dark-cancel-button',
      draggable: true,
      center: true,
      roundButton: true,
    })

    if (confirmResult === 'confirm') {
      await request({
        url: BASE_URL + '/mgn/agent/unbindX',
        method: 'GET',
        params: {
          sid: myAgent.value.sid,
        },
        headers: {
          'X-Access-Token': token.value,
        },
      })
      location.reload()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage({
        type: 'error',
        message: 'Failed to unbind X',
      })
    }
  }
}
</script>
<template>
  <div class="home-view">
    <!-- LOGO部分调整到最左边 -->
    <div class="w-full flex items-start px-4 py-3 border-b border-gray-700 fixed top-0 left-0 z-10">
      <img @click="goHome" src="../../assets/logo1.gif" style="width: 60px; height: 80px" loading="lazy" class="cursor-pointer ml-[100px]" alt="logo" />
    </div>

    <!-- Entire chat panel -->
    <div class="chat-panel" style="margin-top: 100px" v-loading="loading">
      <!-- 将联系人列表移到最左边 -->
      <div class="contact-panel w-64 border-r border-gray-700 bg-[#1e1e1e] h-full">
        <!-- 其他内容添加padding -->
        <div class="p-4">
          <div class="text-white text-lg mb-4">My Agent</div>
          <div class="space-y-4 mb-6">
            <div v-if="myAgent" class="flex items-center space-x-3 p-2 hover:bg-gray-700 rounded-lg cursor-pointer transition-colors duration-200" :class="{ 'bg-gray-700': selectAgentId === myAgent.id }" @click="handleSelectAgent(selectAgentId === myAgent.id ? null : myAgent)">
              <el-avatar :size="40" :src="myAgent.avatar" />
              <div>
                <div class="text-white text-sm">{{ myAgent.nickName }}</div>
              </div>
            </div>

            <div v-else>
              <div class="flex items-center justify-center p-2 hover:bg-gray-700 rounded-lg cursor-pointer transition-colors duration-200">
                <el-button @click="handleCreateMyAgent" round type="primary" class="w-full">Create My Agent</el-button>
              </div>
            </div>
          </div>

          <!-- 搜索框和列表内容 -->
          <div class="text-white text-lg mb-4">Agent List</div>
          <div class="mb-4">
            <el-input v-model="searchQuery" placeholder="Search agents..." class="w-full" :prefix-icon="Search"></el-input>
          </div>
          <div ref="contactListRef" class="h-[calc(75vh-80px)] overflow-y-auto custom-scrollbar" style="max-height: calc(90% - 120px)">
            <div class="space-y-2">
              <div
                v-for="agent in agentList"
                :key="agent.id"
                class="flex items-center space-x-3 p-2 bg-[#1e1e1e] hover:bg-[#2c2c2c] rounded-lg cursor-pointer transition-colors duration-200"
                :class="{ 'bg-[#2c2c2c]': selectAgentId === agent.id }"
                @click="handleSelectAgent(selectAgentId === agent.id ? null : agent)"
              >
                <el-avatar :size="40" :src="agent.avatar" />
                <div>
                  <div class="text-white text-sm flex items-center">
                    {{ agent.nickName }}
                    <img v-if="agent.xname" src="../../assets/x.svg" alt="X" class="w-4 h-4 ml-6 mt-2" />
                    <el-icon size="18" class="hover:text-blue-300 transition-colors duration-300 ml-6 mt-2" @click.stop="handleShareTwitter(agent.sid)">
                      <Share />
                    </el-icon>
                  </div>
                </div>
              </div>

              <!-- Loading status -->
              <div v-if="loading" class="text-center py-4 text-gray-400">Loading...</div>

              <!-- No more data -->
              <div v-if="noMore && agentList.length > 0" class="text-center py-4 text-gray-400">No more data</div>

              <!-- No data -->
              <div v-if="!loading && agentList.length === 0" class="text-center py-4 text-gray-400">No data</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 会话列表面板 -->
      <div class="session-panel w-64 border-r border-gray-700 bg-[#141414] p-4 h-full">
        <div class="button-wrapper mt-20">
          <div class="create-session-btn cursor-pointer flex flex-col items-center justify-center px-4 py-2 text-sm hover:bg-gray-700 rounded" @click="handleSessionCreate">
            <img style="width: 30px" src="../../assets/create.png" alt="create" class="create-icon" />
          </div>
        </div>

        <div class="session-list h-[calc(80vh-80px)] overflow-y-auto custom-scrollbar" v-if="activeSession">
          <session-item v-for="session in sessionList" :key="session.id" :active="session.id === activeSession.id" :session="session" class="session" @click="handleSelectSession(session)" @delete="handleDeleteSession(session.id)" />
        </div>

        <div class="option-panel">
          <el-form size="small" v-if="myAgent && myAgent.id === selectAgentId" class="rag-form">
            <el-form-item label-width="8.2rem" label="RAG Knowledge" class="form-item-align">
              <el-button class="ml-0" :style="{ backgroundColor: '#2d2736', color: 'white', border: 'aliceblue' }" @click="showUploadEmbedding">
                <img style="width: 15px; height: 15px" src="../../assets/docUpload.svg" alt="upload" />
              </el-button>
            </el-form-item>
            <el-form-item label="Enable Knowledge Base" class="form-item-align">
              <el-switch class="ml-0" v-model="options.enableVectorStore" style="--el-switch-on-color: #13ce66"></el-switch>
            </el-form-item>
            <el-form-item v-if="!myAgent || !myAgent.xname" label="Bind X" class="form-item-align">
              <el-button class="ml-0" :style="{ backgroundColor: '#2d2736', color: 'white', border: 'aliceblue' }" @click="handleBindTwitter">
                <img style="width: 15px; height: 15px" src="../../assets/bind.svg" alt="upload" />
              </el-button>
            </el-form-item>

            <el-form-item v-else label="Unbind X" class="form-item-align">
              <el-button class="ml-0" :style="{ backgroundColor: '#2d2736', color: 'white', border: 'aliceblue' }" @click="unbindX">
                <img style="width: 15px; height: 15px" src="../../assets/unbind.svg" alt="upload" />
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 消息面板 -->
      <div class="message-panel">
        <!-- Session name -->
        <div class="header" v-if="activeSession">
          <div class="front">
            <!-- 显示当前选中agent的信息 -->
            <div class="flex flex-col">
              <div class="flex items-center">
                <el-avatar :size="30" :src="selectAgent.avatar" class="mr-3" />
                <div class="flex flex-col ml-10">
                  <div class="flex items-center">
                    <span class="text-white text-base">{{ selectAgent.nickName }}</span>

                    <el-link v-if="!selectAgent.xname" class="flex items-center transition-all duration-300 hover:scale-105 ml-5 mt-2" @click="handleShareTwitter(selectAgent.sid)" :underline="false">
                      <el-icon size="18" class="text-blue-400 hover:text-blue-300 transition-colors duration-300">
                        <Share />
                      </el-icon>
                    </el-link>
                  </div>

                  <div v-if="selectAgent.xname" class="mt-3 flex items-start rounded-lg transition-all duration-300 hover:bg-gray-700/10">
                    <span class="text-gray-300 text-sm flex items-center group text-left">
                      <img src="../../assets/x.svg" alt="X" class="w-4 h-4 mr-2 transition-transform duration-300 group-hover:scale-110 flex-shrink-0" />
                      <a :href="`https://x.com/${selectAgent.xusername}`" target="_blank" class="font-medium truncate text-gray-400">@{{ selectAgent.xusername }}</a>
                    </span>
                    <el-link class="flex items-center transition-all duration-300 hover:scale-105 ml-5 mt-2" :underline="false" @click="handleShareTwitter(selectAgent.sid)">
                      <el-icon size="18" class="text-blue-400 hover:text-blue-300 transition-colors duration-300">
                        <Share />
                      </el-icon>
                    </el-link>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <!-- Edit buttons at end -->
          <div class="flex items-center">
            <div class="rear">
              <el-icon :size="20" style="margin-right: 10px; color: white">
                <Delete @click="handleDeleteSession(activeSession.id)" />
              </el-icon>

              <el-icon :size="20" style="color: white" @click="handleEditSession">
                <EditPen v-if="!isEdit" />
                <Close v-else @click="handleCancelEdit" />
              </el-icon>
            </div>
            <div class="front">
              <!-- 编辑模式下显示输入框 -->
              <div v-if="showSessionEdit" class="title flex-grow">
                <el-input v-model="activeSession.name" @keydown.enter="handleUpdateSession" @blur="handleUpdateSession"></el-input>
              </div>
              <!-- 非编辑模式下显示文本 -->
              <div v-else class="title flex-grow">{{ activeSession.name }}</div>
            </div>
          </div>
        </div>
        <el-divider :border-style="'solid'" border-color="#666666" />
        <div ref="messageListRef" class="message-list">
          <!-- Transition effect -->
          <transition-group name="list" v-if="activeSession && agent">
            <message-row v-for="message in messageList" :agent-avatar="message.avatar" :avatar="loginUser && loginUser.avatar ? loginUser.avatar : user" :key="message.id" :message="message"></message-row>
          </transition-group>
        </div>
        <!-- Listen for send event -->
        <message-input @send="preHandleSendMessage" :loading="sendLoading" @search="handleSearchWeb" :functionStatus="selectAgent.functionStatus" v-if="activeSession && selectAgent"></message-input>

        <el-dropdown v-if="loginUser" class="bg-[#303133] rounded-full fixed top-2 right-25 h-7 w-40 mt-20 z-50">
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
        <div v-else class="bg-[#303133] rounded-full fixed top-2 text-white right-25 h-7 w-40 mt-20 z-50 flex items-center justify-center cursor-pointer" @click="handleLogin">
          <el-avatar :size="16" :src="user" style="border: none" />
          <span class="ml-1.25 text-white"> Login In </span>
        </div>

        <Login ref="loginRef" />

        <UploadEmbedding ref="uploadEmbeddingRef"></UploadEmbedding>
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
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  overflow: hidden;

  .chat-panel {
    margin: 0 auto;
    width: 90%;
    display: flex;
    background-color: #1e1e1e;
    height: 90vh;
    box-shadow: 0 0 10px rgba(black, 0.1);
    border-radius: 10px;
    overflow: hidden;

    .session-panel {
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
      padding: 20px;
      position: relative;
      border-right: 1px solid rgba(255, 255, 255, 0.07);
      background-color: #141414;
      height: 100%;
      overflow: hidden;
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
      width: calc(100% - 500px);
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
            font-size: 16px;
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

// 修改滚动条样式
.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 0; // 将宽度设为0来隐藏滚动条
    display: none; // 完全隐藏滚动条
  }

  // 添加 Firefox 的滚动条隐藏
  scrollbar-width: none;

  // 添加 IE 的滚动条隐藏
  -ms-overflow-style: none;
}

// 可以添加选中状态的过渡效果
.transition-colors {
  transition: background-color 0.2s ease;
}

:deep(.option-form) {
  .el-form-item {
    margin-bottom: 12px;

    .el-form-item__label {
      justify-content: flex-start;
      padding-right: 8px;
    }
  }

  .el-button {
    margin-left: 0;
  }

  .el-switch {
    margin-left: 0;
  }
}

:deep(.el-input) {
  .el-input__wrapper {
    background-color: #1e1e1e !important;
    background-color: #1e1e1e;
    box-shadow: 0 0 0 1px #303133 inset;

    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .el-input__inner {
    color: white !important;

    &::placeholder {
      color: #606266;
    }
  }
}

.rag-form {
  :deep(.form-item-align) {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .el-form-item__label {
      justify-content: flex-start;
    }

    .el-form-item__content {
      margin-left: auto !important;
      justify-content: flex-end;
    }
  }
}
</style>
