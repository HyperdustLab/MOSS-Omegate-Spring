<script setup lang="ts">
import { Check, Warning } from '@element-plus/icons-vue'

interface ThoughtChainItem {
  title: string
  status: 'success' | 'error' | 'pending'
}

defineProps<{
  items: ThoughtChainItem[]
}>()
</script>

<template>
  <div class="thought-chain">
    <el-timeline>
      <el-timeline-item v-for="(item, index) in items" :key="index" :type="item.status === 'error' ? 'danger' : 'success'" :icon="item.status === 'error' ? Warning : Check" :color="item.status === 'error' ? '#ff4949' : '#67c23a'">
        <div class="timeline-content">
          <h4>{{ item.title }}</h4>
          <p class="status-text" :class="item.status">status: {{ item.status }}</p>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped lang="scss">
.thought-chain {
  padding: 20px;
  background: #1e1e1e;
  border-radius: 8px;

  .run-next-btn {
    margin-bottom: 20px;
  }

  :deep(.el-timeline) {
    padding-left: 16px;
  }

  :deep(.el-timeline-item__node) {
    background-color: transparent;
    border: none;
  }

  :deep(.el-timeline-item__tail) {
    border-left: 2px solid #4a4a4a;
  }

  .timeline-content {
    h4 {
      color: #ffffff;
      margin: 0 0 8px 0;
      font-size: 16px;
    }

    .status-text {
      margin: 0;
      font-size: 14px;

      &.success {
        color: #67c23a;
      }

      &.error {
        color: #ff4949;
      }

      &.pending {
        color: #909399;
      }
    }
  }
}
</style>
