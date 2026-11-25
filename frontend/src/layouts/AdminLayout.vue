<template>
  <el-config-provider :locale="zhCn">
    <div class="admin">
      <aside class="admin__side">
        <div class="admin__brand">
          <span class="admin__logo">优</span>
          <div>
            <strong>优护管理后台</strong>
            <small>社区护工预约平台</small>
          </div>
        </div>
        <nav class="admin__nav">
          <router-link to="/admin/categories">服务类别</router-link>
          <router-link to="/admin/caregivers">护工管理</router-link>
          <router-link to="/admin/schedules">排班与名额</router-link>
        </nav>
        <button type="button" class="admin__logout" @click="logout">退出登录</button>
      </aside>

      <div class="admin__main">
        <header class="admin__header">
          <span class="admin__user">{{ auth.displayName }}</span>
          <router-link class="admin__link" to="/">返回用户端</router-link>
        </header>
        <main class="admin__content">
          <router-view />
        </main>
      </div>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

import 'element-plus/dist/index.css'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.clear()
  router.replace('/login')
}
</script>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: #f1f5f9;
}

.admin__side {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 220px;
  padding: 20px 16px;
  color: #e2e8f0;
  background: #0f172a;
}

.admin__brand {
  display: flex;
  gap: 10px;
  align-items: center;
}

.admin__logo {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  font-weight: 700;
  color: #0f172a;
  background: #67e8f9;
  border-radius: 12px;
}

.admin__brand strong {
  display: block;
  font-size: 14px;
  color: #fff;
}

.admin__brand small {
  font-size: 11px;
  color: #94a3b8;
}

.admin__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin__nav a {
  padding: 10px 12px;
  font-size: 13px;
  color: #cbd5e1;
  text-decoration: none;
  border-radius: 10px;
}

.admin__nav a.router-link-active {
  font-weight: 600;
  color: #0f172a;
  background: #67e8f9;
}

.admin__logout {
  padding: 10px;
  margin-top: auto;
  font: inherit;
  font-size: 12px;
  color: #fca5a5;
  cursor: pointer;
  background: rgba(248, 113, 113, 0.12);
  border: none;
  border-radius: 10px;
}

.admin__main {
  flex: 1;
  min-width: 0;
}

.admin__header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.admin__user {
  font-size: 13px;
  color: #334155;
}

.admin__link {
  font-size: 12px;
  color: #0891b2;
  text-decoration: none;
}

.admin__content {
  padding: 24px;
}
</style>
