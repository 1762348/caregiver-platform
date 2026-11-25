<template>
  <section class="page">
    <header class="page__head">
      <div>
        <h1>服务类别</h1>
        <p>维护护理服务类型与基础价格，删除前会校验是否仍有护工使用。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增类别</el-button>
    </header>

    <el-table v-loading="loading" :data="categories" stripe style="width: 100%">
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="basePrice" label="基础价" width="110">
        <template #default="{ row }">¥{{ row.basePrice }}</template>
      </el-table-column>
      <el-table-column prop="caregiverCount" label="护工数" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as AdminCategory)">编辑</el-button>
          <el-button link type="danger" @click="remove(row as AdminCategory)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑类别' : '新增类别'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" maxlength="30" placeholder="如 DAILY_CARE" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="基础价" required>
          <el-input-number v-model="form.basePrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 140px">
            <el-option label="启用" value="ACTIVE" />
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
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
  type AdminCategory,
  type CategoryPayload,
} from '@/api/admin'

const categories = ref<AdminCategory[]>([])
const loading = ref(true)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<AdminCategory | null>(null)

const form = reactive<CategoryPayload>({
  code: '',
  name: '',
  description: '',
  basePrice: 100,
  status: 'ACTIVE',
})

async function load() {
  loading.value = true
  try {
    categories.value = await listCategories()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    code: '',
    name: '',
    description: '',
    basePrice: 100,
    status: 'ACTIVE',
  })
  dialogVisible.value = true
}

function openEdit(row: AdminCategory) {
  editing.value = row
  Object.assign(form, {
    code: row.code,
    name: row.name,
    description: row.description ?? '',
    basePrice: row.basePrice,
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.code.trim() || !form.name.trim()) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateCategory(editing.value.id, { ...form })
      ElMessage.success('已保存')
    } else {
      await createCategory({ ...form })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: AdminCategory) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」吗？若仍有护工使用该类别将无法删除。`,
      '删除类别',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
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
</style>
