<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTargets, type Target } from '@/api/target'
import { listAcls, createAcl, deleteAcl, createScramUser, type KafkaAcl, type AclCreateRequest } from '@/api/kafka'

const clusters = ref<Target[]>([])
const selectedTargetId = ref<string | null>(null)
const refreshKey = ref(0)

const loading = ref(false)
const acls = ref<KafkaAcl[]>([])

// ACL dialog
const aclDialogVisible = ref(false)
const aclForm = ref<AclCreateRequest>({
  principal: 'User:',
  resourceType: 'TOPIC',
  resourceName: '',
  operation: 'READ',
  permissionType: 'ALLOW',
  host: '*',
})
const aclSubmitting = ref(false)

const resourceTypes = ['TOPIC', 'GROUP', 'CLUSTER', 'TRANSACTIONAL_ID', 'DELEGATION_TOKEN']
const operations = ['READ', 'WRITE', 'CREATE', 'DELETE', 'ALTER', 'DESCRIBE', 'CLUSTER_ACTION', 'DESCRIBE_CONFIGS', 'ALTER_CONFIGS', 'IDEMPOTENT_WRITE', 'ALL']

// SCRAM user dialog
const scramDialogVisible = ref(false)
const scramForm = ref({ username: '', password: '' })
const scramSubmitting = ref(false)

async function loadClusters() {
  try {
    const res = await getTargets({ page: 1, size: 100, storageType: 'KAFKA' })
    clusters.value = res.data.data.records
    if (clusters.value.length > 0) {
      selectedTargetId.value = String(clusters.value[0].id)
    }
  } catch {
    ElMessage.error('加载 Kafka 集群列表失败')
  }
}

async function fetchAcls() {
  if (!selectedTargetId.value) return
  loading.value = true
  try {
    const res = await listAcls(selectedTargetId.value)
    acls.value = res.data.data || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function handleCreateAcl() {
  aclForm.value = {
    principal: 'User:',
    resourceType: 'TOPIC',
    resourceName: '',
    operation: 'READ',
    permissionType: 'ALLOW',
    host: '*',
  }
  aclDialogVisible.value = true
}

async function handleSubmitAcl() {
  if (!selectedTargetId.value) return
  aclSubmitting.value = true
  try {
    await createAcl(selectedTargetId.value, aclForm.value)
    ElMessage.success('ACL 创建成功')
    aclDialogVisible.value = false
    fetchAcls()
  } catch {
    // handled
  } finally {
    aclSubmitting.value = false
  }
}

async function handleDeleteAcl(row: KafkaAcl) {
  if (!selectedTargetId.value) return
  try {
    await ElMessageBox.confirm(`确定删除 ACL「${row.principal} → ${row.resourceType}:${row.resourceName}」吗？`, '确认删除', { type: 'warning' })
    await deleteAcl(selectedTargetId.value, {
      principal: row.principal,
      resourceType: row.resourceType,
      resourceName: row.resourceName,
      operation: row.operation,
      permissionType: row.permissionType,
      host: row.host,
    })
    ElMessage.success('ACL 删除成功')
    fetchAcls()
  } catch {
    // cancelled or handled
  }
}

function handleCreateScramUser() {
  scramForm.value = { username: '', password: '' }
  scramDialogVisible.value = true
}

async function handleSubmitScram() {
  if (!selectedTargetId.value) return
  scramSubmitting.value = true
  try {
    await createScramUser(selectedTargetId.value, scramForm.value.username, scramForm.value.password)
    ElMessage.success('SCRAM 用户创建成功')
    scramDialogVisible.value = false
  } catch {
    // handled
  } finally {
    scramSubmitting.value = false
  }
}

function tagTypeForResourceType(type: string) {
  const map: Record<string, string> = { TOPIC: 'primary', GROUP: 'success', CLUSTER: 'warning', TRANSACTIONAL_ID: 'info', DELEGATION_TOKEN: '' }
  return map[type] || ''
}

function tagTypeForOperation(op: string) {
  if (['READ', 'DESCRIBE', 'DESCRIBE_CONFIGS'].includes(op)) return 'info'
  if (['WRITE', 'CREATE', 'ALTER', 'ALTER_CONFIGS'].includes(op)) return 'warning'
  if (['DELETE', 'ALL'].includes(op)) return 'danger'
  return ''
}

function handleRefresh() {
  refreshKey.value++
}

onMounted(loadClusters)
watch(selectedTargetId, () => { if (selectedTargetId.value) fetchAcls() })
watch(refreshKey, () => { if (selectedTargetId.value) fetchAcls() })
</script>

<template>
  <div class="acl-panel">
    <div class="kafka-header">
      <h2 class="page-title">权限管理</h2>
      <div class="header-controls">
        <el-select
          v-model="selectedTargetId"
          placeholder="选择 Kafka 集群"
          style="width: 260px"
          @change="refreshKey++"
        >
          <el-option
            v-for="c in clusters"
            :key="c.id"
            :label="c.name + ' (' + c.endpoint + ')'"
            :value="c.id"
          />
        </el-select>
        <el-button @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <div v-if="selectedTargetId">
      <div class="toolbar">
        <el-button type="primary" @click="handleCreateAcl">新建 ACL</el-button>
        <el-button type="success" @click="handleCreateScramUser">新建 SCRAM 用户</el-button>
      </div>

      <el-table :data="acls" v-loading="loading" border stripe>
        <el-table-column prop="principal" label="Principal" width="180" />
        <el-table-column prop="resourceType" label="资源类型" width="120">
          <template #default="{ row }">
            <el-tag :type="tagTypeForResourceType(row.resourceType)" size="small">{{ row.resourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceName" label="资源名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作" width="100">
          <template #default="{ row }">
            <el-tag :type="tagTypeForOperation(row.operation)" size="small">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permissionType" label="权限" width="90">
          <template #default="{ row }">
            <el-tag :type="row.permissionType === 'ALLOW' ? 'success' : 'danger'" size="small">{{ row.permissionType === 'ALLOW' ? '允许' : '拒绝' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="handleDeleteAcl(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ACL 创建对话框 -->
    <el-dialog v-model="aclDialogVisible" title="新建 ACL" width="480px">
      <el-form :model="aclForm" label-width="100px">
        <el-form-item label="Principal" required>
          <el-input v-model="aclForm.principal" placeholder="如 User:alice" />
        </el-form-item>
        <el-form-item label="资源类型" required>
          <el-select v-model="aclForm.resourceType" style="width:100%">
            <el-option v-for="t in resourceTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源名称" required>
          <el-input v-model="aclForm.resourceName" placeholder="Topic 名 / Group 名 / kafka-cluster" />
        </el-form-item>
        <el-form-item label="操作" required>
          <el-select v-model="aclForm.operation" style="width:100%">
            <el-option v-for="o in operations" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限类型">
          <el-radio-group v-model="aclForm.permissionType">
            <el-radio value="ALLOW">允许 (ALLOW)</el-radio>
            <el-radio value="DENY">拒绝 (DENY)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主机">
          <el-input v-model="aclForm.host" placeholder="默认 * 表示所有主机" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aclDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aclSubmitting" @click="handleSubmitAcl">确定</el-button>
      </template>
    </el-dialog>

    <!-- SCRAM 用户创建对话框 -->
    <el-dialog v-model="scramDialogVisible" title="新建 SCRAM 用户" width="400px">
      <el-form :model="scramForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="scramForm.username" placeholder="SASL 用户名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="scramForm.password" type="password" show-password placeholder="用户密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scramDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scramSubmitting" @click="handleSubmitScram">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.kafka-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--rl-text-primary);
  margin: 0;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
</style>
