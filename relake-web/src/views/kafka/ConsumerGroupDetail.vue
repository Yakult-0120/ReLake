<script setup lang="ts">
import { ref, watch } from 'vue'
import { describeConsumerGroup, type ConsumerGroupDetail as CgDetail } from '@/api/kafka'

const props = defineProps<{ targetId: number | string; groupId: string }>()

const loading = ref(false)
const detail = ref<CgDetail | null>(null)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await describeConsumerGroup(props.targetId, props.groupId)
    detail.value = res.data.data
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function lagColor(lag: number) {
  if (lag > 10000) return 'var(--el-color-danger)'
  if (lag > 1000) return 'var(--el-color-warning)'
  return ''
}

watch(() => props.groupId, fetchDetail, { immediate: true })
</script>

<template>
  <div class="cg-detail" v-loading="loading">
    <template v-if="detail">
      <div v-for="member in detail.members" :key="member.memberId" class="member-block">
        <div class="member-header">
          <span class="member-id">{{ member.memberId }}</span>
          <span class="member-meta">{{ member.clientId }} @ {{ member.host }}</span>
        </div>
        <el-table :data="member.partitions" size="small" border v-if="member.partitions.length">
          <el-table-column prop="topic" label="Topic" width="200" />
          <el-table-column prop="partition" label="Partition" width="100" align="center" />
          <el-table-column prop="currentOffset" label="Current Offset" width="140" align="right" />
          <el-table-column prop="endOffset" label="End Offset" width="140" align="right" />
          <el-table-column prop="lag" label="Lag" width="120" align="right">
            <template #default="{ row }">
              <span :style="{ color: lagColor(row.lag), fontWeight: row.lag > 10000 ? 600 : 400 }">
                {{ row.lag.toLocaleString() }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="no-partitions">无分区分配</div>
      </div>
    </template>
    <div v-else-if="!loading" class="no-data">暂无详情数据</div>
  </div>
</template>

<style scoped>
.cg-detail {
  padding: 16px 24px;
  background: var(--rl-bg-secondary);
}

.member-block {
  margin-bottom: 16px;
}
.member-block:last-child { margin-bottom: 0; }

.member-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.member-id {
  font-weight: 600;
  color: var(--rl-text-primary);
  font-size: 13px;
}

.member-meta {
  font-size: 12px;
  color: var(--rl-text-secondary);
}

.no-partitions,
.no-data {
  padding: 12px 0;
  color: var(--rl-text-placeholder);
  font-size: 13px;
}
</style>
