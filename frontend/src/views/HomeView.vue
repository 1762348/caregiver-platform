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
      <router-link class="home__avatar" :to="auth.isLoggedIn ? '/profile' : '/login'">
        <span v-if="auth.isLoggedIn">{{ avatarInitial }}</span>
        <van-icon v-else name="user-o" size="22" />
      </router-link>
    </header>

    <section class="hero" @click="notReady('在线预约')">
      <div class="hero__text">
        <h2>预约护工</h2>
        <p>查排班 · 选护工 · 在线支付</p>
      </div>
      <van-button round size="small" class="hero__button">立即预约</van-button>
    </section>

    <div class="section-heading">
      <h2>护理服务</h2>
      <span>全部服务</span>
    </div>
    <div class="service-grid">
      <button
        v-for="item in services"
        :key="item.name"
        type="button"
        class="service-item"
        @click="notReady(item.name)"
      >
        <span class="service-item__icon">
          <van-icon :name="item.icon" size="20" />
        </span>
        <span class="service-item__name">{{ item.name }}</span>
        <span class="service-item__desc">{{ item.desc }}</span>
      </button>
    </div>

    <div class="section-heading">
      <h2>服务进度</h2>
      <span>实时跟踪</span>
    </div>
    <div class="card empty-card">
      <van-icon name="clock-o" size="30" />
      <div>
        <strong>暂无进行中的服务</strong>
        <p class="text-muted">预约成功后，可在这里查看护工出发、到达与服务剩余时间</p>
      </div>
    </div>

    <div class="section-heading">
      <h2>系统状态</h2>
      <button type="button" class="link-button" @click="loadMeta">刷新</button>
    </div>
    <div class="card status-card">
      <span class="status-dot" :class="`status-dot--${metaState}`" />
      <div class="status-card__body">
        <strong v-if="metaState === 'loading'">正在连接后端…</strong>
        <strong v-else-if="metaState === 'ok'">后端已连接</strong>
        <strong v-else>后端未连接</strong>
        <p v-if="meta" class="text-muted">
          {{ meta.displayName }} · v{{ meta.version }} · {{ meta.profile }}
        </p>
        <p v-else-if="metaState === 'error'" class="text-muted">
          请确认后端已在 8080 端口启动
        </p>
      </div>
    </div>

    <p class="home__footnote">P2 已完成登录鉴权 · 预约、支付与服务进度将在后续阶段接入</p>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed, onMounted, ref } from 'vue'

import { fetchServerMeta, type ServerMeta } from '@/api/meta'
import { useAuthStore } from '@/stores/auth'
import { maskPhone } from '@/utils/format'

const auth = useAuthStore()

const avatarInitial = computed(() => {
  const name = auth.user?.name?.trim()
  if (name) return name.slice(0, 1)
  return auth.user?.phone?.slice(-2) ?? '优'
})

const services = [
  { name: '日常照料', desc: '饮食起居', icon: 'friends-o' },
  { name: '专业护理', desc: '术后康复', icon: 'medal-o' },
  { name: '康复训练', desc: '功能恢复', icon: 'chart-trending-o' },
  { name: '陪诊服务', desc: '陪同就医', icon: 'location-o' },
  { name: '夜间陪护', desc: '全天守护', icon: 'closed-eye' },
] as const

const meta = ref<ServerMeta | null>(null)
const metaState = ref<'loading' | 'ok' | 'error'>('loading')

const greeting = (() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})()

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

function notReady(feature: string) {
  showToast(`${feature}将在后续阶段接入`)
}

onMounted(loadMeta)
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

.home__avatar {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  font-size: 16px;
  font-weight: 700;
  color: var(--yh-primary);
  text-decoration: none;
  background: var(--yh-primary-light);
  border-radius: 50%;
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
  background: var(--yh-surface);
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.service-item:active {
  transform: scale(0.98);
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
  color: var(--yh-text);
}

.service-item__desc {
  font-size: 11px;
  color: var(--yh-text-secondary);
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

.status-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.status-dot {
  flex: none;
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #cbd5e1;
}

.status-dot--ok {
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.15);
}

.status-dot--error {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.15);
}

.status-dot--loading {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.15);
}

.status-card__body strong {
  font-size: 14px;
}

.status-card__body p {
  margin: 4px 0 0;
  line-height: 1.5;
}

.link-button {
  padding: 0;
  font: inherit;
  font-size: 12px;
  color: var(--yh-primary);
  cursor: pointer;
  background: none;
  border: none;
}

.home__footnote {
  margin: 28px 0 0;
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
}
</style>
