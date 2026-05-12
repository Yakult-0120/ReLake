<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDatasources, getDatasourceList, createDatasource, updateDatasource, deleteDatasource, testDatasource,
  type Datasource, type PageResult,
} from '@/api/datasource'

const loading = ref(false)
const tableData = ref<Datasource[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('创建数据源')
const isEdit = ref(false)
const submitting = ref(false)

const form = reactive<Datasource>({
  name: '',
  dbType: 'MYSQL',
  host: '',
  port: 3306,
  dbName: '',
  username: '',
  password: '',
  description: '',
})

const dbTypes = ['MYSQL', 'POSTGRESQL']

async function fetchData() {
  loading.value = true
  try {
    const res = await getDatasources({ page: page.value, size: size.value, keyword: keyword.value })
    const result = res.data.data
    tableData.value = result.records
    total.value = result.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handleReset() {
  keyword.value = ''
  page.value = 1
  fetchData()
}

function handleAdd() {
  dialogTitle.value = '创建数据源'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    name: '',
    dbType: 'MYSQL',
    host: '',
    port: 3306,
    dbName: '',
    username: '',
    password: '',
    description: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Datasource) {
  dialogTitle.value = '编辑数据源'
  isEdit.value = true
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateDatasource({ ...form })
      ElMessage.success('更新成功')
    } else {
      await createDatasource({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Datasource) {
  try {
    await ElMessageBox.confirm(`确定删除数据源「${row.name}」吗？`, '确认删除', {
      type: 'warning',
    })
    await deleteDatasource(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // cancelled or handled by interceptor
  }
}

async function handleTest(row: Datasource) {
  try {
    await testDatasource(row.id!)
    ElMessage.success('连接测试成功')
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">数据源管理</h2>

    <el-card>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="handleAdd" style="margin-left: auto">新增数据源</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" width="180" />
        <el-table-column prop="dbType" label="数据库类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.dbType === 'MYSQL' ? 'primary' : 'success'" size="small">
              {{ row.dbType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机" width="160" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="dbName" label="数据库" width="140" />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleTest(row)" link type="success">
              测试连接
            </el-button>
            <el-button size="small" @click="handleEdit(row)" link type="primary">
              编辑
            </el-button>
            <el-button size="small" @click="handleDelete(row)" link type="danger">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[5, 10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="dialogVisible = false">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="数据源名称" />
        </el-form-item>
        <el-form-item label="数据库类型">
          <el-select v-model="form.dbType" style="width: 100%">
            <el-option v-for="t in dbTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" required>
          <el-input v-model="form.host" placeholder="如 127.0.0.1" />
        </el-form-item>
        <el-form-item label="端口" required>
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据库名" required>
          <el-input v-model="form.dbName" placeholder="数据库名" />
        </el-form-item>
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" :required="!isEdit">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空表示不修改' : '密码'" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
