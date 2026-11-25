<template>
  <div class="cg-home">
    <header class="cg-hero">
      <div class="cg-hero__head">
        <div>
          <p class="cg-hero__eyebrow">护工工作台</p>
          <h1>{{ profile?.name ?? '护工' }}</h1>
          <p class="cg-hero__meta">
            {{ profile?.code }} · {{ profile?.categoryName || '未分类' }}
            <span v-if="profile?.rating">· ★ {{ profile.rating }}</span>
          </p>
        </div>
        <button type="button" class="cg-hero__logout" @click="logout">退出</button>
      </div>
      <div class="cg-stats">
        <div><strong>{{ todaySchedules.length }}</strong><span>今日排班</span></div>
        <div><strong>{{ pendingOrders.length }}</strong><span>待服务</span></div>
        <div><strong>{{ servicingOrders.length }}</strong><span>服务中</span></div>
      </div>
    </header>

    <div v-if="loading" class="state-block"><van-loading size="24" /></div>

    <template v-else>
      <section class="cg-section">
        <div class="cg-section__head">
          <h2>今日安排</h2>
          <span>{{ todayLabel }}</span>
        </div>
        <div v-if="todaySchedules.length" class="schedule-list">
          <div v-for="item in todaySchedules" :key="item.id" class="schedule-card">
            <div class="schedule-card__time">{{ item.timeRange }}</div>
            <div class="schedule-card__meta">
              <strong>{{ item.status === 'OPEN' ? '可预约' : '已停班' }}</strong>
              <span>剩余 {{ item.remainingQuota }}/{{ item.totalQuota }} · ¥{{ item.price }}</span>
            </div>
          </div>
        </div>
        <div v-else class="cg-empty">今天没有排班</div>
      </section>

      <section class="cg-section">
        <div class="cg-section__head">
          <h2>待处理订单</h2>
          <span>按服务进度操作</span>
        </div>

        <div v-if="activeOrders.length" class="order-list">
          <article v-for="order in activeOrders" :key="order.id" class="cg-order">
            <div class="cg-order__head">
              <strong>{{ order.receiverName }} · {{ order.categoryName }}</strong>
              <span class="pill" :class="`pill--${statusTone(order.status)}`">
                {{ STATUS_LABELS[order.status] }}
              </span>
            </div>
            <p class="cg-order__meta">
              {{ formatDateLabel(order.serviceDate) }} {{ order.serviceTime }}
            </p>
            <p class="cg-order__stage">
              {{ STAGE_LABELS[order.stage] }}
              <span v-if="order.caregiverPhone">· 用户电话见订单</span>
            </p>
            <div class="cg-order__actions">
              <span class="cg-order__amount">¥{{ order.amount }}</span>
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
        <div v-else class="cg-empty">暂无待处理订单</div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  fetchCaregiverOrders,
  fetchCaregiverProfile,
  fetchCaregiverSchedules,
  updateOrderProgress,
  type CaregiverProfile,
  type CaregiverSchedule,
  type ProgressAction,
} from '@/api/caregiver'
import type { Order } from '@/api/orders'
import { useAuthStore } from '@/stores/auth'
import {
  STAGE_LABELS,
  STATUS_LABELS,
  caregiverAction,
  formatDateLabel,
  statusTone,
} from '@/utils/order'

const auth = useAuthStore()
const router = useRouter()

const profile = ref<CaregiverProfile | null>(null)
const schedules = ref<CaregiverSchedule[]>([])
const orders = ref<Order[]>([])
const loading = ref(true)
const busyId = ref<number | null>(null)

const today = new Date()
const todayValue = [
  today.getFullYear(),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0'),
].join('-')
const todayLabel = formatDateLabel(todayValue)

const todaySchedules = computed(() =>
  schedules.value.filter((item) => item.serviceDate === todayValue),
)
const activeOrders = computed(() =>
  orders.value.filter((order) => ['PAID', 'IN_SERVICE'].includes(order.status)),
)
const pendingOrders = computed(() =>
  activeOrders.value.filter((order) => order.status === 'PAID'),
)
const servicingOrders = computed(() =>
  activeOrders.value.filter((order) => order.status === 'IN_SERVICE'),
)

type Action = { key: ProgressAction; label: string }

const actionFor = caregiverAction

async function load() {
  loading.value = true
  try {
    const [profileData, scheduleData, orderPage] = await Promise.all([
      fetchCaregiverProfile(),
      fetchCaregiverSchedules(),
      fetchCaregiverOrders({ page: 0, size: 50 }),
    ])
    profile.value = profileData
    schedules.value = scheduleData
    orders.value = orderPage.items
  } catch (error) {
    showToast(error instanceof Error ? error.message : '数据加载失败')
  } finally {
    loading.value = false
  }
}

async function runAction(order: Order, action: Action) {
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

function logout() {
  auth.clear()
  router.replace('/login')
}

onMounted(load)
</script>

<style scoped>
.cg-home {
  padding: 16px 16px 32px;
}

.cg-hero {
  padding: 20px;
  color: #fff;
  background: linear-gradient(140deg, #0f766e, #115e59);
  border-radius: var(--yh-radius-lg);
  box-shadow: 0 12px 28px rgba(15, 118, 110, 0.25);
}

.cg-hero__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.cg-hero__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.08em;
  opacity: 0.8;
}

.cg-hero h1 {
  margin: 0;
  font-size: 24px;
}

.cg-hero__meta {
  margin: 6px 0 0;
  font-size: 12px;
  opacity: 0.85;
}

.cg-hero__logout {
  padding: 6px 12px;
  font: inherit;
  font-size: 12px;
  color: #fff;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.18);
  border: none;
  border-radius: 999px;
}

.cg-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.cg-stats div {
  padding: 10px;
  text-align: center;
  background: rgba(255, 255, 255, 0.14);
  border-radius: 12px;
}

.cg-stats strong {
  display: block;
  font-size: 20px;
}

.cg-stats span {
  font-size: 11px;
  opacity: 0.85;
}

.cg-section {
  margin-top: 22px;
}

.cg-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.cg-section__head h2 {
  margin: 0;
  font-size: 16px;
}

.cg-section__head span {
  font-size: 11px;
  color: var(--yh-text-secondary);
}

.schedule-list,
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.schedule-card__time {
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #0f766e;
  background: #ccfbf1;
  border-radius: 10px;
}

.schedule-card__meta strong {
  display: block;
  font-size: 13px;
}

.schedule-card__meta span {
  font-size: 11px;
  color: var(--yh-text-secondary);
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

.cg-order__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--yh-border);
}

.cg-order__amount {
  font-size: 17px;
  font-weight: 700;
}

.cg-empty {
  padding: 24px;
  font-size: 12px;
  color: var(--yh-text-secondary);
  text-align: center;
  background: #fff;
  border: 1px dashed var(--yh-border);
  border-radius: var(--yh-radius-md);
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
