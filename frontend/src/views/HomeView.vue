<template>
  <div class="home">
    <header class="home__header">
      <div>
        <p class="home__eyebrow">优护 · 社区护工</p>
        <h1 class="home__title">{{ greeting }}{{ auth.user?.name ? '，' + auth.user.name : '，您好' }}</h1>
        <p class="home__subtitle">
          {{
            auth.isLoggedIn
              ? `${auth.roleLabel} · ${maskPhone(auth.user?.phone ?? '')}`
              : '让专业照护离家人更近一步'
          }}
        </p>
      </div>
      <div class="home__actions">
        <router-link class="home__bell" to="/messages" aria-label="消息通知">
          <van-icon name="bell" size="20" />
          <span v-if="unread > 0" class="home__badge">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
        <router-link class="home__avatar" :to="auth.isLoggedIn ? '/profile' : '/login'">
          <span v-if="auth.isLoggedIn">{{ avatarInitial }}</span>
          <van-icon v-else name="user-o" size="22" />
        </router-link>
      </div>
    </header>

    <section class="hero" @click="router.push('/booking')">
      <div class="hero__text">
        <h2>预约护工</h2>
        <p>查排班 · 选护工 · 在线支付</p>
      </div>
      <van-button round size="small" class="hero__button">立即预约</van-button>
    </section>

    <div class="section-heading">
      <h2>护理服务</h2>
      <span>{{ categories.length ? '按类别预约' : '加载中…' }}</span>
    </div>
    <div v-if="categories.length" class="service-grid">
      <button
        v-for="category in categories"
        :key="category.id"
        type="button"
        class="service-item"
        @click="router.push({ path: '/booking', query: { categoryId: category.id } })"
      >
        <span class="service-item__icon">
          <van-icon :name="categoryIcons[category.code] ?? 'service-o'" size="20" />
        </span>
        <span class="service-item__name">{{ category.name }}</span>
        <span class="service-item__desc">¥{{ category.basePrice }} 起</span>
      </button>
    </div>
    <div v-else class="card placeholder-card">
      <span class="text-muted">{{ categoryError || '正在加载服务类别…' }}</span>
      <button v-if="categoryError" type="button" class="link-button" @click="loadCategories">
        重试
      </button>
    </div>

    <div class="section-heading">
      <h2>服务进度</h2>
      <router-link to="/orders" class="link-button">全部订单</router-link>
    </div>

    <section
      v-if="activeOrder"
      class="active-card"
      @click="router.push(`/orders/${activeOrder.id}`)"
    >
      <div class="active-card__top">
        <span class="pill" :class="`pill--${statusTone(activeOrder.status)}`">
          {{ STATUS_LABELS[activeOrder.status] }}
        </span>
        <span class="active-card__stage">{{ STAGE_LABELS[activeOrder.stage] }}</span>
      </div>
      <h3>{{ activeOrder.caregiverName }} · {{ activeOrder.categoryName }}</h3>
      <p>
        {{ formatDateLabel(activeOrder.serviceDate) }} {{ activeOrder.serviceTime }} ·
        {{ activeOrder.receiverName }}
      </p>
      <div class="active-card__foot">
        <strong>¥{{ activeOrder.amount }}</strong>
        <span>{{ activeOrder.canPay ? '去支付' : '查看详情' }} →</span>
      </div>
    </section>

    <div v-else class="card empty-card">
      <van-icon name="clock-o" size="28" />
      <div>
        <strong>暂无进行中的服务</strong>
        <p class="text-muted">预约成功后，可在这里查看护工出发、到达与服务进度</p>
      </div>
    </div>

    <div class="quick-links">
      <router-link to="/receivers" class="quick-link">
        <van-icon name="friends-o" size="20" />
        <span>被照顾人</span>
      </router-link>
      <router-link to="/messages" class="quick-link">
        <van-icon name="chat-o" size="20" />
        <span>消息通知</span>
      </router-link>
      <router-link to="/orders" class="quick-link">
        <van-icon name="orders-o" size="20" />
        <span>我的订单</span>
      </router-link>
    </div>

    <div class="system-status" @click="loadMeta">
      <span class="status-dot" :class="`status-dot--${metaState}`" />
      <span v-if="metaState === 'ok'">后端已连接 · v{{ meta?.version }} · {{ meta?.profile }}</span>
      <span v-else-if="metaState === 'loading'">正在连接后端…</span>
      <span v-else>后端未连接，点击重试</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchCategories, type Category } from '@/api/catalog'
import { fetchServerMeta, type ServerMeta } from '@/api/meta'
import { fetchUnreadCount } from '@/api/messages'
import { fetchOrders, type Order } from '@/api/orders'
import { useAuthStore } from '@/stores/auth'
import { maskPhone } from '@/utils/format'
import { STAGE_LABELS, STATUS_LABELS, formatDateLabel, statusTone } from '@/utils/order'

