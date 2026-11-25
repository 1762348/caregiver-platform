<template>
  <div class="booking">
    <header class="booking__header">
      <button type="button" class="icon-button" @click="goBack">
        <van-icon name="arrow-left" size="18" />
      </button>
      <div class="booking__title">
        <h1>预约护工</h1>
        <p>第 {{ step }}/4 步 · {{ STEP_TITLES[step - 1] }}</p>
      </div>
      <span class="booking__step">{{ step }}/4</span>
    </header>

    <div class="progress-bar">
      <span v-for="index in 4" :key="index" :class="{ active: index <= step }" />
    </div>

    <!-- 1. 服务类别 -->
    <section v-if="step === 1" class="section">
      <div class="category-grid">
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          class="category-card"
          :class="{ selected: selectedCategory?.id === category.id }"
          @click="selectCategory(category)"
        >
          <span class="category-card__icon">
            <van-icon :name="categoryIcons[category.code] ?? 'service-o'" size="22" />
          </span>
          <strong>{{ category.name }}</strong>
          <small>{{ category.description }}</small>
          <em>¥{{ category.basePrice }} 起</em>
        </button>
      </div>
    </section>

    <!-- 2. 日期与排班 -->
    <section v-else-if="step === 2" class="section">
      <div class="date-row">
        <button
          v-for="day in dateOptions"
          :key="day.value"
          type="button"
          class="date-chip"
          :class="{ selected: day.value === selectedDate }"
          @click="selectDate(day.value)"
        >
          <strong>{{ day.label }}</strong>
          <small>{{ day.value.slice(5) }}</small>
        </button>
      </div>

      <div v-if="loadingSchedules" class="state-block">
        <van-loading size="22" />
        <span>正在查询可约时段…</span>
      </div>
      <van-empty v-else-if="!schedules.length" description="当天暂无可约时段">
        <p class="text-muted">可以切换日期或服务类别再看看</p>
      </van-empty>
      <div v-else class="slot-list">
        <button
          v-for="slot in schedules"
          :key="slot.id"
          type="button"
          class="slot-card"
          :class="{ selected: selectedSchedule?.id === slot.id }"
          @click="selectSchedule(slot)"
        >
          <div class="slot-card__time">{{ slot.timeRange }}</div>
          <div class="slot-card__body">
            <strong>{{ slot.caregiverName }}</strong>
            <small
              >{{ slot.categoryName }} · 剩 {{ slot.remainingQuota }}/{{
                slot.totalQuota
              }}
              名额</small
            >
          </div>
          <div class="slot-card__price">¥{{ slot.price }}</div>
        </button>
      </div>
    </section>

    <!-- 3. 被照顾人 -->
    <section v-else-if="step === 3" class="section">
      <div v-if="loadingReceivers" class="state-block">
        <van-loading size="22" />
      </div>
      <van-empty v-else-if="!receivers.length" description="还没有被照顾人">
        <van-button type="primary" round @click="router.push('/receivers?from=booking')">
          去添加
        </van-button>
      </van-empty>
      <div v-else class="receiver-list">
        <button
          v-for="receiver in receivers"
          :key="receiver.id"
          type="button"
          class="receiver-card"
          :class="{ selected: selectedReceiver?.id === receiver.id }"
          @click="selectedReceiver = receiver"
        >
          <span class="receiver-card__avatar">{{ receiver.name.slice(0, 1) }}</span>
          <div class="receiver-card__body">
            <strong
              >{{ receiver.name }}<small>{{ receiver.relation }}</small></strong
            >
            <p>{{ receiver.health || '暂无健康备注' }}</p>
          </div>
          <van-icon name="success" class="receiver-card__check" />
        </button>
      </div>
    </section>

    <!-- 4. 确认 -->
    <section v-else class="section">
      <div class="card summary">
        <div class="summary__row">
          <span>服务类别</span><strong>{{ selectedCategory?.name }}</strong>
        </div>
        <div class="summary__row">
          <span>护工</span><strong>{{ selectedSchedule?.caregiverName }}</strong>
        </div>
        <div class="summary__row">
          <span>服务时间</span>
          <strong>
            {{ selectedSchedule ? formatDateLabel(selectedSchedule.serviceDate) : '' }}
            {{ selectedSchedule?.timeRange }}
          </strong>
        </div>
        <div class="summary__row">
          <span>被照顾人</span><strong>{{ selectedReceiver?.name }}</strong>
        </div>
        <div class="summary__row summary__row--total">
          <span>应付金额</span>
          <strong>¥{{ selectedSchedule?.price }}</strong>
        </div>
      </div>
      <p class="summary__hint">
        提交后将为你锁定该时段名额，请在 30 分钟内完成支付；服务开始前 2 小时以上取消可全额退款。
      </p>
    </section>

    <footer class="booking__footer">
      <van-button v-if="step > 1" class="booking__back" @click="step -= 1">上一步</van-button>
      <van-button
        type="primary"
        class="booking__next"
        :disabled="!canNext"
        :loading="submitting"
        @click="next"
      >
        {{ step === 4 ? '确认预约' : '下一步' }}
      </van-button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { showToast } from 'vant'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCategories, fetchSchedules, type Category, type Schedule } from '@/api/catalog'
