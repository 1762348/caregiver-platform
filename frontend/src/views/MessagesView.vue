<template>
  <div class="messages">
    <header class="messages__header">
      <button type="button" class="icon-button" @click="router.back()">
        <van-icon name="arrow-left" size="18" />
      </button>
      <h1>消息通知</h1>
      <button type="button" class="text-button" :disabled="!unread" @click="readAll">
        全部已读
      </button>
    </header>

    <div v-if="loading" class="state-block"><van-loading size="24" /></div>

    <van-empty v-else-if="!messages.length" description="暂时没有消息" />

    <div v-else class="message-list">
      <button
        v-for="message in messages"
        :key="message.id"
        type="button"
        class="message-card"
        :class="{ unread: !message.read }"
        @click="openMessage(message)"
      >
        <span class="message-card__dot" />
        <div class="message-card__body">
          <div class="message-card__head">
            <strong>{{ message.title }}</strong>
            <small>{{ formatDateTime(message.createdAt) }}</small>
          </div>
          <p>{{ message.body }}</p>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchMessages, markAllMessagesRead, markMessageRead, type Message } from '@/api/messages'
import { formatDateTime } from '@/utils/order'

const router = useRouter()
const messages = ref<Message[]>([])
const loading = ref(true)

const unread = computed(() => messages.value.filter((message) => !message.read).length)

async function load() {
  loading.value = true
  try {
    const page = await fetchMessages({ page: 0, size: 50 })
    messages.value = page.items
  } catch (error) {
    showToast(error instanceof Error ? error.message : '消息加载失败')
  } finally {
    loading.value = false
  }
}

async function openMessage(message: Message) {
  if (!message.read) {
    try {
      await markMessageRead(message.id)
      message.read = true
    } catch {
      // 标记失败不影响跳转
    }
  }
  if (message.deeplink) {
    await router.push(message.deeplink)
  }
}

async function readAll() {
  try {
    await markAllMessagesRead()
    messages.value.forEach((message) => {
      message.read = true
    })
    showToast('已全部标记为已读')
  } catch (error) {
    showToast(error instanceof Error ? error.message : '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.messages {
  min-height: 100vh;
  padding: 16px 16px 32px;
}

.messages__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.messages__header h1 {
  flex: 1;
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--yh-text);
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: 12px;
}

.text-button {
  padding: 0;
  font: inherit;
  font-size: 12px;
  color: var(--yh-primary);
  cursor: pointer;
  background: none;
  border: none;
}

.text-button:disabled {
  color: #cbd5e1;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-card {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 14px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.message-card__dot {
  flex: none;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  background: transparent;
  border-radius: 50%;
}

.message-card.unread .message-card__dot {
  background: var(--yh-primary);
}

.message-card__body {
  flex: 1;
}

.message-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.message-card__head strong {
  font-size: 14px;
  font-weight: 600;
}

.message-card__head small {
  flex: none;
  font-size: 10px;
  color: #94a3b8;
}

.message-card__body p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--yh-text-secondary);
}

.state-block {
  display: grid;
  place-items: center;
  padding: 60px 0;
}
</style>
