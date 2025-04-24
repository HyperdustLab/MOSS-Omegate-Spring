<script lang="ts" setup>
import { nextTick, normalizeClass, onMounted, onUnmounted, ref, Text, watch } from 'vue'
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

const selectMyAgentId = ref('')

const replySearch = ref(null)

// Add timestamp and flag for message processing control
const lastProcessedTime = ref(0)
const isProcessing = ref(false)

const reasoningRecord = ref(null)

// ChatGPT response
const responseMessage = ref<AiMessage>({
  id: new Date().getTime().toString(),
  type: 'ASSISTANT',
  medias: [],
  textContent: '',
  sessionId: '',
})

const chatMessage = ref<AiMessage>({
  id: new Date().getTime().toString(),
  type: 'USER',
  medias: [],
  textContent: '',
  sessionId: '',
})

const wsUrl = BASE_URL.replace('http', 'ws').replace('https', 'wss') + '/ws/app/websocket/' + wsUserId

const { status, data, send, open, close } = useWebSocket(wsUrl, {
  autoReconnect: true,
  onMessage: async (ws, event) => {
    console.info('event', event)
    try {
      const currentTime = Date.now()
      // Only process if more than 10 seconds have passed since last processing
      if (currentTime - lastProcessedTime.value < 3000 || isProcessing.value) {
        console.log('Message ignored due to rate limiting')
        return
      }

      const msg = JSON.parse(event.data)

      if (msg.data.replyTweetsRecordId !== wsUserId) {
        console.log('msg.data.replyTweetsRecordId', msg.data.replyTweetsRecordId)
        console.log('wsUserId', wsUserId)
        return
      }

      const text = inputText.value

      switch (msg.action) {
        case 'callbackAutoReplyTweetsMedia':
          isProcessing.value = true
          lastProcessedTime.value = currentTime
          inputReplyMediaFileUrls.value = msg.data.replyMediaFileUrls
          inputText.value = msg.data.tweets
          sendLoading.value = true
          console.log('inputTextReplyStatus.value', inputTextReplyStatus.value)

          if (!inputTextReplyStatus.value) {
            inputTextReplyStatus.value = true

            if (inputReplyMediaFileUrls.value.length > 0) {
              messageList.value[messageList.value.length - 1].medias = inputReplyMediaFileUrls.value.map((url) => ({ type: 'image', data: url }))
            }

            console.log('messageList.value[messageList.value.length - 1]', messageList.value[messageList.value.length - 1])

            reasoningRecord.value.inputReplyMediaFileUrls = inputReplyMediaFileUrls.value

            responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'

            responseMessage.value.thinkingList.push({ title: 'Analysis complete', status: 'success' })

            await saveMessage(chatMessage.value)
            await saveMessage(responseMessage.value)

            await addReasoningRecord(reasoningRecord.value)

            isProcessing.value = false
            sendLoading.value = false
          }

          break
        case 'callbackAutoReplyTweetsText':
          isProcessing.value = true
          lastProcessedTime.value = currentTime
          inputTextReplyStatus.value = true
          inputText.value = msg.data.tweets
          sendLoading.value = true

          responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'

          responseMessage.value.thinkingList.push({ title: 'Think', status: 'pending' })

          handleSendMessage({ text: text, inputText: inputText.value, image: '' }).finally(() => {
            isProcessing.value = false
          })
          break
      }
    } catch (error) {
      console.error('error', error)
      isProcessing.value = false
    }
  },
})

window.setInterval(() => {
  send('PING')
}, 10000)

const myAgentList = ref([])

const messageList = ref([])

const showSessionEdit = ref(false)
const isSessionPanelCollapsed = ref(true)

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

const loginUser = ref(null)

const mobileContactListRef = ref(null)

import { useRoute } from 'vue-router'

const route = useRoute()
const sid = route.query.sid

const defaultContent = ref(null)

const isOnline = ref(false)

const defAvatar = ref('https://s3.hyperdust.io/upload/20250411/67f8cbcbe4b0bc355fbb060e.png')

const options = ref<AiMessageParams>({
  enableVectorStore: true,
  enableAgent: false,
  model: '',
  baseUrl: '',
})
const embeddingLoading = ref(false)

const isMobile = ref(false)
const drawerVisible = ref(false)
const showAgentList = ref(false)
const showSessionList = ref(false)

// 添加检测移动端的函数
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

function handleShowAgentList() {
  showAgentList.value = true
  nextTick(() => {
    if (mobileContactListRef.value) {
      mobileContactListRef.value.addEventListener('scroll', handleMobileScroll)
    }
  })
}