import { createOrder } from '@/api/orders'
import { fetchReceivers, type Receiver } from '@/api/receivers'
import { formatDateLabel, nextDays } from '@/utils/order'

const STEP_TITLES = ['选择服务类别', '选择日期与时段', '选择被照顾人', '确认预约'] as const

const categoryIcons: Record<string, string> = {
  DAILY_CARE: 'friends-o',
  PROFESSIONAL_CARE: 'medal-o',
  REHAB_TRAINING: 'chart-trending-o',
  ESCORT_SERVICE: 'location-o',
  NIGHT_CARE: 'closed-eye',
}

const router = useRouter()
const route = useRoute()

const step = ref(1)
const categories = ref<Category[]>([])
const selectedCategory = ref<Category | null>(null)
const dateOptions = nextDays(14)
const selectedDate = ref(dateOptions[0].value)
const schedules = ref<Schedule[]>([])
const selectedSchedule = ref<Schedule | null>(null)
const receivers = ref<Receiver[]>([])
const selectedReceiver = ref<Receiver | null>(null)
const loadingSchedules = ref(false)
const loadingReceivers = ref(false)
const submitting = ref(false)

const canNext = computed(() => {
  if (step.value === 1) return selectedCategory.value !== null
  if (step.value === 2) return selectedSchedule.value !== null
  if (step.value === 3) return selectedReceiver.value !== null
  return true
})

function selectCategory(category: Category) {
  selectedCategory.value = category
  selectedSchedule.value = null
}

function selectDate(value: string) {
  selectedDate.value = value
  selectedSchedule.value = null
}

function selectSchedule(slot: Schedule) {
  selectedSchedule.value = slot
}

async function loadCategories() {
  try {
    categories.value = await fetchCategories()
    const categoryId = Number(route.query.categoryId)
    if (categoryId) {
      selectedCategory.value = categories.value.find((item) => item.id === categoryId) ?? null
    }
  } catch (error) {
    showToast(error instanceof Error ? error.message : '服务类别加载失败')
  }
}

async function loadSchedules() {
  if (!selectedCategory.value) return
  loadingSchedules.value = true
  try {
    schedules.value = await fetchSchedules({
      date: selectedDate.value,
      categoryId: selectedCategory.value.id,
    })
  } catch (error) {
    schedules.value = []
    showToast(error instanceof Error ? error.message : '排班加载失败')
  } finally {
    loadingSchedules.value = false
  }
}

async function loadReceivers() {
  loadingReceivers.value = true
  try {
    receivers.value = await fetchReceivers()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '被照顾人加载失败')
  } finally {
    loadingReceivers.value = false
  }
}

watch([selectedDate, () => selectedCategory.value?.id], () => {
  if (step.value === 2) void loadSchedules()
})

watch(step, (value) => {
  if (value === 2 && !schedules.value.length) void loadSchedules()
  if (value === 3 && !receivers.value.length) void loadReceivers()
})

async function next() {
  if (!canNext.value) return
  if (step.value < 4) {
    step.value += 1
    return
  }
  await submit()
}

