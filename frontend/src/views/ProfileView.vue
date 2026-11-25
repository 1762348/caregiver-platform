<template>
  <div class="profile">
    <header class="profile__header">
      <div class="profile__avatar">{{ initial }}</div>
      <div class="profile__info">
        <h1>{{ auth.displayName || '优护用户' }}</h1>
        <p>
          {{ maskPhone(auth.user?.phone ?? '') }}
          <span class="profile__role">{{ auth.roleLabel }}</span>
        </p>
      </div>
    </header>

    <div class="profile__stats">
      <div class="stat">
        <strong>{{ auth.user?.permissions?.length ?? 0 }}</strong>
        <span>权限项</span>
      </div>
      <div class="stat">
        <strong>{{ auth.user?.id ?? '-' }}</strong>
        <span>账号 ID</span>
      </div>
      <div class="stat">
        <strong>{{ auth.roleLabel || '-' }}</strong>
        <span>当前角色</span>
      </div>
    </div>

    <van-cell-group inset class="profile__menu">
      <van-cell
        v-for="item in menu"
        :key="item.title"
        :title="item.title"
        :label="item.label"
        is-link
        @click="openMenu(item)"
      />
    </van-cell-group>

    <van-button class="profile__book" block round type="primary" @click="router.push('/booking')">
      预约护工
    </van-button>

    <div class="profile__footer">
      <van-button block round plain type="danger" @click="logout">退出登录</van-button>
      <p>优护 · 社区护工预约平台 v0.1.0</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { maskPhone } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()

const initial = computed(() => {
  const name = auth.user?.name?.trim()
  if (name) return name.slice(0, 1)
  return auth.user?.phone?.slice(-2) ?? '优'
})

const menu = [
  { title: '我的订单', label: '查看全部服务订单', to: '/orders' },
  { title: '被照顾人', label: '添加家人的护理档案', to: '/receivers' },
  { title: '消息通知', label: '预约、支付与服务提醒', to: '/messages' },
  { title: '关于平台', label: '版本与服务协议', to: '' },
]

function openMenu(item: { title: string; to: string }) {
  if (item.to) {
    router.push(item.to)
  } else {
    showToast('优护 · 社区护工预约平台 v0.1.0')
  }
}

function logout() {
  auth.clear()
  showToast('已退出登录')
  router.replace('/')
}
</script>

<style scoped>
.profile {
  padding: 24px 16px 32px;
}

.profile__header {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 24px 20px;
  color: #fff;
  background: linear-gradient(140deg, #0891b2 0%, #0e7490 100%);
  border-radius: var(--yh-radius-lg);
  box-shadow: 0 12px 28px rgba(8, 145, 178, 0.24);
}

.profile__avatar {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  font-size: 22px;
  font-weight: 700;
  color: var(--yh-primary);
  background: #fff;
  border-radius: 18px;
}

.profile__info h1 {
  margin: 0;
  font-size: 19px;
}

.profile__info p {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 8px 0 0;
  font-size: 12px;
  opacity: 0.9;
}

.profile__role {
  padding: 2px 8px;
  font-size: 11px;
  color: #0e7490;
  background: #e0f2fe;
  border-radius: 999px;
}

.profile__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 16px 0;
}

.stat {
  padding: 14px 8px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.stat strong {
  display: block;
  font-size: 16px;
  color: var(--yh-text);
}

.stat span {
  font-size: 11px;
  color: var(--yh-text-secondary);
}

.profile__menu {
  overflow: hidden;
  border-radius: var(--yh-radius-md);
}

.profile__footer {
  margin-top: 28px;
  text-align: center;
}

.profile__book {
  margin-top: 16px;
}

.profile__footer p {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}
</style>
