<template>
  <div class="order-detail">
    <header class="order-detail__header">
      <button type="button" class="icon-button" @click="router.back()">
        <van-icon name="arrow-left" size="18" />
      </button>
      <h1>订单详情</h1>
      <button type="button" class="icon-button" @click="load">
        <van-icon name="replay" size="18" />
      </button>
    </header>

    <div v-if="loading" class="state-block">
      <van-loading size="24" />
    </div>

    <template v-else-if="order">
      <section class="status-hero" :class="`status-hero--${order.status.toLowerCase()}`">
        <p class="status-hero__eyebrow">订单 {{ order.orderNo }}</p>
        <h2>{{ STAGE_LABELS[order.stage] }}</h2>
        <p class="status-hero__hint">{{ stageHint }}</p>
        <span class="status-hero__pill">{{ STATUS_LABELS[order.status] }}</span>
      </section>

      <section v-if="showProgress" class="card">
        <h3 class="card__title">服务进度</h3>
        <div class="timeline">
          <div
            v-for="(item, index) in steps"
            :key="item.key"
            class="timeline__item"
            :class="{ done: item.done, active: item.active }"
          >
            <span class="timeline__dot">
              <van-icon v-if="item.done" name="success" size="12" />
              <template v-else>{{ index + 1 }}</template>
            </span>
            <div class="timeline__body">
              <strong>{{ item.label }}</strong>
              <small>{{ item.time ? formatDateTime(item.time) : '待进行' }}</small>
            </div>
          </div>
        </div>
        <p class="timeline__tip">
          <van-icon name="info-o" /> 进度每 15 秒自动刷新，也可点击右上角手动刷新
        </p>
      </section>

      <section class="card">
        <h3 class="card__title">服务信息</h3>
        <dl class="info-list">
          <div>
            <dt>服务类别</dt>
            <dd>{{ order.categoryName || '-' }}</dd>
          </div>
          <div>
            <dt>护工</dt>
            <dd>{{ order.caregiverName || '-' }}</dd>
          </div>
          <div>
            <dt>服务时间</dt>
            <dd>{{ formatDateLabel(order.serviceDate) }} {{ order.serviceTime }}</dd>
          </div>
          <div>
            <dt>被照顾人</dt>
            <dd>{{ order.receiverName || '-' }}</dd>
          </div>
          <div>
            <dt>订单金额</dt>
            <dd class="amount">¥{{ order.amount }}</dd>
          </div>
          <div>
            <dt>下单时间</dt>
            <dd>{{ formatDateTime(order.createdAt) }}</dd>
          </div>
          <div v-if="order.payTime">
            <dt>支付时间</dt>
            <dd>{{ formatDateTime(order.payTime) }}</dd>
          </div>
          <div v-if="order.cancelTime">
            <dt>取消时间</dt>
            <dd>{{ formatDateTime(order.cancelTime) }}</dd>
          </div>
          <div v-if="order.cancelReason">
            <dt>取消原因</dt>
            <dd>{{ order.cancelReason }}</dd>
          </div>
        </dl>
      </section>

      <section
        v-if="order.caregiverPhone && order.status !== 'CANCELLED'"
        class="card contact-card"
      >
        <div>
          <strong>联系护工</strong>
          <p class="text-muted">{{ order.caregiverName }} · {{ order.caregiverPhone }}</p>
        </div>
        <a class="call-button" :href="`tel:${order.caregiverPhone}`">
          <van-icon name="phone-o" /> 拨号
        </a>
      </section>

      <section v-if="order.canPay || order.canCancel" class="actions">
        <van-button v-if="order.canPay" type="primary" block round @click="paySheet = true">
          去支付 ¥{{ order.amount }}
        </van-button>
        <van-button v-if="order.canCancel" block round plain type="danger" @click="confirmCancel">
          取消订单
        </van-button>
      </section>

      <section v-else-if="order.status === 'COMPLETED'" class="actions">
        <van-button block round type="primary" @click="router.push('/booking')"
          >再次预约</van-button
        >
      </section>

      <p class="rule-note">
        取消规则：服务开始前 2 小时以上可全额退款；2 小时内取消收取 20% 手续费；服务开始后不可取消。
      </p>
    </template>

    <van-action-sheet
      v-model:show="paySheet"
      :actions="payActions"
      cancel-text="取消"
      title="选择支付方式"
      close-on-click-action
      @select="handlePay"
    />
  </div>
</template>

<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { cancelOrder, fetchOrder, payOrder, type Order, type PaymentChannel } from '@/api/orders'
import {
  STAGE_LABELS,
  STATUS_LABELS,
  formatDateLabel,
  formatDateTime,
  progressSteps,
} from '@/utils/order'

const router = useRouter()
const route = useRoute()

const order = ref<Order | null>(null)
const loading = ref(true)
const paySheet = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

const payActions = [
  { name: '微信支付', value: 'WECHAT' },
  { name: '支付宝', value: 'ALIPAY' },
]

const steps = computed(() => (order.value ? progressSteps(order.value) : []))
const showProgress = computed(
  () => order.value !== null && ['PAID', 'IN_SERVICE', 'COMPLETED'].includes(order.value.status),
)

