<template>
  <div class="caregiver-layout">
    <main class="caregiver-layout__main">
      <router-view />
    </main>
    <van-tabbar route safe-area-inset-bottom class="caregiver-tabbar">
      <van-tabbar-item replace to="/caregiver" icon="todo-list-o">工作台</van-tabbar-item>
      <van-tabbar-item replace to="/caregiver/orders" icon="orders-o">我的订单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
// 护工端移动布局
</script>

<style scoped>
.caregiver-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.caregiver-layout__main {
  flex: 1;
  padding-bottom: 72px;
}

.caregiver-tabbar {
  border-top: 1px solid var(--yh-border);
}

.caregiver-layout :deep(.van-tabbar--fixed) {
  right: auto;
  left: 50%;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
}
</style>
