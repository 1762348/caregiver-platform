<template>
  <div class="receivers">
    <header class="receivers__header">
      <button type="button" class="icon-button" @click="router.back()">
        <van-icon name="arrow-left" size="18" />
      </button>
      <h1>被照顾人</h1>
      <button type="button" class="icon-button" @click="openForm()">
        <van-icon name="plus" size="18" />
      </button>
    </header>

    <div v-if="loading" class="state-block"><van-loading size="24" /></div>

    <van-empty v-else-if="!receivers.length" description="还没有添加被照顾人">
      <van-button type="primary" round @click="openForm()">添加第一位</van-button>
    </van-empty>

    <div v-else class="receiver-list">
      <article v-for="receiver in receivers" :key="receiver.id" class="receiver-card">
        <div class="receiver-card__head">
          <span class="receiver-card__avatar">{{ receiver.name.slice(0, 1) }}</span>
          <div>
            <h3>
              {{ receiver.name }}
              <small v-if="receiver.relation">{{ receiver.relation }}</small>
            </h3>
            <p class="text-muted">
              {{ receiver.gender === 'MALE' ? '男' : receiver.gender === 'FEMALE' ? '女' : '未知' }}
              <template v-if="receiver.birth"> · {{ receiver.birth }}</template>
            </p>
          </div>
        </div>
        <dl class="receiver-card__info">
          <div v-if="receiver.health">
            <dt>健康状况</dt>
            <dd>{{ receiver.health }}</dd>
          </div>
          <div v-if="receiver.special">
            <dt>特殊需求</dt>
            <dd>{{ receiver.special }}</dd>
          </div>
          <div v-if="receiver.phone">
            <dt>联系电话</dt>
            <dd>{{ receiver.phone }}</dd>
          </div>
        </dl>
        <div class="receiver-card__actions">
          <van-button size="small" plain @click="openForm(receiver)">编辑</van-button>
          <van-button size="small" plain type="danger" @click="remove(receiver)">删除</van-button>
        </div>
      </article>
    </div>

    <van-popup v-model:show="formVisible" position="bottom" round :style="{ height: '84%' }">
      <div class="receiver-form">
        <h2>{{ editing ? '编辑被照顾人' : '添加被照顾人' }}</h2>
        <van-field v-model="form.name" label="姓名" placeholder="请输入姓名" maxlength="50" />
        <van-field
          v-model="form.idCard"
          label="身份证"
          placeholder="18 位身份证号"
          maxlength="18"
        />
        <van-field
          v-model="form.relation"
          label="关系"
          placeholder="本人 / 母亲 / 父亲"
          maxlength="20"
        />
        <van-field v-model="form.phone" label="电话" type="tel" placeholder="选填" maxlength="11" />
        <van-field
          v-model="form.health"
          label="健康状况"
          type="textarea"
          rows="2"
          autosize
          placeholder="如：高血压、糖尿病"
        />
        <van-field
          v-model="form.special"
          label="特殊需求"
          type="textarea"
          rows="2"
          autosize
          placeholder="如：低盐饮食、需要搀扶"
        />
        <p class="receiver-form__tip">身份证用于核验身份，并自动解析性别与出生日期。</p>
        <van-button block round type="primary" :loading="saving" @click="save">保存</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { showConfirmDialog, showToast } from 'vant'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  createReceiver,
  deleteReceiver,
  fetchReceivers,
  updateReceiver,
  type Receiver,
  type ReceiverPayload,
} from '@/api/receivers'

const router = useRouter()
const receivers = ref<Receiver[]>([])
const loading = ref(true)
const formVisible = ref(false)
const saving = ref(false)
const editing = ref<Receiver | null>(null)

const form = reactive<ReceiverPayload>({
  name: '',
  idCard: '',
  relation: '',
  phone: '',
  health: '',
  special: '',
})

async function load() {
  loading.value = true
  try {
    receivers.value = await fetchReceivers()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openForm(receiver?: Receiver) {
  editing.value = receiver ?? null
  form.name = receiver?.name ?? ''
  form.idCard = receiver?.idCard ?? ''
  form.relation = receiver?.relation ?? ''
  form.phone = receiver?.phone ?? ''
  form.health = receiver?.health ?? ''
  form.special = receiver?.special ?? ''
  formVisible.value = true
}

async function save() {
  if (!form.name.trim()) {
    showToast('请输入姓名')
    return
  }
  if (form.idCard.trim().length !== 18) {
    showToast('请输入 18 位身份证号')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateReceiver(editing.value.id, { ...form })
      showToast('已保存')
    } else {
      await createReceiver({ ...form })
      showToast('已添加')
    }
    formVisible.value = false
    await load()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(receiver: Receiver) {
  try {
    await showConfirmDialog({
      title: '删除被照顾人',
      message: `确定删除「${receiver.name}」吗？已有订单时无法删除。`,
      confirmButtonText: '删除',
      confirmButtonColor: '#ef4444',
    })
  } catch {
    return
  }
  try {
    await deleteReceiver(receiver.id)
    showToast('已删除')
    await load()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.receivers {
  min-height: 100vh;
  padding: 16px 16px 32px;
}

.receivers__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.receivers__header h1 {
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

.receiver-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.receiver-card {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--yh-border);
  border-radius: var(--yh-radius-md);
}

.receiver-card__head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.receiver-card__avatar {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  font-weight: 700;
  color: var(--yh-primary);
  background: var(--yh-primary-light);
  border-radius: 14px;
}

.receiver-card h3 {
  margin: 0;
  font-size: 16px;
}

.receiver-card h3 small {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 400;
  color: var(--yh-primary);
}

.receiver-card__head p {
  margin: 4px 0 0;
}

.receiver-card__info {
  margin: 12px 0 0;
}

.receiver-card__info div {
  display: flex;
  gap: 12px;
  padding: 6px 0;
  font-size: 12px;
}

.receiver-card__info dt {
  flex: none;
  width: 60px;
  color: var(--yh-text-secondary);
}

.receiver-card__info dd {
  margin: 0;
}

.receiver-card__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px dashed var(--yh-border);
}

.receiver-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
  height: 100%;
  padding: 20px 8px calc(20px + env(safe-area-inset-bottom));
  overflow-y: auto;
}

.receiver-form h2 {
  margin: 0 0 8px;
  font-size: 18px;
  text-align: center;
}

.receiver-form__tip {
  margin: 8px 16px 16px;
  font-size: 11px;
  line-height: 1.6;
  color: #94a3b8;
}

.state-block {
  display: grid;
  place-items: center;
  padding: 60px 0;
}
</style>
