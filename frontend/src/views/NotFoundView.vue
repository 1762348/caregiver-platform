<template>
  <div class="not-found">
    <van-empty image="error" description="页面不存在或已被移动" />
    <router-link to="/" class="not-found__link">返回首页</router-link>
  </div>
</template>

<script setup lang="ts">
// 404 兜底页
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--yh-bg);
}

.not-found__link {
  padding: 8px 20px;
  font-size: 14px;
  color: var(--yh-primary);
  text-decoration: none;
  border: 1px solid var(--yh-primary);
  border-radius: 999px;
}
</style>