const handleMobileScroll = () => {
  if (!mobileContactListRef.value) return

  const { scrollTop, scrollHeight, clientHeight } = mobileContactListRef.value
  if (scrollHeight - scrollTop - clientHeight < 20) {
    loadMore()
  }
}

function handleCloseAgentList() {
  showAgentList.value = false
  if (mobileContactListRef.value) {
    mobileContactListRef.value.removeEventListener('scroll', handleMobileScroll)
  }
}

onMounted(async () => {
  getReplySearch()
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await getDefaultContent()

  if (localStorage.getItem('X-Token')) {
    await getLoginUser()
  }

  await getAgentList()

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
    if (loginUser.value) {
      await getMyAgent()

      handleSelectAgent(myAgentList.value[0])
      selectMyAgentId.value = myAgentList.value[0].id
    } else {
      handleSelectAgent(agentList.value[0])
    }
  }

  // 添加滚动监听
  contactListRef.value?.addEventListener('scroll', handleScroll)
})

async function getReplySearch() {
  const { result } = await request({
    url: BASE_URL + '/sys/dict/getDictText/3d_client_config/reply_search',
    method: 'GET',
    headers: {
      'X-Access-Token': token.value,
    },
  })

  replySearch.value = JSON.parse(result)
}

// 组件卸载时移除监听
onUnmounted(() => {
  contactListRef.value?.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', checkMobile)
})

async function getDefaultContent() {
  const { result } = await request({
    url: BASE_URL + '/mgn/agentNpc/list',
    method: 'GET',
    params: {
      name: 'agentme',
    },
    headers: {
      'X-Access-Token': token.value,
    },
  })
  if (result.records.length > 0) {
    defaultContent.value = result.records[0].alpacaPrompt
  }
}

