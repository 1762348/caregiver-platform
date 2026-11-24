<template>
  <div class="profile">
    <header class="profile__header">
      <div class="profile__avatar">优</div>
      <div>
        <h1>{{ auth.isLoggedIn ? '已登录用户' : '未登录' }}</h1>
        <p>{{ auth.isLoggedIn ? '普通用户' : '登录后可管理资料与订单' }}</p>
      </div>
    </header>

    <van-cell-group inset class="profile__menu">
      <van-cell
        v-for="item in menu"
        :key="item.title"
        :title="item.title"
        :label="item.label"
        is-link
        @click="notReady(item.title)"
      />
    </van-cell-group>

    <div class="profile__footer">
      <p>优护 · 社区护工预约平台 v0.1.0</p>
      <router-link v-if="!auth.isLoggedIn" to="/login" class="profile__login">去登录</router-link>
      <button v-else type="button" class="profile__logout" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const menu = [
  { title: '个人信息', label: '姓名、手机号、头像' },
  { title: '被照顾人', label: '添加家人的护理档案' },
  { title: '消息通知', label: '预约、支付与服务提醒' },
  { title: '关于平台', label: '版本与服务协议' },
]

function notReady(feature: string) {
  showToast(`${feature}将在后续阶段接入`)
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
  font-size: 24px;
  font-weight: 700;
  color: var(--yh-primary);
  background: #fff;
  border-radius: 18px;
}

.profile__header h1 {
  margin: 0;
  font-size: 19px;
}

.profile__header p {
  margin: 6px 0 0;
  font-size: 12px;
  opacity: 0.85;
}

.profile__menu {
  margin-top: 20px;
  overflow: hidden;
  border-radius: var(--yh-radius-md);
}

.profile__footer {
  margin-top: 32px;
  font-size: 12px;
  color: var(--yh-text-secondary);
  text-align: center;
}

.profile__login,
.profile__logout {
  display: inline-block;
  margin-top: 12px;
  font: inherit;
  font-size: 13px;
  color: var(--yh-primary);
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
}
</style>
