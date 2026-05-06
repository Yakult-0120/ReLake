import request, { type R } from './request'

export interface SchemaTable {
  tableName: string
  tableComment?: string
  columns?: SchemaColumn[]
}

export interface SchemaColumn {
  columnName: string
  dataType: string
  columnSize?: number
  nullable?: boolean
  columnComment?: string
  isPrimaryKey?: boolean
}

export function getSchemaTables(datasourceId: number) {
  return request.get<any, R<SchemaTable[]>>('/api/v1/schemas', {
    params: { datasourceId },
  })
}

export function getTableSchema(datasourceId: number, tableName: string) {
  return request.get<any, R<SchemaTable>>(
    `/api/v1/schemas/${datasourceId}/tables/${tableName}`,
  )
}
