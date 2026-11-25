<template>
  <div class="login">
    <div class="login__brand">
      <div class="login__logo">优</div>
      <h1>优护 · 社区护工预约平台</h1>
      <p>手机号登录 / 注册，首次登录自动创建账号</p>
    </div>

    <div class="login__panel">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="phone"
            name="phone"
            type="tel"
            label="手机号"
            placeholder="请输入 11 位手机号"
            maxlength="11"
            :rules="phoneRules"
          />
          <van-field
            v-model="code"
            name="code"
            type="digit"
            label="验证码"
            placeholder="请输入 6 位验证码"
            maxlength="6"
            :rules="codeRules"
          >
            <template #button>
              <van-button
                size="small"
                type="primary"
                plain
                :disabled="counting"
                :loading="sending"
                @click.prevent="sendCode"
              >
                {{ codeButtonText }}
              </van-button>
            </template>
          </van-field>
        </van-cell-group>

        <div class="login__actions">
          <van-button block round type="primary" native-type="submit" :loading="submitting">
            登录 / 注册
          </van-button>
          <router-link class="login__back" to="/">先随便看看</router-link>
        </div>
      </van-form>
    </div>

    <div class="login__demo">
      <p class="login__demo-title">演示账号（开发环境验证码固定 123456）</p>
      <div class="login__demo-chips">
        <button type="button" class="demo-chip" @click="fillDemo('13800000002')">
          用户 · 13800000002
        </button>
        <button type="button" class="demo-chip" @click="fillDemo('13800000000')">
          管理员 · 13800000000
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const phone = ref('')
const code = ref('')
const seconds = ref(0)
const sending = ref(false)
const submitting = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

const counting = computed(() => seconds.value > 0)
const codeButtonText = computed(() => (counting.value ? `${seconds.value}s 后重发` : '获取验证码'))

const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' },
]

const codeRules = [
  { required: true, message: '请输入验证码' },
  { pattern: /^\d{6}$/, message: '验证码为 6 位数字' },
]

function fillDemo(value: string) {
  phone.value = value
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    showToast('请先输入正确的手机号')
    return
  }
  sending.value = true
  try {
    await auth.sendCode(phone.value)
    showToast('验证码已发送')
    seconds.value = 60
    timer = setInterval(() => {
      seconds.value -= 1
      if (seconds.value <= 0 && timer) {
        clearInterval(timer)
        timer = undefined
      }
    }, 1000)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '验证码发送失败')
  } finally {
    sending.value = false
  }
}

async function onSubmit() {
  submitting.value = true
  try {
    await auth.login(phone.value, code.value)
    showToast('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : auth.homePath
    await router.replace(redirect)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '登录失败')
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  gap: 20px;
  justify-content: center;
  min-height: 100vh;
  padding: 32px 20px 40px;
  background:
    radial-gradient(120% 60% at 50% 0%, #cffafe 0%, rgba(207, 250, 254, 0) 70%), var(--yh-bg);
}

.login__brand {
  text-align: center;
}

.login__logo {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #0891b2, #0e7490);
  border-radius: 20px;
  box-shadow: 0 12px 24px rgba(8, 145, 178, 0.28);
}

.login__brand h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.login__brand p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--yh-text-secondary);
}

.login__panel {
  padding: 20px 8px;
  background: #fff;
  border-radius: var(--yh-radius-lg);
  box-shadow: var(--yh-shadow);
}

.login__actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
  padding: 24px 16px 8px;
}

.login__back {
  font-size: 13px;
  color: var(--yh-text-secondary);
  text-decoration: none;
}

.login__demo {
  padding: 14px 16px;
  text-align: center;
  background: rgba(255, 255, 255, 0.7);
  border: 1px dashed var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.login__demo-title {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.login__demo-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.demo-chip {
  padding: 6px 12px;
  font: inherit;
  font-size: 12px;
  color: var(--yh-primary);
  cursor: pointer;
  background: var(--yh-primary-light);
  border: none;
  border-radius: 999px;
}
</style>
