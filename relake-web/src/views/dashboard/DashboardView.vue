<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { getTaskList } from '@/api/task'
import { Coin, FolderOpened, Connection, VideoPlay } from '@element-plus/icons-vue'

interface Stats {
  datasourceCount: number
  targetCount: number
  taskCount: number
  runningTaskCount: number
}

const stats = ref<Stats>({
  datasourceCount: 0,
  targetCount: 0,
  taskCount: 0,
  runningTaskCount: 0,
})

async function fetchStats() {
  try {
    const res = await request.get('/api/v1/datasources?page=1&size=1')
    // 后端 Jackson 配置将 Long 序列化为字符串，需转换
    stats.value.datasourceCount = Number(res.data?.data?.total) || 0
  } catch { /* ignore */ }

  try {
    const res = await request.get('/api/v1/targets?page=1&size=1')
    stats.value.targetCount = Number(res.data?.data?.total) || 0
  } catch { /* ignore */ }

  try {
    const res = await getTaskList()
    const tasks = (res as any).data?.data || []
    stats.value.taskCount = tasks.length
    stats.value.runningTaskCount = tasks.filter((t: any) => t.status === 'RUNNING').length
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchStats()
})
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon ds"><el-icon :size="22"><Connection /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.datasourceCount }}</div>
          <div class="stat-label">数据源</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon tg"><el-icon :size="22"><FolderOpened /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.targetCount }}</div>
          <div class="stat-label">目标存储</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon tk"><el-icon :size="22"><Coin /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.taskCount }}</div>
          <div class="stat-label">同步任务</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon run"><el-icon :size="22"><VideoPlay /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.runningTaskCount }}</div>
          <div class="stat-label">运行中</div>
        </div>
      </div>
    </div>

    <div class="intro-card">
      <div class="intro-header">
        <h3>ReLake 实时数据湖平台</h3>
        <p>多引擎 CDC 数据采集，实时同步到 Apache Paimon 湖仓一体存储</p>
      </div>
      <div class="engine-grid">
        <div class="engine-item">
          <div class="engine-badge canal">Canal</div>
          <p>MySQL binlog 实时解析，增量采集，轻量级部署</p>
        </div>
        <div class="engine-item">
          <div class="engine-badge flink">Flink CDC</div>
          <p>全量+增量一体化，多数据源支持，Exactly-Once 语义</p>
        </div>
        <div class="engine-item">
          <div class="engine-badge datax">DataX</div>
          <p>离线批量同步，全量初始化，XXL-JOB 定时调度</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard { max-width: 1100px; }

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 24px;
  font-family: 'Share Tech Mono', monospace;
  letter-spacing: 0.05em;
  color: var(--rl-text-primary);
}

/* 统计卡片 */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }

.stat-card {
  display: flex; align-items: center; gap: 14px;
  background: var(--rl-bg-card); border: 1px solid var(--rl-border-color); border-radius: 10px; padding: 20px;
  transition: border-color 0.2s;
}
.stat-card:hover { border-color: var(--rl-accent); }

.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.stat-icon.ds  { background: rgba(6, 182, 212, 0.12); color: var(--rl-accent); }
.stat-icon.tg  { background: rgba(16, 185, 129, 0.12); color: #10b981; }
.stat-icon.tk  { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.stat-icon.run { background: rgba(239, 68, 68, 0.12); color: #ef4444; }

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--rl-text-primary);
  font-family: 'Share Tech Mono', monospace;
}
.stat-label { font-size: 13px; color: var(--rl-text-secondary); margin-top: 2px; }

/* 介绍卡片 */
.intro-card {
  background: var(--rl-bg-card); border: 1px solid var(--rl-border-color); border-radius: 10px; padding: 28px;
}

.intro-header h3 { color: var(--rl-text-primary); font-size: 16px; margin: 0 0 8px; font-family: 'Share Tech Mono', monospace; letter-spacing: 0.05em; }
.intro-header p  { color: var(--rl-text-secondary); font-size: 13px; margin: 0; }

.engine-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 24px; }

.engine-item {
  background: var(--rl-bg-primary); border: 1px solid var(--rl-border-color); border-radius: 8px; padding: 18px;
  transition: border-color 0.15s;
}
.engine-item:hover { border-color: var(--rl-accent); }

.engine-badge {
  display: inline-block; font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 4px; margin-bottom: 10px;
}
.engine-badge.canal { background: rgba(6, 182, 212, 0.12); color: var(--rl-accent); }
.engine-badge.flink { background: rgba(168, 85, 247, 0.12); color: #8b5cf6; }
.engine-badge.datax { background: rgba(245, 158, 11, 0.12); color: #d97706; }

.engine-item p { color: var(--rl-text-secondary); font-size: 13px; margin: 0; line-height: 1.6; }
</style>
