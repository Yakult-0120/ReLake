import request, { type R } from './request'

export interface SchemaTable {
  tableName: string
  tableComment?: string
}

export interface SchemaColumn {
  columnName: string
  dataType: string
  columnSize: number
  nullable: boolean
  isPrimaryKey: boolean
  columnComment: string
}

/** 后端 ColumnInfo 原始字段 */
interface RawColumn {
  columnName: string
  columnType: string
  dataType: number
  columnSize: number
  nullable: boolean
  primaryKey: boolean
  comment: string
}

function mapColumn(raw: RawColumn): SchemaColumn {
  return {
    columnName: raw.columnName,
    dataType: raw.columnType,
    columnSize: raw.columnSize ?? 0,
    nullable: raw.nullable,
    isPrimaryKey: raw.primaryKey,
    columnComment: raw.comment ?? '',
  }
}

/**
 * 获取数据源下的所有表
 * 后端 GET /api/v1/schemas/{datasourceId}/tables → R<List<String>>
 */
export function getSchemaTables(datasourceId: number) {
  return request.get<any, R<SchemaTable[]>>(
    `/api/v1/schemas/${datasourceId}/tables`,
  ).then(res => {
    const raw = (res as any).data?.data
    if (Array.isArray(raw)) {
      const mapped: SchemaTable[] = raw.map((name: string) => ({ tableName: name }))
      return { ...res, data: { ...res.data, data: mapped } } as typeof res
    }
    return res
  })
}

/**
 * 获取指定表的列信息
 * 后端 GET /api/v1/schemas/{datasourceId}/tables/{tableName} → R<TableInfo>
 */
export async function getTableSchema(
  datasourceId: number,
  tableName: string,
): Promise<R<SchemaColumn[]>> {
  const res = await request.get<any, R<any>>(
    `/api/v1/schemas/${datasourceId}/tables/${tableName}`,
  )
  const raw = (res as any).data?.data
  const columns: SchemaColumn[] = raw?.columns
    ? raw.columns.map(mapColumn)
    : []
  return {
    code: 0,
    message: '',
    data: columns as any,
    timestamp: 0,
  } as R<SchemaColumn[]>
}
