<script setup lang="ts">
import { ref, watch, onBeforeMount } from 'vue'

import { ElMessage } from 'element-plus'

// 导入插件
import useClipboard from 'vue-clipboard3'

const { toClipboard } = useClipboard()
const props = defineProps({
  value: { type: String, required: true },
  copys: { type: Boolean, default: true },
  color: { type: String, default: '#a88a18' },
  fontSize: { type: String, default: '14px' },
  sub: { type: Boolean, default: true },
})

const text = ref('')

onBeforeMount(() => {
  substring()
})

function substring() {
  if (props.value && props.value.length > 12 && props.sub) {
    let start = props.value.substring(0, 6)
    let end = props.value.substring(props.value.length - 6, props.value.length)
    text.value = start + '***' + end
  } else {
    text.value = props.value
  }
}

// 点击复制
function touchCopy() {
  // 调用
  copy(props.value)
}

watch(
  () => props.value,
  (newValue, oldValue) => {
    // 这里是你的处理逻辑

    substring()
  }
)

// 使用插件

const copy = async (msg) => {
  try {
    // 复制
    await toClipboard(msg)

    ElMessage.success('Copied successfully')

    // 复制成功
  } catch (e) {
    // 复制失败
  }
}
</script>

<template>
  <span class="address spaceBetween">
    <span :style="{ color: color, 'font-size': fontSize }">{{ text }}</span>
    <el-icon style="margin-left: 10px; color: #fff" @click.stop="touchCopy" v-if="copys">
      <CopyDocument />
    </el-icon>
  </span>
</template>

<style lang="scss">
.address {
  span {
    color: #a88a18;
  }

  .copy {
    margin-left: 14px;
  }
}
</style>
