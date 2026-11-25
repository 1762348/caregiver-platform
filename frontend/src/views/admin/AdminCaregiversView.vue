<template>
  <section class="page">
    <header class="page__head">
      <div>
        <h1>护工管理</h1>
        <p>新增护工时会自动开通护工账号（手机号 + 验证码登录）。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增护工</el-button>
    </header>

    <div class="filters">
      <el-input
        v-model="keyword"
        placeholder="按姓名或工号搜索"
        clearable
        style="width: 240px"
        @keyup.enter="load"
      />
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="caregivers" stripe style="width: 100%">
      <el-table-column prop="code" label="工号" width="110" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="categoryName" label="类别" width="120" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="评分" width="100">
        <template #default="{ row }">{{ row.rating ?? '-' }}（{{ row.reviews ?? 0 }}）</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '在职' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as AdminCaregiver)">编辑</el-button>
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            @click="toggleStatus(row as AdminCaregiver)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑护工' : '新增护工'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="工号" required>
          <el-input v-model="form.code" maxlength="30" placeholder="如 CG0003" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-select v-model="form.gender" style="width: 140px">
            <el-option label="女" value="FEMALE" />
            <el-option label="男" value="MALE" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.birth"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
        <el-form-item label="服务类别">
          <el-select v-model="form.categoryId" clearable style="width: 220px">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="资质">
          <el-input v-model="form.qualification" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 140px">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createCaregiver,
  listCaregivers,
  listCategories,
  updateCaregiver,
  updateCaregiverStatus,
  type AdminCaregiver,
  type AdminCategory,
  type CaregiverPayload,
  type RecordStatus,
} from '@/api/admin'

const caregivers = ref<AdminCaregiver[]>([])
const categories = ref<AdminCategory[]>([])
const loading = ref(true)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<AdminCaregiver | null>(null)
const keyword = ref('')
const statusFilter = ref<RecordStatus | ''>('')

const form = reactive<CaregiverPayload>({
  phone: '',
  name: '',
  gender: 'FEMALE',
  birth: '',
  code: '',
  categoryId: null,
  price: 200,
  qualification: '',
  intro: '',
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    const page = await listCaregivers({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: 0,
      size: 50,
    })
    caregivers.value = page.items
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    phone: '',
    name: '',
    gender: 'FEMALE',
    birth: '',
    code: '',
    categoryId: null,
    price: 200,
    qualification: '',
    intro: '',
    status: 'ACTIVE',
  })
  dialogVisible.value = true
}

function openEdit(row: AdminCaregiver) {
  editing.value = row
  Object.assign(form, {
    phone: row.phone,
    name: row.name,
    gender: row.gender,
    birth: row.birth ?? '',
    code: row.code,
    categoryId: row.categoryId,
    price: row.price,
    qualification: '',
    intro: '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.phone || !form.name || !form.code) {
    ElMessage.warning('手机号、姓名、工号不能为空')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateCaregiver(editing.value.id, { ...form })
      ElMessage.success('已保存')
    } else {
      await createCaregiver({ ...form })
      ElMessage.success('已新增，护工可使用该手机号登录')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminCaregiver) {
  const next: RecordStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await updateCaregiverStatus(row.id, next)
    ElMessage.success(next === 'ACTIVE' ? '已启用' : '已停用')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

onMounted(async () => {
  await Promise.all([
    load(),
    listCategories()
      .then((data) => {
        categories.value = data
      })
      .catch(() => {
        categories.value = []
      }),
  ])
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

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
</style>
