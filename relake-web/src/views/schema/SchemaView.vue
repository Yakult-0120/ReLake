<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSchemaTables, getTableSchema, type SchemaTable, type SchemaColumn } from '@/api/schema'
import { getDatasourceList, type Datasource } from '@/api/datasource'

const datasources = ref<Datasource[]>([])
const selectedDsId = ref<number | null>(null)
const tables = ref<SchemaTable[]>([])
const loading = ref(false)
const expaandedTable = ref<string | null>(null)
const columns = ref<SchemaColumn[]>([])
const columnsLoading = ref(false)

async function loadDatasources() {
  try {
    const res = await getDatasourceList()
    datasources.value = (res as any).data?.data || []
  } catch {
    // handled
  }
}

async function loadTables() {
  if (!selectedDsId.value) return
  loading.value = true
  try {
    const res = await getSchemaTables(selectedDsId.value)
    tables.value = (res as any).data?.data || []
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function handleTableExpand(row: SchemaTable) {
  if (expaandedTable.value === row.tableName) {
    expaandedTable.value = null
    return
  }
  expaandedTable.value = row.tableName
  columnsLoading.value = true
  try {
    // getTableSchema 已将 ColumnInfo[] 映射为 R<SchemaColumn[]>
    const columnsData = await getTableSchema(selectedDsId.value!, row.tableName)
    columns.value = (columnsData as any).data || []
  } catch {
    columns.value = []
  } finally {
    columnsLoading.value = false
  }
}

onMounted(() => {
  loadDatasources()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">Schema 浏览</h2>

    <el-card>
      <div class="toolbar">
        <el-select
          v-model="selectedDsId"
          placeholder="选择数据源"
          style="width: 280px"
          @change="loadTables"
        >
          <el-option
            v-for="ds in datasources"
            :key="ds.id"
            :label="`${ds.name} (${ds.dbType})`"
            :value="ds.id"
          />
        </el-select>
        <span v-if="tables.length" class="table-count">
          共 {{ tables.length }} 张表
        </span>
      </div>

      <el-empty v-if="!selectedDsId" description="请先选择一个数据源" />
      <el-empty v-else-if="!loading && !tables.length" description="未发现表" />

      <el-table v-if="selectedDsId && tables.length" :data="tables" v-loading="loading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table v-if="expaandedTable === row.tableName" :data="columns" v-loading="columnsLoading" border size="small" style="margin:8px 0">
              <el-table-column prop="columnName" label="列名" width="180" />
              <el-table-column prop="dataType" label="数据类型" width="160" />
              <el-table-column prop="columnSize" label="长度" width="80" />
              <el-table-column prop="nullable" label="可为空" width="80">
                <template #default="{ row: col }">
                  <el-tag :type="col.nullable ? 'success' : 'danger'" size="small">
                    {{ col.nullable ? 'YES' : 'NO' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="isPrimaryKey" label="主键" width="80">
                <template #default="{ row: col }">
                  <el-tag v-if="col.isPrimaryKey" type="warning" size="small">PK</el-tag>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="columnComment" label="注释" min-width="200" show-overflow-tooltip />
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="tableName" label="表名" width="260" />
        <el-table-column prop="tableComment" label="注释" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              size="small"
              link
              type="primary"
              @click="handleTableExpand(row)"
            >
              {{ expaandedTable === row.tableName ? '收起' : '查看列' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
}

.table-count {
  color: var(--rl-text-secondary);
  font-size: 13px;
  margin-left: 12px;
}

.text-muted {
  color: var(--rl-text-placeholder);
}
</style>
