<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTargets, type Target } from '@/api/target'
import { listConsumerGroups, type ConsumerGroupSummary } from '@/api/kafka'
import ConsumerGroupDetail from './ConsumerGroupDetail.vue'

const clusters = ref<Target[]>([])
const selectedTargetId = ref<string | null>(null)
const refreshKey = ref(0)

const loading = ref(false)
const groups = ref<ConsumerGroupSummary[]>([])
const expandedGroup = ref<string | null>(null)

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

async function fetchGroups() {
  if (!selectedTargetId.value) return
  loading.value = true
  try {
    const res = await listConsumerGroups(selectedTargetId.value)
    groups.value = res.data.data || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function handleExpand(row: ConsumerGroupSummary) {
  expandedGroup.value = expandedGroup.value === row.groupId ? null : row.groupId
}

function stateTagType(state: string) {
  const map: Record<string, string> = {
    STABLE: 'success', EMPTY: 'info', DEAD: 'danger',
    PREPARING_REBALANCE: 'warning', COMPLETING_REBALANCE: 'warning',
  }
  return map[state] || 'info'
}

function lagColor(lag: number) {
  if (lag > 10000) return 'var(--el-color-danger)'
  if (lag > 1000) return 'var(--el-color-warning)'
  return ''
}

function handleRefresh() {
  refreshKey.value++
}

onMounted(loadClusters)
watch(selectedTargetId, () => { if (selectedTargetId.value) fetchGroups() })
watch(refreshKey, () => { if (selectedTargetId.value) fetchGroups() })
</script>

<template>
  <div class="cg-panel">
    <div class="kafka-header">
      <h2 class="page-title">消费者组</h2>
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
      <el-table
        :data="groups"
        v-loading="loading"
        border
        stripe
        @expand-change="(row: ConsumerGroupSummary) => handleExpand(row)"
        row-key="groupId"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <ConsumerGroupDetail
              v-if="expandedGroup === row.groupId"
              :target-id="selectedTargetId!"
              :group-id="row.groupId"
            />
          </template>
        </el-table-column>
        <el-table-column prop="groupId" label="Group ID" min-width="220" show-overflow-tooltip />
        <el-table-column prop="state" label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="stateTagType(row.state)" size="small">{{ row.state }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="members" label="成员数" width="80" align="center" />
        <el-table-column prop="subscribedTopics" label="订阅 Topic" width="110" align="center" />
        <el-table-column prop="activeTopics" label="活跃 Topic" width="110" align="center" />
        <el-table-column prop="totalLag" label="总 Lag" width="130" align="right">
          <template #default="{ row }">
            <span :style="{ color: lagColor(row.totalLag), fontWeight: row.totalLag > 10000 ? 600 : 400 }">
              {{ row.totalLag.toLocaleString() }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
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
  margin: 0;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
