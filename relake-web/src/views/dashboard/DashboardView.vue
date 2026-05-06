<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

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
    const [dsRes, tRes, taskRes] = await Promise.all([
      request.get('/api/v1/datasources?page=1&size=1'),
      request.get('/api/v1/targets?page=1&size=1'),
      request.get('/api/v1/tasks?page=1&size=1'),
    ])
    stats.value.datasourceCount = dsRes.data?.data?.total || 0
    stats.value.targetCount = tRes.data?.data?.total || 0
    stats.value.taskCount = taskRes.data?.data?.total || 0
  } catch {
    // gracefully handle when backend is unavailable
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>

    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="数据源" :value="stats.datasourceCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="目标存储" :value="stats.targetCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="同步任务" :value="stats.taskCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="运行中" :value="stats.runningTaskCount">
            <template #suffix>
              <span style="font-size:14px;color:#67c23a">个</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>ReLake 实时数据湖平台</span>
      </template>
      <p style="color:#606266;line-height:2">
        ReLake 是一个实时数据湖平台，支持多引擎 CDC 数据采集（Canal / Flink CDC / DataX），
        将源数据库的变更数据实时同步到 Apache Paimon 湖仓一体存储中。
      </p>
      <el-divider />
      <el-row :gutter="16">
        <el-col :span="8">
          <h4 style="margin-bottom:8px">Canal 引擎</h4>
          <p style="color:#909399;font-size:13px">MySQL binlog 实时解析，增量采集，轻量级部署</p>
        </el-col>
        <el-col :span="8">
          <h4 style="margin-bottom:8px">Flink CDC 引擎</h4>
          <p style="color:#909399;font-size:13px">全量+增量一体化，多数据源支持，exactly-once 语义</p>
        </el-col>
        <el-col :span="8">
          <h4 style="margin-bottom:8px">DataX 引擎</h4>
          <p style="color:#909399;font-size:13px">离线批量同步，全量初始化，定时调度</p>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
}

.stat-card {
  text-align: center;
}
</style>
