<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { ElProgress } from 'element-plus'

const props = defineProps<{
  step: string
  progress?: number
}>()

const currentProgress = ref(0)

onMounted(() => {
  // 如果没有提供进度，则自动增加进度
  if (!props.progress) {
    const interval = setInterval(() => {
      if (currentProgress.value < 95) {
        currentProgress.value += Math.random() * 15
      }
    }, 1000)

    return () => clearInterval(interval)
  }
})
</script>

<template>
  <div class="thinking-process">
    <div class="thinking-content">
      <div class="thinking-step">{{ step }}</div>
      <el-progress :percentage="props.progress || currentProgress" :show-text="false" :stroke-width="4" class="progress-bar" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.thinking-process {
  padding: 12px 16px;
  background-color: #2b2b2b;
  border-radius: 12px;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  max-width: 85%;
  width: 15rem;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  .thinking-content {
    .thinking-step {
      font-size: 15px;
      margin-bottom: 8px;
      color: #e6e6e6;
    }

    .progress-bar {
      :deep(.el-progress-bar__inner) {
        background-color: #409eff;
        transition: all 0.3s linear;
      }
      :deep(.el-progress-bar__outer) {
        background-color: rgba(255, 255, 255, 0.1);
      }
    }
  }
}

// 响应式样式
@media screen and (max-width: 768px) {
  .thinking-process {
    padding: 10px 14px;

    .thinking-content {
      .thinking-step {
        font-size: 14px;
      }
    }
  }
}
</style>