const categoryIcons: Record<string, string> = {
  DAILY_CARE: 'friends-o',
  PROFESSIONAL_CARE: 'medal-o',
  REHAB_TRAINING: 'chart-trending-o',
  ESCORT_SERVICE: 'location-o',
  NIGHT_CARE: 'closed-eye',
}

const auth = useAuthStore()
const router = useRouter()

const categories = ref<Category[]>([])
const categoryError = ref('')
const activeOrder = ref<Order | null>(null)
const unread = ref(0)
const meta = ref<ServerMeta | null>(null)
const metaState = ref<'loading' | 'ok' | 'error'>('loading')

const avatarInitial = computed(() => {
  const name = auth.user?.name?.trim()
  if (name) return name.slice(0, 1)
  return auth.user?.phone?.slice(-2) ?? '优'
})

const greeting = (() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})()

async function loadCategories() {
  categoryError.value = ''
  try {
    categories.value = await fetchCategories()
  } catch (error) {
    categories.value = []
    categoryError.value = error instanceof Error ? error.message : '服务类别加载失败'
  }
}

async function loadActiveOrder() {
  if (!auth.isLoggedIn) return
  try {
    const page = await fetchOrders({ page: 0, size: 10 })
    activeOrder.value =
      page.items.find((order) =>
        ['PENDING_PAYMENT', 'PAID', 'IN_SERVICE'].includes(order.status),
      ) ?? null
  } catch {
    activeOrder.value = null
  }
}

async function loadUnread() {
  if (!auth.isLoggedIn) return
  try {
    unread.value = await fetchUnreadCount()
  } catch {
    unread.value = 0
  }
}

async function loadMeta() {
  metaState.value = 'loading'
  try {
    meta.value = await fetchServerMeta()
    metaState.value = 'ok'
  } catch {
    meta.value = null
    metaState.value = 'error'
  }
}

onMounted(() => {
  void loadCategories()
  void loadActiveOrder()
  void loadUnread()
  void loadMeta()
})
</script>

<style scoped>
.home {
  padding: 20px 16px 32px;
}

.home__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.home__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.08em;
  color: var(--yh-primary);
}

.home__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.home__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--yh-text-secondary);
}

.home__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.home__bell,
.home__avatar {
  position: relative;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  font-size: 16px;
  font-weight: 700;
  color: var(--yh-primary);
  text-decoration: none;
  background: var(--yh-primary-light);
  border-radius: 50%;
}

.home__badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  padding: 0 5px;
  font-size: 10px;
  line-height: 18px;
  color: #fff;
  text-align: center;
  background: #ef4444;
  border-radius: 9px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 20px;
  margin-top: 20px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(140deg, #0891b2 0%, #0e7490 100%);
  border-radius: var(--yh-radius-lg);
  box-shadow: 0 12px 28px rgba(8, 145, 178, 0.28);
}

.hero__text h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.hero__text p {
  margin: 6px 0 0;
  font-size: 13px;
  opacity: 0.85;
}

.hero__button {
  color: var(--yh-primary);
  font-weight: 600;
  background: #fff;
  border: none;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.service-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  padding: 14px 12px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.service-item__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--yh-primary);
  background: var(--yh-primary-light);
  border-radius: 12px;
}

.service-item__name {
  font-size: 14px;
  font-weight: 600;
}

.service-item__desc {
  font-size: 11px;
  color: var(--yh-text-secondary);
}

.active-card {
  padding: 16px;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-left: 5px solid var(--yh-primary);
  border-radius: var(--yh-radius-md);
  box-shadow: var(--yh-shadow);
}

.active-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.active-card__stage {
  font-size: 12px;
  font-weight: 600;
  color: var(--yh-primary);
}

.active-card h3 {
  margin: 10px 0 6px;
  font-size: 15px;
}

.active-card p {
  margin: 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.active-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed var(--yh-border);
}

.active-card__foot strong {
  font-size: 18px;
}

.active-card__foot span {
  font-size: 12px;
  color: var(--yh-primary);
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

.empty-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  color: var(--yh-primary);
}

.empty-card strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  color: var(--yh-text);
}

.empty-card p {
  margin: 0;
  line-height: 1.6;
}

.placeholder-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.quick-link {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  padding: 14px 8px;
  font-size: 12px;
  color: var(--yh-text);
  text-decoration: none;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.quick-link :deep(.van-icon) {
  color: var(--yh-primary);
}

.system-status {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  margin-top: 22px;
  font-size: 11px;
  color: #94a3b8;
  cursor: pointer;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #cbd5e1;
  border-radius: 50%;
}

.status-dot--ok {
  background: #10b981;
}

.status-dot--error {
  background: #ef4444;
}

.link-button {
  padding: 0;
  font: inherit;
  font-size: 12px;
  color: var(--yh-primary);
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
}
</style>
