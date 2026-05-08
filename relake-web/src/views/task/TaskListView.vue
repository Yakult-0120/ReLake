<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTasks, createTask, updateTask, deleteTask,
  validateTask, startTask, stopTask, getTaskStatus, getTaskMetrics,
  type Task, type Metrics,
} from '@/api/task'
import { getDatasourceList, type Datasource } from '@/api/datasource'
import { getTargetList, type Target } from '@/api/target'

const loading = ref(false)
const tableData = ref<Task[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('创建同步任务')
const isEdit = ref(false)
const submitting = ref(false)

const form = reactive<Task>({
  name: '',
  datasourceId: '' as string | number,
  targetId: '' as string | number,
  engineType: 'CANAL',
  sourceTables: '',
  configJson: '',
  cronExpr: '',
  description: '',
})

const engineTypes = ['CANAL', 'FLINK_CDC', 'DATAX']
const datasources = ref<Datasource[]>([])
const targets = ref<Target[]>([])

const statusVisible = ref(false)
const statusText = ref('')
const metricsVisible = ref(false)
const metrics = ref<Metrics>({ recordsIn: 0, recordsOut: 0, bytesIn: 0, bytesOut: 0, errorCount: 0, latencyMs: 0 })

function getStatusTag(status: string | undefined) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    VALIDATING: 'warning',
    READY: 'success',
    RUNNING: '',
    FAILED: 'danger',
    FINISHED: 'success',
    STOPPED: 'info',
  }
  return map[status || 'DRAFT'] || 'info'
}

function getStatusLabel(status: string | undefined) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    VALIDATING: '校验中',
    READY: '就绪',
    RUNNING: '运行中',
    FAILED: '失败',
    FINISHED: '已完成',
    STOPPED: '已停止',
  }
  return map[status || 'DRAFT'] || status || '未知'
}

async function loadRefs() {
  try {
    const [dsRes, tRes] = await Promise.all([getDatasourceList(), getTargetList()])
    datasources.value = dsRes.data.data || []
    targets.value = tRes.data.data || []
  } catch {
    // handled
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getTasks({ page: page.value, size: size.value, keyword: keyword.value })
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
  dialogTitle.value = '创建同步任务'
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    name: '',
    datasourceId: datasources.value[0]?.id || '',
    targetId: targets.value[0]?.id || '',
    engineType: 'CANAL',
    sourceTables: '',
    configJson: '',
    cronExpr: '',
    description: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Task) {
  dialogTitle.value = '编辑同步任务'
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value && form.id) {
      await updateTask({ ...form })
      ElMessage.success('更新成功')
    } else {
      await createTask({ ...form })
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

async function handleDelete(row: Task) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.name}」吗？`, '确认删除', {
      type: 'warning',
    })
    await deleteTask(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // cancelled
  }
}

async function handleValidate(row: Task) {
  try {
    await validateTask(row.id!)
    ElMessage.success('校验已提交')
    fetchData()
  } catch {
    // handled
  }
}

async function handleStart(row: Task) {
  try {
    await ElMessageBox.confirm(`确定启动任务「${row.name}」吗？`, '确认启动', {
      type: 'info',
    })
    await startTask(row.id!)
    ElMessage.success('任务已启动')
    fetchData()
  } catch {
    // cancelled or error
  }
}

async function handleStop(row: Task) {
  try {
    await ElMessageBox.confirm(`确定停止任务「${row.name}」吗？`, '确认停止', {
      type: 'warning',
    })
    await stopTask(row.id!)
    ElMessage.success('任务已停止')
    fetchData()
  } catch {
    // cancelled or error
  }
}

async function handleCheckStatus(row: Task) {
  try {
    const res = await getTaskStatus(row.id!)
    statusText.value = res.data.data || 'UNKNOWN'
    statusVisible.value = true
    // 引擎终态时自动刷新列表
    if (statusText.value === 'FINISHED' || statusText.value === 'FAILED') {
      fetchData()
    }
  } catch {
    // handled
  }
}

async function handleCheckMetrics(row: Task) {
  try {
    const res = await getTaskMetrics(row.id!)
    metrics.value = res.data.data
    metricsVisible.value = true
  } catch {
    // handled
  }
}

function getDatasourceName(id: string | number) {
  return datasources.value.find(d => d.id === id)?.name || `#${id}`
}