const stageHint = computed(() => {
  if (!order.value) return ''
  switch (order.value.stage) {
    case 'PENDING_PAYMENT':
      return '请在 30 分钟内完成支付，超时订单会自动取消并释放名额'
    case 'PENDING_DEPARTURE':
      return '护工将按预约时间上门，请保持电话畅通'
    case 'ON_THE_WAY':
      return '护工正在前往服务地点'
    case 'ARRIVED':
      return '护工已到达，即将开始服务'
    case 'IN_SERVICE':
      return '服务进行中，可随时查看进度'
    case 'COMPLETED':
      return '服务已完成，感谢您的信任'
    default:
      return '订单已取消，名额已释放'
  }
})

async function load() {
  const id = Number(route.params.id)
  if (!id) {
    showToast('订单不存在')
    router.replace('/orders')
    return
  }
  try {
    order.value = await fetchOrder(id)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    loading.value = false
  }
  schedulePolling()
}

function schedulePolling() {
  if (timer) clearInterval(timer)
  if (!order.value) return
  if (!['PAID', 'IN_SERVICE'].includes(order.value.status)) return
  timer = setInterval(() => {
    void loadSilently()
  }, 15_000)
}

async function loadSilently() {
  try {
    order.value = await fetchOrder(Number(route.params.id))
    if (!['PAID', 'IN_SERVICE'].includes(order.value.status) && timer) {
      clearInterval(timer)
      timer = undefined
    }
  } catch {
    // 轮询失败不打扰用户
  }
}

async function handlePay(action: { name: string; value: PaymentChannel }) {
  if (!order.value) return
  try {
    await payOrder(order.value.id, action.value)
    showToast('支付成功')
    await load()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '支付失败')
  }
}

async function confirmCancel() {
  if (!order.value) return
  try {
    await showConfirmDialog({
      title: '取消订单',
      message: '服务开始前 2 小时以上可全额退款；2 小时内取消收取 20% 手续费。确定取消吗？',
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }
  try {
    const result = await cancelOrder(order.value.id, '用户取消')
    showToast(result.refundAmount > 0 ? `已取消，退款 ¥${result.refundAmount}` : '订单已取消')
    await load()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '取消失败')
  }
}

onMounted(load)
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.order-detail {
  min-height: 100vh;
  padding: 16px 16px 32px;
}

.order-detail__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.order-detail__header h1 {
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

.status-hero {
  position: relative;
  padding: 22px 20px;
  margin-bottom: 16px;
  color: #fff;
  background: linear-gradient(140deg, #0891b2, #0e7490);
  border-radius: var(--yh-radius-lg);
  box-shadow: 0 12px 28px rgba(8, 145, 178, 0.24);
}

.status-hero--completed {
  background: linear-gradient(140deg, #475569, #1e293b);
}

.status-hero--cancelled {
  background: linear-gradient(140deg, #94a3b8, #64748b);
}

.status-hero--pending_payment {
  background: linear-gradient(140deg, #f59e0b, #d97706);
}

.status-hero__eyebrow {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.04em;
  opacity: 0.8;
}

.status-hero h2 {
  margin: 8px 0 6px;
  font-size: 24px;
}

.status-hero__hint {
  max-width: 90%;
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.88;
}

.status-hero__pill {
  position: absolute;
  top: 18px;
  right: 18px;
  padding: 4px 10px;
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999px;
}

.card {
  padding: 16px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.card__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.timeline__item {
  display: flex;
  gap: 12px;
  align-items: center;
  color: #94a3b8;
}

.timeline__dot {
  display: grid;
  flex: none;
  place-items: center;
  width: 24px;
  height: 24px;
  font-size: 11px;
  background: #f1f5f9;
  border-radius: 50%;
}

.timeline__item.done {
  color: var(--yh-primary);
}

.timeline__item.done .timeline__dot {
  color: #fff;
  background: var(--yh-primary);
}

.timeline__item.active .timeline__dot {
  box-shadow: 0 0 0 4px rgba(8, 145, 178, 0.15);
}

.timeline__body strong {
  display: block;
  font-size: 13px;
}

.timeline__body small {
  font-size: 11px;
}

.timeline__tip {
  display: flex;
  gap: 6px;
  align-items: center;
  margin: 14px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.info-list {
  margin: 0;
}

.info-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
}

.info-list dt {
  color: var(--yh-text-secondary);
}

.info-list dd {
  margin: 0;
  text-align: right;
}

.info-list .amount {
  font-size: 16px;
  font-weight: 700;
  color: var(--yh-primary);
}

.contact-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.contact-card strong {
  font-size: 14px;
}

.contact-card p {
  margin: 4px 0 0;
}

.call-button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--yh-primary);
  text-decoration: none;
  background: var(--yh-primary-light);
  border-radius: 999px;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
}

.rule-note {
  margin: 18px 4px 0;
  font-size: 11px;
  line-height: 1.7;
  color: #94a3b8;
}

.state-block {
  display: grid;
  place-items: center;
  padding: 80px 0;
}
</style>
