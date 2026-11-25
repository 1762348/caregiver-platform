<template>
  <div class="orders">
    <header class="orders__header">
      <div>
        <h1>我的订单</h1>
        <p class="text-muted">共 {{ orders.length }} 笔</p>
      </div>
      <router-link class="orders__new" to="/booking">+ 新预约</router-link>
    </header>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="tabs__item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="state-block"><van-loading size="24" /></div>

    <van-empty v-else-if="!filtered.length" description="这里还没有订单">
      <van-button type="primary" round @click="router.push('/booking')">去预约</van-button>
    </van-empty>

    <div v-else class="order-list">
      <button
        v-for="order in filtered"
        :key="order.id"
        type="button"
        class="order-card"
        @click="router.push(`/orders/${order.id}`)"
      >
        <div class="order-card__head">
          <span class="order-card__category">{{ order.categoryName || '护工服务' }}</span>
          <span class="pill" :class="`pill--${statusTone(order.status)}`">
            {{ STATUS_LABELS[order.status] }}
          </span>
        </div>
        <h3>{{ order.caregiverName }}<span>·</span>{{ order.receiverName }}</h3>
        <p class="order-card__time">
          <van-icon name="calendar-o" />
          {{ formatDateLabel(order.serviceDate) }} {{ order.serviceTime }}
        </p>
        <div class="order-card__foot">
          <strong>¥{{ order.amount }}</strong>
          <span>{{ STAGE_LABELS[order.stage] }}</span>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchOrders, type Order, type OrderStatus } from '@/api/orders'
import { STAGE_LABELS, STATUS_LABELS, formatDateLabel, statusTone } from '@/utils/order'

type TabKey = 'ALL' | 'PENDING_PAYMENT' | 'ONGOING' | 'COMPLETED' | 'CANCELLED'

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待支付' },
  { key: 'ONGOING', label: '进行中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

const router = useRouter()
const orders = ref<Order[]>([])
const loading = ref(true)
const activeTab = ref<TabKey>('ALL')

const activeStatuses: OrderStatus[] = ['PAID', 'IN_SERVICE']

const filtered = computed(() => {
  if (activeTab.value === 'ALL') return orders.value
  if (activeTab.value === 'ONGOING') {
    return orders.value.filter((order) => activeStatuses.includes(order.status))
  }
  return orders.value.filter((order) => order.status === activeTab.value)
})

async function load() {
  loading.value = true
  try {
    const page = await fetchOrders({ page: 0, size: 50 })
    orders.value = page.items
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.orders {
  padding: 20px 16px 32px;
}

.orders__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.orders__header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.orders__header p {
  margin: 4px 0 0;
}

.orders__new {
  padding: 8px 14px;
  font-size: 13px;
  color: #fff;
  text-decoration: none;
  background: var(--yh-primary);
  border-radius: 999px;
}

.tabs {
  display: flex;
  gap: 16px;
  padding: 16px 0 8px;
  overflow-x: auto;
}

.tabs__item {
  flex: none;
  padding: 0 0 6px;
  font: inherit;
  font-size: 14px;
  color: var(--yh-text-secondary);
  cursor: pointer;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
}

.tabs__item.active {
  font-weight: 600;
  color: var(--yh-primary);
  border-bottom-color: var(--yh-primary);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.order-card {
  padding: 16px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.order-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__category {
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.pill {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 999px;
}

.pill--warning {
  color: #92400e;
  background: #fef3c7;
}

.pill--primary {
  color: #075985;
  background: #e0f2fe;
}

.pill--success {
  color: #166534;
  background: #dcfce7;
}

.pill--default {
  color: #334155;
  background: #e2e8f0;
}

.pill--danger {
  color: #991b1b;
  background: #fee2e2;
}

.order-card h3 {
  margin: 10px 0 6px;
  font-size: 15px;
  font-weight: 600;
}

.order-card h3 span {
  margin: 0 6px;
  color: #cbd5e1;
}

.order-card__time {
  display: flex;
  gap: 6px;
  align-items: center;
  margin: 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.order-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed var(--yh-border);
}

.order-card__foot strong {
  font-size: 18px;
}

.order-card__foot span {
  font-size: 12px;
  color: var(--yh-primary);
}

.state-block {
  display: grid;
  place-items: center;
  padding: 60px 0;
}
</style>
