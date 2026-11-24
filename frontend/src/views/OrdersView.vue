<template>
  <div class="orders">
    <h1 class="orders__title">我的订单</h1>
    <div class="orders__tabs">
      <span
        v-for="tab in tabs"
        :key="tab"
        class="orders__tab"
        :class="{ 'orders__tab--active': tab === activeTab }"
        @click="activeTab = tab"
      >
        {{ tab }}
      </span>
    </div>

    <van-empty description="还没有订单">
      <p class="text-muted">预约功能开放后，订单会按状态展示在这里</p>
    </van-empty>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tabs = ['全部', '待支付', '服务中', '已完成', '已取消'] as const
const activeTab = ref<(typeof tabs)[number]>('全部')
</script>

<style scoped>
.orders {
  padding: 24px 16px 32px;
}

.orders__title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 700;
}

.orders__tabs {
  display: flex;
  gap: 18px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.orders__tab {
  flex: none;
  padding-bottom: 6px;
  font-size: 14px;
  color: var(--yh-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.orders__tab--active {
  font-weight: 600;
  color: var(--yh-primary);
  border-bottom-color: var(--yh-primary);
}
</style>
