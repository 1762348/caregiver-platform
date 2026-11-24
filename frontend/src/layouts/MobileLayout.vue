<template>
  <div class="mobile-layout">
    <main class="mobile-layout__main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <van-tabbar route safe-area-inset-bottom class="mobile-tabbar">
      <van-tabbar-item replace to="/" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/orders" icon="orders-o">订单</van-tabbar-item>
      <van-tabbar-item replace to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
// 移动端主框架：内容区 + 底部导航
</script>

<style scoped>
.mobile-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.mobile-layout__main {
  flex: 1;
  padding-bottom: 72px;
}

.mobile-tabbar {
  border-top: 1px solid var(--yh-border);
}

/* 固定定位的 Tabbar 限制在 480px 壳层内，桌面预览时不铺满整屏 */
.mobile-layout :deep(.van-tabbar--fixed) {
  right: auto;
  left: 50%;
  width: 100%;
  max-width: 480px;
  transform: translateX(-50%);
}
</style>
