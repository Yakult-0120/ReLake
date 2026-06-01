<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTargets, type Target } from '@/api/target'
import { listTopics, createTopic, deleteTopic, type KafkaTopic } from '@/api/kafka'

const clusters = ref<Target[]>([])
const selectedTargetId = ref<string | null>(null)
const refreshKey = ref(0)

const loading = ref(false)
const topics = ref<KafkaTopic[]>([])
const dialogVisible = ref(false)
const form = ref({ topicName: '', numPartitions: 1, replicationFactor: 1 })
const submitting = ref(false)

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

async function fetchTopics() {
  if (!selectedTargetId.value) return
  loading.value = true
  try {
    const res = await listTopics(selectedTargetId.value)
    topics.value = res.data.data || []
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  form.value = { topicName: '', numPartitions: 1, replicationFactor: 1 }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!selectedTargetId.value) return
  submitting.value = true
  try {
    await createTopic(selectedTargetId.value, form.value)
    ElMessage.success('Topic 创建成功')
    dialogVisible.value = false
    fetchTopics()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(topicName: string) {
  if (!selectedTargetId.value) return
  try {
    await ElMessageBox.confirm(`确定删除 Topic「${topicName}」吗？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteTopic(selectedTargetId.value, topicName)
    ElMessage.success('Topic 删除成功')
    fetchTopics()
  } catch {
    // cancelled or handled by interceptor
  }
}

function handleRefresh() {
  refreshKey.value++
}

onMounted(loadClusters)
watch(selectedTargetId, () => { if (selectedTargetId.value) fetchTopics() })
watch(refreshKey, () => { if (selectedTargetId.value) fetchTopics() })
</script>

<template>
  <div class="topic-panel">
    <div class="kafka-header">
      <h2 class="page-title">主题管理</h2>
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
        <el-button type="primary" @click="handleCreate">新建 Topic</el-button>
      </div>

      <el-table :data="topics" v-loading="loading" border stripe>
        <el-table-column prop="name" label="Topic 名称" min-width="200" />
        <el-table-column prop="partitions" label="分区数" width="100" align="center" />
        <el-table-column prop="replicationFactor" label="副本数" width="100" align="center" />
        <el-table-column prop="internal" label="内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.internal ? 'info' : ''" size="small">{{ row.internal ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              link
              @click="handleDelete(row.name)"
              :disabled="row.internal"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新建 Topic" width="420px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Topic 名称" required>
          <el-input v-model="form.topicName" placeholder="如 my-topic" />
        </el-form-item>
        <el-form-item label="分区数">
          <el-input-number v-model="form.numPartitions" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="副本数">
          <el-input-number v-model="form.replicationFactor" :min="1" :max="5" />
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

.toolbar { margin-bottom: 12px; }
</style>
