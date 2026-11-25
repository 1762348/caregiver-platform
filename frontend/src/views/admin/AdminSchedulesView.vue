<template>
  <section class="page">
    <header class="page__head">
      <div>
        <h1>排班与名额</h1>
        <p>按护工维护可预约时段；停班将自动取消该时段订单并全额退款。</p>
      </div>
      <div class="page__actions">
        <el-button @click="openGenerate">批量生成</el-button>
        <el-button type="primary" @click="openCreate">新增排班</el-button>
      </div>
    </header>

    <div class="filters">
      <el-select
        v-model="caregiverId"
        placeholder="选择护工"
        filterable
        style="width: 220px"
        @change="load"
      >
        <el-option
          v-for="item in caregivers"
          :key="item.id"
          :label="`${item.name}（${item.code}）`"
          :value="item.id"
        />
      </el-select>
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <el-button @click="load">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="schedules" stripe style="width: 100%">
      <el-table-column prop="serviceDate" label="日期" width="120" />
      <el-table-column prop="timeRange" label="时段" width="130" />
      <el-table-column prop="totalQuota" label="总名额" width="90" />
      <el-table-column prop="remainingQuota" label="剩余" width="80" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'">
            {{ row.status === 'OPEN' ? '可预约' : '已停班' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editQuota(row as AdminSchedule)">名额</el-button>
          <el-button
            link
            type="warning"
            :disabled="row.status === 'CLOSED'"
            @click="stop(row as AdminSchedule)"
          >
            停班
          </el-button>
          <el-button link type="danger" @click="remove(row as AdminSchedule)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="generateVisible" title="批量生成排班" width="460px">
      <el-form label-width="90px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="generateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
          />
        </el-form-item>
        <el-form-item label="每时段名额">
          <el-input-number v-model="generateQuota" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <p class="hint">按护工的基础时段生成，已存在的时段会自动跳过。</p>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="generate">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" title="新增排班" width="460px">
      <el-form label-width="90px">
        <el-form-item label="日期" required>
          <el-date-picker v-model="createForm.serviceDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-picker
            v-model="createForm.startTime"
            value-format="HH:mm"
            format="HH:mm"
            placeholder="08:00"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-picker
            v-model="createForm.endTime"
            value-format="HH:mm"
            format="HH:mm"
            placeholder="12:00"
          />
        </el-form-item>
        <el-form-item label="名额" required>
          <el-input-number v-model="createForm.totalQuota" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="createForm.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimePicker,
} from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createSchedule,
  generateSchedules,
  listCaregivers,
  listSchedules,
  stopSchedule,
  updateQuota,
  type AdminSchedule,
} from '@/api/admin'
import { deleteSchedule } from '@/api/admin'

const caregivers = ref<Array<{ id: number; name: string; code: string }>>([])
const caregiverId = ref<number | null>(null)
const range = ref<[string, string] | null>(null)
const schedules = ref<AdminSchedule[]>([])
const loading = ref(false)
const saving = ref(false)

const generateVisible = ref(false)
const generateRange = ref<[string, string] | null>(null)
const generateQuota = ref(3)

const createVisible = ref(false)
const createForm = reactive({
  serviceDate: '',
  startTime: '08:00',
  endTime: '12:00',
  totalQuota: 3,
  price: 200,
})

function isoDate(offset: number) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

async function load() {
  if (!caregiverId.value) return
  const [from, to] = range.value ?? [isoDate(0), isoDate(13)]
  loading.value = true
  try {
    schedules.value = await listSchedules(caregiverId.value, from, to)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排班加载失败')
  } finally {
    loading.value = false
  }
}

function openGenerate() {
  if (!caregiverId.value) {
    ElMessage.warning('请先选择护工')
    return
  }
  generateRange.value = [isoDate(14), isoDate(20)]
  generateVisible.value = true
}

function openCreate() {
  if (!caregiverId.value) {
    ElMessage.warning('请先选择护工')
    return
  }
  createForm.serviceDate = isoDate(1)
  createVisible.value = true
}

async function generate() {
  if (!caregiverId.value || !generateRange.value) {
    ElMessage.warning('请选择日期范围')
    return
  }
  saving.value = true
  try {
    const created = await generateSchedules({
      caregiverId: caregiverId.value,
      from: generateRange.value[0],
      to: generateRange.value[1],
      totalQuota: generateQuota.value,
    })
    ElMessage.success(`已生成 ${created} 条排班`)
    generateVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成失败')
  } finally {
    saving.value = false
  }
}

async function create() {
  if (!caregiverId.value || !createForm.serviceDate || !createForm.startTime || !createForm.endTime) {
    ElMessage.warning('请完整填写日期与时段')
    return
  }
  saving.value = true
  try {
    await createSchedule({
      caregiverId: caregiverId.value,
      serviceDate: createForm.serviceDate,
      startTime: createForm.startTime,
      endTime: createForm.endTime,
      totalQuota: createForm.totalQuota,
      price: createForm.price,
    })
    ElMessage.success('已新增')
    createVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function editQuota(row: AdminSchedule) {
  try {
    const { value } = await ElMessageBox.prompt(
      `当前总名额 ${row.totalQuota}，请输入新的剩余名额（0-${row.totalQuota}）`,
      '调整名额',
      {
        inputValue: String(row.remainingQuota),
        inputPattern: /^\d+$/,
        inputErrorMessage: '请输入非负整数',
      },
    )
    await updateQuota(row.id, Number(value))
    ElMessage.success('名额已更新')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '调整失败')
    }
  }
}

async function stop(row: AdminSchedule) {
  try {
    const { value } = await ElMessageBox.prompt(
      '停班后该时段将不可预约，已支付订单会全额退款。请填写停班原因：',
      '停班确认',
      { inputValue: '护工临时请假', confirmButtonText: '确认停班' },
    )
    const result = await stopSchedule(row.id, value)
    ElMessage.success(
      `已停班，取消 ${result.cancelledOrders} 单，退款 ¥${result.refundTotal}`,
    )
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '停班失败')
    }
  }
}

async function remove(row: AdminSchedule) {
  try {
    await ElMessageBox.confirm('确定删除该排班吗？已有订单时不可删除。', '删除排班', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteSchedule(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

onMounted(async () => {
  range.value = [isoDate(0), isoDate(13)]
  try {
    const page = await listCaregivers({ page: 0, size: 50 })
    caregivers.value = page.items.map((item) => ({
      id: item.id,
      name: item.name,
      code: item.code,
    }))
    caregiverId.value = caregivers.value[0]?.id ?? null
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '护工列表加载失败')
  }
})
</script>

<style scoped>
.page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page__head h1 {
  margin: 0;
  font-size: 20px;
}

.page__head p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #64748b;
}

.page__actions {
  display: flex;
  gap: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}
</style>