async function submit() {
  if (!selectedSchedule.value || !selectedReceiver.value) return
  submitting.value = true
  try {
    const order = await createOrder({
      scheduleId: selectedSchedule.value.id,
      receiverId: selectedReceiver.value.id,
    })
    showToast('预约成功，请完成支付')
    await router.replace(`/orders/${order.id}`)
  } catch (error) {
    showToast(error instanceof Error ? error.message : '预约失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  if (step.value > 1) {
    step.value -= 1
  } else {
    router.back()
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.booking {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 16px 16px 96px;
}

.booking__header {
  display: flex;
  gap: 12px;
  align-items: center;
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

.booking__title {
  flex: 1;
}

.booking__title h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.booking__title p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.booking__step {
  font-size: 12px;
  color: var(--yh-primary);
}

.progress-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin: 16px 0 4px;
}

.progress-bar span {
  height: 4px;
  background: #e2e8f0;
  border-radius: 4px;
}

.progress-bar span.active {
  background: var(--yh-primary);
}

.section {
  margin-top: 16px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.category-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  padding: 16px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1.5px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.category-card.selected,
.slot-card.selected,
.receiver-card.selected {
  border-color: var(--yh-primary);
  box-shadow: 0 8px 20px rgba(8, 145, 178, 0.14);
}

.category-card__icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--yh-primary);
  background: var(--yh-primary-light);
  border-radius: 12px;
}

.category-card strong {
  font-size: 15px;
}

.category-card small {
  font-size: 11px;
  line-height: 1.5;
  color: var(--yh-text-secondary);
}

.category-card em {
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  color: var(--yh-primary);
}

.date-row {
  display: flex;
  gap: 8px;
  padding-bottom: 6px;
  overflow-x: auto;
}

.date-chip {
  display: flex;
  flex: none;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  width: 60px;
  padding: 10px 0;
  font: inherit;
  cursor: pointer;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: 14px;
}

.date-chip strong {
  font-size: 13px;
}

.date-chip small {
  font-size: 10px;
  color: var(--yh-text-secondary);
}

.date-chip.selected {
  color: #fff;
  background: var(--yh-primary);
  border-color: var(--yh-primary);
}

.date-chip.selected small {
  color: rgba(255, 255, 255, 0.8);
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.slot-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1.5px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.slot-card__time {
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--yh-primary);
  white-space: nowrap;
  background: var(--yh-primary-light);
  border-radius: 10px;
}

.slot-card__body {
  flex: 1;
}

.slot-card__body strong {
  display: block;
  font-size: 14px;
}

.slot-card__body small {
  font-size: 11px;
  color: var(--yh-text-secondary);
}

.slot-card__price {
  font-size: 15px;
  font-weight: 700;
  color: var(--yh-text);
}

.receiver-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.receiver-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  font: inherit;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1.5px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.receiver-card__avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  font-weight: 700;
  color: var(--yh-primary);
  background: var(--yh-primary-light);
  border-radius: 50%;
}

.receiver-card__body {
  flex: 1;
}

.receiver-card__body strong {
  font-size: 15px;
}

.receiver-card__body strong small {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 400;
  color: var(--yh-text-secondary);
}

.receiver-card__body p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--yh-text-secondary);
}

.receiver-card__check {
  color: var(--yh-primary);
  opacity: 0;
}

.receiver-card.selected .receiver-card__check {
  opacity: 1;
}

.card {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.summary__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
}

.summary__row span {
  color: var(--yh-text-secondary);
}

.summary__row--total {
  margin-top: 6px;
  border-top: 1px dashed var(--yh-border);
}

.summary__row--total strong {
  font-size: 20px;
  color: var(--yh-primary);
}

.summary__hint {
  margin: 14px 4px 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--yh-text-secondary);
}

.state-block {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  font-size: 13px;
  color: var(--yh-text-secondary);
}

.booking__footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 50%;
  z-index: 10;
  display: flex;
  gap: 10px;
  width: 100%;
  max-width: 480px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid var(--yh-border);
  transform: translateX(-50%);
  backdrop-filter: blur(8px);
}

.booking__back {
  flex: 0 0 110px;
}

.booking__next {
  flex: 1;
}
</style>