function getTargetName(id: string | number) {
  return targets.value.find(t => t.id === id)?.name || `#${id}`
}

onMounted(() => {
  loadRefs()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">同步任务管理</h2>

    <el-card>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索任务名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="handleAdd" style="margin-left: auto">创建任务</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="任务名称" width="160" />
        <el-table-column label="数据源" width="120">
          <template #default="{ row }">{{ getDatasourceName(row.datasourceId) }}</template>
        </el-table-column>
        <el-table-column label="目标" width="120">
          <template #default="{ row }">{{ getTargetName(row.targetId) }}</template>
        </el-table-column>
        <el-table-column prop="engineType" label="引擎" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.engineType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTables" label="源表" width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'DRAFT' || row.status === 'FAILED'">
              <el-button size="small" link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" link type="warning" @click="handleValidate(row)">校验</el-button>
              <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else-if="row.status === 'READY'">
              <el-button size="small" link type="success" @click="handleStart(row)">启动</el-button>
              <el-button size="small" link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else-if="row.status === 'RUNNING'">
              <el-button size="small" link type="danger" @click="handleStop(row)">停止</el-button>
              <el-button size="small" link type="info" @click="handleCheckStatus(row)">查看状态</el-button>
              <el-button size="small" link type="info" @click="handleCheckMetrics(row)">查看指标</el-button>
            </template>
            <template v-else-if="row.status === 'STOPPED'">
              <el-button size="small" link type="info" @click="handleCheckStatus(row)">查看状态</el-button>
              <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else>
              <span style="color:#c0c4cc">-</span>
            </template>
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

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" @close="dialogVisible = false">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" placeholder="同步任务名称" />
        </el-form-item>
        <el-form-item label="数据源" required>
          <el-select v-model="form.datasourceId" style="width: 100%" placeholder="选择数据源">
            <el-option
              v-for="ds in datasources"
              :key="ds.id"
              :label="`${ds.name} (${ds.dbType})`"
              :value="ds.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标存储" required>
          <el-select v-model="form.targetId" style="width: 100%" placeholder="选择目标存储">
            <el-option
              v-for="t in targets"
              :key="t.id"
              :label="`${t.name} (${t.storageType})`"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="引擎类型" required>
          <el-select v-model="form.engineType" style="width: 100%">
            <el-option v-for="e in engineTypes" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="源表" required>
          <el-input v-model="form.sourceTables" placeholder="多个表用逗号分隔，如: user,order" />
        </el-form-item>
        <el-form-item label="引擎配置">
          <el-input
            v-model="form.configJson"
            type="textarea"
            :rows="4"
            placeholder='JSON 格式的引擎专属配置，如: {"parallelism": 2}'
          />
        </el-form-item>
        <el-form-item label="Cron 表达式" v-if="form.engineType === 'DATAX'">
          <el-input v-model="form.cronExpr" placeholder="如 0 0 2 * * ? (每天凌晨2点)" />
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

    <!-- Status Dialog -->
    <el-dialog v-model="statusVisible" title="任务状态" width="400px">
      <el-result
        :icon="statusText === 'RUNNING' || statusText === 'READY' ? 'success' : 'info'"
        :title="getStatusLabel(statusText)"
        :sub-title="`引擎状态: ${statusText}`"
      />
    </el-dialog>

    <!-- Metrics Dialog -->
    <el-dialog v-model="metricsVisible" title="运行指标" width="480px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="输入记录">{{ metrics.recordsIn }}</el-descriptions-item>
        <el-descriptions-item label="输出记录">{{ metrics.recordsOut }}</el-descriptions-item>
        <el-descriptions-item label="输入字节">{{ metrics.bytesIn }}</el-descriptions-item>
        <el-descriptions-item label="输出字节">{{ metrics.bytesOut }}</el-descriptions-item>
        <el-descriptions-item label="错误数">{{ metrics.errorCount }}</el-descriptions-item>
        <el-descriptions-item label="延迟(ms)">{{ metrics.latencyMs }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1300px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