function generateUUID() {
  const bytes = new Uint8Array(16)
  window.crypto.getRandomValues(bytes)

  bytes[6] = (bytes[6] & 0x0f) | 0x40 // Set the version to 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80 // Set the variant to 10xx

  const uuid = [...bytes].map((byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${uuid.slice(0, 8)}-${uuid.slice(8, 12)}-${uuid.slice(12, 16)}-${uuid.slice(16, 20)}-${uuid.slice(20)}`
}

function handleCreateMyAgent() {
  window.open('https://www.hyperagi.network/store')
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
  showSessionList.value = false
  activeSession.value = session
  getMessageList()
}

async function getCurrAgentOnlineStatus() {
  const { result } = await request({
    url: BASE_URL + '/mgn/agent/list',
    method: 'GET',
    params: {
      sid: selectAgent.value.sid,
    },
    headers: {
      'X-Access-Token': token.value,
    },
  })

  const agent = result.records[0]
  return agent.onlineStatus === 1
}

const preHandleSendMessage = async (message: { text: string; image: string }) => {
  inputTextReplyStatus.value = false
  sendLoading.value = true
  inputText.value = message.text

  isOnline.value = await getCurrAgentOnlineStatus()

  // Image/Audio
  const medias: AiMessage['medias'] = []
  if (message.image) {
    medias.push({ type: 'image', data: message.image })
  }

  for (const item of messageList.value) {
    item.thinkingList = null
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
    thinkingList: [],
  }

  chatMessage.value = {
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

  messageList.value.push(chatMessage.value)
  messageList.value.push(responseMessage.value)

  // 确保在下一个 tick 时滚动到底部
  await nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTo({
        top: messageListRef.value.scrollHeight,
        behavior: 'smooth',
      })
    }
  })

  if (isOnline.value) {
    const msg = {
      action: 'autoReplyTweetsText',
      data: {
        replyTweetsRecordId: wsUserId,
        tweets: inputText.value,
      },
    }

    if (options.value.enableAgent) {
      responseMessage.value.thinkingList.push({ title: 'Think', status: 'pending' })
      handleSendMessage({ text: message.text, inputText: message.text, image: '' })
    } else {
      await request.post(BASE_URL + '/ws/socketMsg/sendSocketMsg', {
        userId: selectAgent.value.owner,
        msg: JSON.stringify(msg),
      })

      responseMessage.value.thinkingList.push({ title: 'Observing the environment', status: 'pending' })

      setTimeout(() => {
        console.info('inputTextReplyStatus.value', inputTextReplyStatus.value)
        if (!inputTextReplyStatus.value) {
          responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'
          responseMessage.value.thinkingList.push({ title: 'Think', status: 'pending' })
          handleSendMessage({ text: message.text, inputText: message.text, image: '' })
        }
      }, 10 * 1000)
    }
  } else {
    responseMessage.value.thinkingList.push({ title: 'Think', status: 'pending' })

    handleSendMessage({ text: message.text, inputText: message.text, image: '' })
  }
}

const handleSendMessage = async (message: { text: string; inputText: string; image: string }) => {
  if (!activeSession.value) {
    ElMessage.warning('Please create a session')
    return
  }

  const form = new FormData()

  let content = ''

  let agentName = ''

  options.value.userId = selectAgent.value.sid

  if (!options.value.enableAgent) {
    agentName = selectAgent.value.nickName

    content = selectAgent.value.personalization
    if (!content) {
      content = defaultContent.value
    }

    content = content.replace('[agent name]', agentName)
  }

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

  const body: AiMessageWrapper = { message: chatMessage.value, params: options.value }

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
      evtSource.close()

      chatMessage.value.textContent = message.text
      // if (message.mediaFileUrls) {
      //   responseMessage.value.medias = message.mediaFileUrls.map((url) => ({ type: 'image', data: url }))
      // }

      reasoningRecord.value = {
        inputContent: message.inputText,
        outContent: responseMessage.value.textContent,
        agentId: selectAgent.value.sid,
        userId: loginUser.value?.id || '',
        serviceName: options.value.model,
        systemContent: content,
        remark: options.value.baseUrl,
        nodeId: res.result.minerNodeId,
      }

      let avatar = loginUser.value ? loginUser.value.avatar : ''

      if (!avatar) {
        avatar = defAvatar.value
      }

      inputTextReplyStatus.value = false

      if (options.value.enableAgent || !isOnline.value) {
        sendLoading.value = false
        await saveMessage(chatMessage.value)
        await saveMessage(responseMessage.value)

        await addReasoningRecord(reasoningRecord.value)

        inputTextReplyStatus.value = true
        responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'

        responseMessage.value.thinkingList.push({ title: 'Analysis complete', status: 'success' })
      } else {
        const type = await isPhotoOrCelebrity(message.text)

        if (type === 'celebrity') {
          responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'
          responseMessage.value.thinkingList.push({ title: 'Generating postcard', status: 'pending' })
        } else if (type === 'photo') {
          responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'
          responseMessage.value.thinkingList.push({ title: 'Generating images', status: 'pending' })
        }

        if (type === 'other') {
          isProcessing.value = false
          sendLoading.value = false
          responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'
          responseMessage.value.thinkingList.push({ title: 'Analysis complete', status: 'success' })
        } else {
          const msg = {
            action: 'autoReplyTweetsMedia',
            data: {
              replyTweetsRecordId: wsUserId,
              tweets: chatMessage.value.textContent,
              replyTweets: responseMessage.value.textContent,
              replyTweetsName: selectAgent.value.xusername || selectAgent.value.nickName,
              authorName: '',
              authorUserName: loginUser.value?.walletAddress ? `${loginUser.value.walletAddress.slice(0, 6)}...${loginUser.value.walletAddress.slice(-4)}` : '',
              avatar: avatar,
            },
          }
          await request.post(BASE_URL + '/ws/socketMsg/sendSocketMsg', {
            userId: selectAgent.value.owner,
            msg: JSON.stringify(msg),
          })

          setTimeout(async () => {
            if (!inputTextReplyStatus.value) {
              inputTextReplyStatus.value = true
              isProcessing.value = false
              await saveMessage(chatMessage.value)
              await saveMessage(responseMessage.value)

              await addReasoningRecord(reasoningRecord.value)

              sendLoading.value = false
              responseMessage.value.thinkingList[responseMessage.value.thinkingList.length - 1].status = 'success'
              responseMessage.value.thinkingList.push({ title: 'Analysis complete', status: 'success' })
            }
          }, 30 * 1000)
        }
      }
    }
  })

  // Call stream to initiate request
  evtSource.stream()

  // Display both messages on page

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

async function getMyAgent() {
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

  myAgentList.value = result.records
}

// 修改获取agent列表的方法，添加搜索参数
async function getAgentList(isLoadMore = false) {
  if (loading.value || noMore.value) return

  try {
    const params = {
      pageNo: pageNum.value,
      pageSize: pageSize.value,
      userOrderNum: true,
      showStatus: 'Y',
      noWalletAddress: loginUser.value ? loginUser.value.walletAddress : '',
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
  try {
    const res = await request({
      url: BASE_URL + '/sys/getCurrUser',
      headers: {
        'X-Access-Token': token.value,
      },
      method: 'GET',
    })

    loginUser.value = res.result
  } catch (error) {
    console.error(error)
  }
}

const disconnect = () => {
  localStorage.removeItem('X-Token')
  location.reload()
}

const goHome = () => {
  location.href = `https://mossai.com/`
}

function goUser() {
  location.href = `https://user.mossai.com/login?token=${token.value}`
}

function showUploadEmbedding() {
  uploadEmbeddingRef.value?.show(selectAgent.value.id)
}

const handleBindTwitter = () => {
  const data = {
    token: token.value,
    id: selectAgent.value.sid,
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

const preHandleSelectAgent = (agent) => {
  selectMyAgentId.value = ''

  handleSelectAgent(agent)
}

// 添加选中方法
const handleSelectAgent = (_agent) => {
  showAgentList.value = false
  if (_agent) {
    selectAgent.value = _agent
    selectAgentId.value = _agent.id
  } else {
    selectAgent.value = agentList.value[0]
    selectAgentId.value = agentList.value[0].id
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

async function isPhotoOrCelebrity(question: string) {
  // Keywords related to celebrity photos
  const celebrityKeywords = replySearch.value['明信片']

  // Keywords related to taking photos
  const photoKeywords = replySearch.value['拍照']

  // Check for celebrity keywords
  const isCelebrity = celebrityKeywords.some((keyword) => {
    // For English words, compare in lowercase
    if (/^[a-zA-Z\s]+$/.test(keyword)) {
      return question.toLowerCase().includes(keyword.toLowerCase())
    }
    // For Chinese characters, compare directly
    return question.includes(keyword)
  })

  // Check for photo keywords
  const isPhoto = photoKeywords.some((keyword) => {
    if (/^[a-zA-Z\s]+$/.test(keyword)) {
      return question.toLowerCase().includes(keyword.toLowerCase())
    }
    return question.includes(keyword)
  })

  if (isCelebrity) {
    return 'celebrity'
  } else if (isPhoto) {
    return 'photo'
  }

  return 'other'
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
          sid: selectAgent.value.sid,
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

const toggleSessionPanel = () => {
  isSessionPanelCollapsed.value = !isSessionPanelCollapsed.value
}
</script>
<template>
  <div class="home-view dark">
    <!-- LOGO部分调整到最左边 -->
    <div class="w-full flex items-start px-4 py-3 border-b border-gray-700 fixed top-0 left-0 z-10 overflow-hidden">
      <div class="flex items-center justify-between w-full">
        <div class="flex items-center">
          <!-- 添加移动端菜单按钮 -->
          <template v-if="isMobile">
            <el-button class="mr-0.5 dark-button" @click="handleShowAgentList">
              <el-icon><Menu /></el-icon>
            </el-button>
            <el-button class="mr-0.5 dark-button" style="margin-left: 0.1rem" @click="showSessionList = !showSessionList">
              <el-icon><ChatRound /></el-icon>
            </el-button>
          </template>
          <div class="flex items-center cursor-pointer w-[10rem]">
            <img @click="goHome" src="../../assets/logo2.png" loading="lazy" class="cursor-pointer w-8 h-8" alt="logo" />
            <span @click="goHome" class="text-white text-2xl font-bold ml-2">MOSS&nbsp;AI</span>
          </div>
        </div>

        <!-- 将登录按钮移到logo旁边 -->
        <div v-if="!loginUser" class="user-login-btn" @click="handleLogin">
          <el-avatar :size="16" :src="user" style="border: none" />
          <span class="ml-1.25 text-white mobile:ml-0.5">Login In</span>
        </div>
        <el-dropdown v-else class="user-dropdown">
          <span class="el-dropdown-link flex items-center">
            <el-avatar :size="16" :src="loginUser.avatar || defAvatar" style="border: none" />

            <span class="ml-1.25 mobile:ml-0.5">
              <Substring :copys="false" color="#ffffff" fontSize="13px" :value="loginUser.walletAddress || loginUser.email"></Substring>
            </span>

            <el-icon size="13" class="el-icon--right mobile:hidden">
              <arrow-down />
            </el-icon>
          </span>
          <template #dropdown>
            <el-card class="w-50 bg-[#303133] text-white mobile:w-40">
              <template #header>
                <div class="card-header flex items-center">
                  <el-avatar :size="25" :src="loginUser.avatar || defAvatar" />

                  <span class="ml-3.75 mobile:ml-2">
                    <Substring fontSize="12px" :value="loginUser.walletAddress || loginUser.email"></Substring>
                  </span>
                </div>
              </template>

              <p class="ml-1.25 flex items-center">
                <el-icon size="20" class="text-white">
                  <User />
                </el-icon>

                <el-button type="text" @click="goUser" style="color: white" class="text-xs text-white" link>Dashboard</el-button>
              </p>

              <p class="ml-1.25 flex items-center mt-7.5">
                <el-image :src="logoutPng" class="w-5 h-5"></el-image>

                <el-button type="text" @click="disconnect" style="color: white" class="text-xs text-white" link>Disconnect</el-button>
              </p>
            </el-card>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Entire chat panel -->
    <div class="chat-panel" v-loading="loading">
      <!-- 在非移动端显示联系人列表 -->
      <div v-if="!isMobile" class="contact-panel w-70 border-r border-gray-700 bg-[#1e1e1e] h-full">
        <!-- 其他内容添加padding -->
        <div class="p-4">
          <div class="text-white text-lg mb-4">My Agent</div>
          <div class="space-y-4 mb-6">
            <el-select v-if="myAgentList.length > 0" v-model="selectMyAgentId" clearable placeholder="Select an agent" @change="(val) => handleSelectAgent(myAgentList.find((a) => a.id === val))">
              <el-option v-for="(myAgent, index) in myAgentList" :key="index" :label="myAgent.nickName" :value="myAgent.id" class="dark-option">
                <div class="flex items-center space-x-3">
                  <div class="relative">
                    <el-avatar :size="40" :src="myAgent.avatar" />
                    <div v-if="myAgent.onlineStatus === 1" class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1e1e1e]"></div>
                    <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1e1e1e]"></div>
                  </div>
                  <span class="text-sm text-white">{{ myAgent.nickName }}</span>
                </div>
              </el-option>
            </el-select>

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
              <div v-for="agent in agentList" :key="agent.id" class="flex items-center space-x-3 p-2 bg-[#1e1e1e] hover:bg-[#2c2c2c] rounded-lg cursor-pointer transition-colors duration-200" :class="{ 'bg-[#2c2c2c]': selectAgentId === agent.id }" @click="preHandleSelectAgent(agent)">
                <div class="relative">
                  <el-avatar :size="40" :src="agent.avatar" />
                  <div v-if="agent.onlineStatus === 1" class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1e1e1e]"></div>
                </div>
                <div>
                  <div class="text-white text-sm flex items-center">
                    {{ agent.nickName }}
                    <img v-if="agent.xname" src="../../assets/x.svg" alt="X" class="w-4 h-4 ml-6 mt-2" />
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
      <div v-if="!isMobile" class="session-panel" :class="{ collapsed: isSessionPanelCollapsed }">
        <div class="button-wrapper mt-20">
          <div class="flex items-center justify-between w-full">
            <div class="toggle-panel-btn cursor-pointer flex items-center py-10 text-sm hover:bg-gray-700 rounded" @click="toggleSessionPanel">
              <el-icon :size="20" style="color: white">
                <component :is="isSessionPanelCollapsed ? 'ArrowRight' : 'ArrowLeft'" />
              </el-icon>
            </div>

            <div v-if="!isSessionPanelCollapsed" class="create-session-btn cursor-pointer flex items-center px-4 py-2 text-sm hover:bg-gray-700 rounded" @click="handleSessionCreate">
              <img src="../../assets/create.png" alt="create" class="create-icon w-5 h-5" />
            </div>
          </div>
        </div>

        <div class="session-list h-[calc(80vh-80px)] overflow-y-auto custom-scrollbar" v-if="activeSession && !isSessionPanelCollapsed">
          <session-item v-for="session in sessionList" :key="session.id" :active="session.id === activeSession.id" :session="session" class="session" @click="handleSelectSession(session)" @delete="handleDeleteSession(session.id)" />
        </div>

        <div class="option-panel" v-if="!isSessionPanelCollapsed">
          <el-form size="small" v-if="selectMyAgentId" class="rag-form">
            <el-form-item label-width="8.2rem" label="RAG Knowledge" class="form-item-align">
              <el-button class="ml-0" :style="{ backgroundColor: '#2d2736', color: 'white', border: 'aliceblue' }" @click="showUploadEmbedding">
                <img style="width: 15px; height: 15px" src="../../assets/docUpload.svg" alt="upload" />
              </el-button>
            </el-form-item>
            <el-form-item label="Enable Knowledge Base" class="form-item-align">
              <el-switch class="ml-0" v-model="options.enableVectorStore" style="--el-switch-on-color: #13ce66"></el-switch>
            </el-form-item>
            <el-form-item v-if="!selectAgent || !selectAgent.xname" label="Bind X" class="form-item-align">
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
        <div class="header" :class="{ 'mt-50': isMobile }" v-if="activeSession">
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
          <transition-group name="list" v-if="activeSession && selectAgent">
            <message-row v-for="message in messageList" :defAgentAvatar="selectAgent.avatar" :avatar="message.avatar" :key="message.id" :message="message"></message-row>
          </transition-group>
        </div>
        <!-- Listen for send event -->
        <message-input @send="preHandleSendMessage" :loading="sendLoading" @search="handleSearchWeb" :functionStatus="selectAgent.functionStatus" v-if="activeSession && selectAgent"></message-input>

        <Login ref="loginRef" />

        <UploadEmbedding ref="uploadEmbeddingRef"></UploadEmbedding>
      </div>
    </div>

    <!-- 移动端抽屉 -->
    <el-drawer v-model="showAgentList" @close="handleCloseAgentList" direction="ltr" size="80%" :with-header="false" class="mobile-drawer dark-drawer overflow-hidden">
      <div class="drawer-content">
        <!-- 联系人列表 -->
        <div class="contact-panel bg-[#1e1e1e] h-full overflow-hidden">
          <div class="p-4">
            <div class="text-white text-lg mb-4">My Agent</div>
            <div class="space-y-4 mb-6">
              <el-select v-if="myAgentList.length > 0" v-model="selectMyAgentId" clearable placeholder="Select an agent" @change="(val) => handleSelectAgent(myAgentList.find((a) => a.id === val))">
                <el-option v-for="(myAgent, index) in myAgentList" :key="index" :label="myAgent.nickName" :value="myAgent.id" class="dark-option">
                  <div class="flex items-center space-x-3">
                    <div class="relative">
                      <el-avatar :size="40" :src="myAgent.avatar" />
                      <div class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1e1e1e]"></div>
                    </div>
                    <span class="text-sm text-white">{{ myAgent.nickName }}</span>
                  </div>
                </el-option>
              </el-select>

              <div v-else>
                <div class="flex items-center justify-center p-2 hover:bg-gray-700 rounded-lg cursor-pointer transition-colors duration-200">
                  <el-button @click="handleCreateMyAgent" round type="primary" class="w-full">Create My Agent</el-button>
                </div>
              </div>
            </div>

            <div class="text-white text-lg mb-4">Agent List</div>
            <div class="mb-4">
              <el-input v-model="searchQuery" placeholder="Search agents..." class="w-full" :prefix-icon="Search"></el-input>
            </div>
            <div ref="mobileContactListRef" class="h-[calc(100vh-10px)] overflow-y-auto custom-scrollbar" style="max-height: calc(90% - 120px)">
              <div class="space-y-2">
                <div v-for="agent in agentList" :key="agent.id" class="flex items-center space-x-3 p-2 bg-[#1e1e1e] hover:bg-[#2c2c2c] rounded-lg cursor-pointer transition-colors duration-200" :class="{ 'bg-[#2c2c2c]': selectAgentId === agent.id }" @click="preHandleSelectAgent(agent)">
                  <div class="relative">
                    <el-avatar :size="40" :src="agent.avatar" />
                    <div v-if="agent.onlineStatus === 1" class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1e1e1e]"></div>
                  </div>
                  <div>
                    <div class="text-white text-sm flex items-center">
                      {{ agent.nickName }}
                      <img v-if="agent.xname" src="../../assets/x.svg" alt="X" class="w-4 h-4 ml-6 mt-2" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 会话列表抽屉 -->
    <el-drawer v-model="showSessionList" direction="ltr" size="80%" :with-header="false" class="mobile-drawer dark-drawer">
      <div class="drawer-content">
        <div class="session-list mt-4">
          <div class="text-white text-lg mb-4 px-4">Sessions</div>

          <div class="create-session-btn cursor-pointer flex items-center px-4 py-2 text-sm hover:bg-gray-700 rounded" @click="handleSessionCreate">
            <img src="../../assets/create.png" alt="create" class="create-icon w-8 h-8" />
          </div>

          <div class="h-[calc(95vh-10vh)] overflow-y-auto mt-10 custom-scrollbar">
            <session-item v-for="session in sessionList" :key="session.id" :active="session.id === activeSession?.id" :session="session" class="session" @click="handleSelectSession(session)" @delete="handleDeleteSession(session.id)" />
          </div>
        </div>
      </div>
    </el-drawer>
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

  .w-full {
    &.flex.items-start {
      padding: 8px 4px;
      height: 60px;
      align-items: center;
      background-color: #000000;
      border-bottom: 1px solid #303133;

      .flex.items-center {
        gap: 4px;
      }

      // 添加logo容器样式
      .flex.items-center.cursor-pointer {
        margin-left: -4px;
        padding-left: 4px;
      }

      // 添加logo图片样式
      img {
        width: 4rem;
        height: 4rem;
      }

      // 添加logo文字样式
      .text-2xl {
        font-size: 1rem;
        margin-left: 0.25rem;
      }

      @media screen and (max-width: 768px) {
        .flex.items-center.cursor-pointer {
          padding-left: 0;
        }
      }
    }
  }

  .chat-panel {
    margin: 0 auto;
    width: 99%;
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
      padding: 10px;
      position: relative;
      border-right: 1px solid rgba(255, 255, 255, 0.07);
      background-color: #141414;
      height: 100%;
      overflow: hidden;
      width: 64px;
      transition: width 0.3s ease;

      &.collapsed {
        width: 40px;
      }

      &:not(.collapsed) {
        width: 350px;
      }

      .button-wrapper {
        display: flex;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        padding: 10px 0;
      }

      .toggle-panel-btn {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        transition: all 0.3s ease;
        background-color: #1e1e1e;
        cursor: pointer;
        margin: 0;
        padding: 0;
        border: none;
        outline: none;

        &:hover {
          background-color: rgba(255, 255, 255, 0.1);
        }

        .el-icon {
          color: white;
          font-size: 20px;
        }
      }

      .create-session-btn {
        margin-left: auto;
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
    }

    /* Right message history panel */
    .message-panel {
      width: calc(100%);
      height: 95%;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      position: relative;
      touch-action: none;
      -webkit-overflow-scrolling: none;
      overscroll-behavior: none;
      -webkit-overscroll-behavior: none;

      @media screen and (max-width: 768px) {
        width: 100vw;
        max-width: 100vw;
        height: 100%;
        overflow: hidden;
        touch-action: none;
        -webkit-overflow-scrolling: none;
        overscroll-behavior: none;
        -webkit-overscroll-behavior: none;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 1;
      }

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
        padding-bottom: 100px;
        width: 100%;
        flex: 1;
        box-sizing: border-box;
        overflow-y: auto;
        overflow-x: hidden;
        touch-action: pan-y;
        max-width: 100vw;
        scroll-behavior: smooth;
        margin-bottom: 20px;
        overscroll-behavior: none;
        -webkit-overscroll-behavior: none;

        @media screen and (max-width: 768px) {
          overflow-y: auto;
          overflow-x: hidden;
          touch-action: pan-y;
          max-width: 100vw;
          overscroll-behavior: none;
          -webkit-overscroll-behavior: none;
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

// Global scrollbar hiding
:deep(*) {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

// Hide scrollbars for message list
.message-list {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

// Hide scrollbars for session list
.session-list {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

// Hide scrollbars for contact list
.contact-panel {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

// Hide scrollbars for drawer content
.drawer-content {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

// Hide scrollbars for el-select dropdown
:deep(.el-select-dropdown__wrap) {
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
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

:deep(.el-select) {
  .el-input__wrapper {
    background-color: #1e1e1e !important;
    box-shadow: 0 0 0 1px #303133 inset;

    &:hover {
      box-shadow: 0 0 0 1px #409eff inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .el-input__inner {
    color: #ffffff !important;

    &::placeholder {
      color: #606266;
    }
  }
}

:deep(.el-select-dropdown) {
  background-color: #1e1e1e !important;
  border: 1px solid #303133;

  .el-select-dropdown__item {
    color: #ffffff;

    &:hover {
      background-color: #2c2c2c;
    }

    &.selected {
      color: #409eff;
      background-color: #2c2c2c;
    }
  }
}

.dark-select {
  .el-input__wrapper {
    background-color: #1e1e1e !important;
    box-shadow: 0 0 0 1px #303133 inset;

    &:hover {
      box-shadow: 0 0 0 1px #409eff inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .el-input__inner {
    color: #ffffff !important;

    &::placeholder {
      color: #606266;
    }
  }
}

:deep(.dark-option) {
  background-color: #1e1e1e !important;
  color: #ffffff !important;

  &:hover {
    background-color: #2c2c2c !important;
  }

  &.selected {
    color: #409eff !important;
    background-color: #2c2c2c !important;
  }
}

// 添加全局暗黑模式样式
.dark {
  .el-drawer {
    :deep {
      background-color: #1e1e1e;
    }

    :deep(.el-drawer__header) {
      background-color: #1e1e1e;
      border-bottom: 1px solid #303133;
      margin-bottom: 0;
      padding: 12px 16px;
      color: white;
    }
  }

  .el-card {
    :deep {
      background-color: #1e1e1e;
      border-color: #303133;
    }

    :deep(.el-card__header) {
      border-bottom-color: #303133;
    }
  }
}

.mobile-drawer {
  :deep(.el-drawer) {
    background-color: #1e1e1e;
    border: none;
    overflow-x: hidden;
    touch-action: pan-y;
    max-width: 100vw;
    width: 100% !important;
  }

  :deep(.el-drawer__header) {
    color: white;
    border-bottom: 1px solid #303133;
  }

  :deep(.el-drawer__body) {
    padding: 0;
    overflow: hidden;
    overflow-x: hidden;
    touch-action: pan-y;
    max-width: 100vw;
    width: 100%;
  }
}

.drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #000;
  overflow-x: hidden;
  width: 100%;
  touch-action: pan-y;
  position: relative;
  max-width: 100vw;

  .session-list {
    min-height: 800px;
    width: 100%;
    overflow-x: hidden;
    touch-action: pan-y;
    position: relative;
    max-width: 100vw;

    .session {
      margin-bottom: 10px;
      width: 100%;
      position: relative;
      max-width: 100vw;
    }
  }
}

// Add these global styles to prevent horizontal scrolling on mobile
@media screen and (max-width: 768px) {
  html,
  body {
    overflow-x: hidden;
    width: 100%;
    position: relative;
    max-width: 100vw;
  }

  .el-drawer__wrapper {
    overflow-x: hidden;
    touch-action: pan-y;
    max-width: 100vw;
  }

  .el-drawer__container {
    overflow-x: hidden;
    touch-action: pan-y;
    max-width: 100vw;
  }

  .home-view {
    width: 100vw;
    max-width: 100vw;
    overflow-x: hidden;
  }

  .chat-panel {
    width: 100vw;
    max-width: 100vw;
    overflow-x: hidden;
  }

  .message-panel {
    width: 100vw;
    max-width: 100vw;
    overflow-x: hidden;
  }

  .message-list {
    overflow-x: hidden;
    touch-action: pan-y;
    max-width: 100vw;
    width: 100%;
  }
}

// Add these styles to prevent horizontal scrolling on all elements
* {
  max-width: 100vw;
  overflow-x: hidden;
  box-sizing: border-box;
}

// 自定义滚动条样式
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: #666 #1e1e1e;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: #1e1e1e;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #666;
    border-radius: 4px;
  }
}

// 消息列表过渡动画
.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.toggle-btn {
  background: transparent;
  border: none;
  padding: 0;
  color: white;

  &:hover {
    background: transparent;
    color: #409eff;
  }
}

.dark-button {
  background-color: #1e1e1e !important;
  border-color: #303133 !important;
  color: white !important;

  &:hover {
    background-color: #2c2c2c !important;
    border-color: #409eff !important;
  }

  &:active {
    background-color: #2c2c2c !important;
    border-color: #409eff !important;
  }
}

.user-dropdown {
  background-color: #303133;
  border-radius: 9999px;
  height: 28px;
  width: 160px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background-color: #2c2c2c;
  }

  @media screen and (max-width: 768px) {
    width: 140px;
  }
}

.user-login-btn {
  background-color: #303133;
  border-radius: 9999px;
  height: 28px;
  width: 160px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background-color: #2c2c2c;
  }

  @media screen and (max-width: 768px) {
    width: 140px;
    margin-right: 0;
  }
}

.create-session-btn {
  margin: 0 16px;

  img {
    transition: transform 0.3s ease;
  }

  &:hover {
    img {
      transform: rotate(90deg);
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

// Add styles for the message input container
.message-input-container {
  position: sticky;
  bottom: 0;
  width: 100%;
  background-color: #1e1e1e;
  padding: 15px;
  z-index: 10;
}
</style>
