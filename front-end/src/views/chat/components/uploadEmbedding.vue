<template>
  <el-dialog v-model="dialogVisible" title="上传文件" width="30%" :before-close="handleClose">
    <el-upload class="upload-demo" drag multiple action="#" :auto-upload="false" :on-change="handleChange" :before-upload="beforeUpload" :file-list="fileList">
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持 doc、txt 格式文件,单个文件不超过 2MB</div>
      </template>
    </el-upload>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleUpload"> 确认上传 </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadProps, UploadUserFile } from 'element-plus'

const dialogVisible = ref(false)
const fileList = ref<UploadUserFile[]>([])

const handleClose = () => {
  dialogVisible.value = false
  fileList.value = []
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isDoc = file.type === 'application/msword' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const isTxt = file.type === 'text/plain'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isDoc && !isTxt) {
    ElMessage.error('上传文件只能是 doc/txt 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('上传文件大小不能超过 2MB!')
    return false
  }
  return true
}

const handleChange: UploadProps['onChange'] = (file) => {
  fileList.value.push(file)
}

const handleUpload = () => {
  // 这里处理文件上传逻辑
  console.log('上传文件列表:', fileList.value)
  handleClose()
}

function show() {
  dialogVisible.value = true
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
