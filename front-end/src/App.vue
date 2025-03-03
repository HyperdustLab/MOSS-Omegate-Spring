<script setup lang="ts">
import { RouterView } from 'vue-router'
import enUs from 'element-plus/dist/locale/en.mjs'
</script>

<template>
  <el-config-provider :locale="enUs">
    <router-view v-slot="{ Component }">
      <transition name="slide">
        <component :is="Component" />
      </transition>
    </router-view>
  </el-config-provider>
</template>

<style lang="scss">
.footer {
  background-color: #333; /* 更改为深色背景 */
  padding: 20px 0;
  text-align: center;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-link {
  color: #fff; /* 更改为浅色文字 */
  text-decoration: none;
  margin-right: 20px;
}

.footer-link:hover {
  text-decoration: underline;
}

.footer-copyright {
  color: #aaa; /* 更改为浅色文字 */
  font-size: 14px;
}

.dark-message-box {
  background-color: #141414;
  border-color: #4c4d4f;

  .el-message-box__title {
    color: #ffffff;
  }

  .el-message-box__content {
    color: #ffffff;
  }

  .el-message-box__btns {
    .el-button {
      background-color: #626262;
      border-color: #4c4d4f;
      color: #ffffff;

      &:hover {
        background-color: #8a8a8a;
      }
    }
  }
}

body {
  margin: 0;
  font-family:
    Noto Sans SC,
    SF Pro SC,
    SF Pro Text,
    SF Pro Icons,
    PingFang SC,
    Helvetica Neue,
    Helvetica,
    Arial,
    sans-serif;
  background-color: #141414;
  color: #ffffff;
}

.el-date-editor--datetime {
  width: 160px !important;
}

::-webkit-scrollbar {
  --bar-width: 5px;
  width: 5px;
  height: 5px;
}

::-webkit-scrollbar-track {
  background-color: transparent;
}

::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 20px;
  background-clip: content-box;
  border: 1px solid transparent;
}

.slide-enter-active {
  transition: all 0.8s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-leave-active {
  transition: all 0.3s ease-out;
}

.slide-enter-from {
  transform: translateX(-100vw);
  opacity: 0;
}

.slide-enter-to {
  transform: translateX(0);
  opacity: 1;
}

.slide-leave-from {
  transform: translateX(0);
  opacity: 1;
}

.slide-leave-to {
  transform: translateX(100vw);
  opacity: 0;
}
</style>
