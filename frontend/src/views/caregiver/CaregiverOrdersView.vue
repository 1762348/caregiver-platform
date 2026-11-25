<template>
  <div class="cg-orders">
    <header class="cg-orders__header">
      <div>
        <h1>我的订单</h1>
        <p class="text-muted">共 {{ orders.length }} 笔</p>
      </div>
      <button type="button" class="refresh" @click="load">刷新</button>
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

    <van-empty v-else-if="!filtered.length" description="暂无订单" />

    <div v-else class="order-list">
      <article v-for="order in filtered" :key="order.id" class="cg-order">
        <div class="cg-order__head">
          <strong>{{ order.receiverName }} · {{ order.categoryName }}</strong>
          <span class="pill" :class="`pill--${statusTone(order.status)}`">
            {{ STATUS_LABELS[order.status] }}
          </span>
        </div>
        <p class="cg-order__meta">
          {{ formatDateLabel(order.serviceDate) }} {{ order.serviceTime }}
        </p>
        <p class="cg-order__stage">当前进度：{{ STAGE_LABELS[order.stage] }}</p>
        <div class="cg-order__foot">
          <span class="amount">¥{{ order.amount }}</span>
          <van-button
            v-if="actionFor(order)"
            size="small"
            type="primary"
            round
            :loading="busyId === order.id"
            @click="runAction(order, actionFor(order)!)"
          >
            {{ actionFor(order)?.label }}
          </van-button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { computed, onMounted, ref } from 'vue'

import {
  fetchCaregiverOrders,
  updateOrderProgress,
  type ProgressAction,
} from '@/api/caregiver'
import type { Order } from '@/api/orders'
import {
  STAGE_LABELS,
  STATUS_LABELS,
  caregiverAction,
  formatDateLabel,
  statusTone,
} from '@/utils/order'

type TabKey = 'ALL' | 'PAID' | 'IN_SERVICE' | 'COMPLETED'

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'ALL', label: '全部' },
  { key: 'PAID', label: '待服务' },
  { key: 'IN_SERVICE', label: '服务中' },
  { key: 'COMPLETED', label: '已完成' },
]

const orders = ref<Order[]>([])
const loading = ref(true)
const activeTab = ref<TabKey>('ALL')
const busyId = ref<number | null>(null)

const actionFor = caregiverAction

const filtered = computed(() => {
  if (activeTab.value === 'ALL') return orders.value
  return orders.value.filter((order) => order.status === activeTab.value)
})

async function load() {
  loading.value = true
  try {
    const page = await fetchCaregiverOrders({ page: 0, size: 50 })
    orders.value = page.items
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function runAction(
  order: Order,
  action: { key: ProgressAction; label: string },
) {
  if (action.key === 'complete') {
    try {
      await showConfirmDialog({
        title: '完成服务',
        message: '确认已完成本次服务？完成后订单不可再修改。',
        confirmButtonText: '确认完成',
      })
    } catch {
      return
    }
  }
  busyId.value = order.id
  try {
    await updateOrderProgress(order.id, action.key)
    showToast('进度已更新')
    await load()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '操作失败')
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.cg-orders {
  padding: 20px 16px 32px;
}

.cg-orders__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.cg-orders__header h1 {
  margin: 0;
  font-size: 22px;
}

.cg-orders__header p {
  margin: 4px 0 0;
}

.refresh {
  padding: 6px 14px;
  font: inherit;
  font-size: 12px;
  color: #0f766e;
  cursor: pointer;
  background: #ccfbf1;
  border: none;
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
  color: #0f766e;
  border-bottom-color: #0f766e;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cg-order {
  padding: 14px;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.cg-order__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cg-order__head strong {
  font-size: 14px;
}

.cg-order__meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.cg-order__stage {
  margin: 4px 0 0;
  font-size: 12px;
  color: #0f766e;
}

.cg-order__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--yh-border);
}

.amount {
  font-size: 17px;
  font-weight: 700;
}

.pill {
  padding: 3px 10px;
  font-size: 11px;
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

.state-block {
  display: grid;
  place-items: center;
  padding: 60px 0;
}
</style>
