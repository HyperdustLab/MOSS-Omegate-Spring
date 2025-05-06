<template>
  <el-dialog v-model="dialogVisible" title="Knowledge Base Upload" width="30%" :before-close="handleClose" class="custom-dialog">
    <el-upload
      class="upload-demo"
      drag
      multiple
      :action="action"
      :headers="{ 'X-Access-Token': token }"
      :auto-upload="true"
      :on-change="handleChange"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      v-model:file-list="fileList"
      :show-file-list="true"
      list-type="text"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">Drop files here or <em>click to upload</em></div>
      <template #tip>
        <div class="el-upload__tip">Supports doc and txt files, max 2MB per file</div>
      </template>
    </el-upload>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">Cancel</el-button>
        <el-button type="primary" @click="handleUpload">Confirm Upload</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { request } from '@/utils/request'
const BASE_URL = import.meta.env.VITE_API_HYPERAGI_API

const action = `${BASE_URL}/sys/common/ipfsUpload`

const token = ref(localStorage.getItem('X-Token'))

const myAgentId = ref('')

const dialogVisible = ref(false)
const fileList = ref<UploadUserFile[]>([])

const handleClose = () => {
  dialogVisible.value = false
  fileList.value = []
}

const agentId = ref('')

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isDoc = file.type === 'application/msword' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const isTxt = file.type === 'text/plain'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isDoc && !isTxt) {
    ElMessage.error('Only doc/txt files are allowed!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('File size cannot exceed 2MB!')
    return false
  }
  return true
}

const handleChange: UploadProps['onChange'] = (uploadFile) => {
  const isExist = fileList.value.some((item) => item.name === uploadFile.name)
  if (!isExist) {
    console.log('File selected:', uploadFile.name)
  }
}

const handleSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  ElMessage.success(`${uploadFile.name} uploaded successfully`)
  console.log('Upload successful:', response)
}

const handleError: UploadProps['onError'] = (error, uploadFile) => {
  ElMessage.error(`${uploadFile.name} upload failed`)
  console.error('Upload failed:', error)
}

const handleUpload = async () => {
  const successFiles = fileList.value.filter((file) => file.status === 'success')
  console.log('Successfully uploaded files:', fileList.value)

  const value = fileList.value.map((file) => {
    return {
      name: file.name,
      link: file.response.result,
    }
  })

  const data = {
    id: agentId.value,
    documents: JSON.stringify({ value: JSON.stringify(value) }),
  }

  const { success, message } = await request({
    url: `${BASE_URL}/mgn/agent/edit`,
    method: 'POST',
    headers: {
      'X-Access-Token': token.value,
    },
    data,
  })

  if (success) {
    ElMessage.success('Upload successful')
    handleClose()
  } else {
    ElMessage.error(message)
  }
}

function show(agentId: string) {
  dialogVisible.value = true
  myAgentId.value = agentId
  initFileList()
}

onMounted(() => {
  if (dialogVisible.value) {
    initFileList()
  }
})

async function initFileList() {
  const { result } = await request({
    url: `${BASE_URL}/mgn/agent/list`,
    method: 'GET',
    params: {
      id: myAgentId.value,
    },
    headers: {
      'X-Access-Token': localStorage.getItem('X-Token') || '',
    },
  })

  const agent = result.records[0]

  agentId.value = agent.id
  const documentsStr = agent.documents

  if (!documentsStr) {
    return
  }

  const fileListStr = JSON.parse(documentsStr).value

  if (!fileListStr) {
    return
  }

  const existingFiles = JSON.parse(fileListStr)

  fileList.value = existingFiles.map((item: any) => ({
    name: item.name,
    response: {
      result: item.link,
    },
    status: 'success',
    uid: Date.now() + '_' + Math.random().toString(36).slice(2),
  }))

  console.log('Updated fileList:', fileList.value)
}

defineExpose({
  show,
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

:deep(.el-upload__tip) {
  color: #909399;
  font-size: 12px;
  margin-top: 7px;
}

:deep(.el-upload-dragger) {
  background-color: #1e1e1e;
  border: 1px dashed #606266;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

:deep(.el-icon--upload) {
  color: #909399;
}

:deep(.el-upload__text) {
  color: #909399;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}
</style>
