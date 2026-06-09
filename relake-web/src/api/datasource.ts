import request, { type R } from './request'
import type { PageResult } from './types'

export interface Datasource {
  id?: string | number
  name: string
  dbType: string
  host: string
  port: number
  dbName: string
  username: string
  password?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export function getDatasources(params: { page: number; size: number; keyword?: string }) {
  return request.get<any, R<PageResult<Datasource>>>('/api/v1/datasources', { params })
}

export function getDatasourceList() {
  return request.get<any, R<Datasource[]>>('/api/v1/datasources/list')
}

export function getDatasource(id: string | number) {
  return request.get<any, R<Datasource>>(`/api/v1/datasources/${id}`)
}

export function createDatasource(data: Datasource) {
  return request.post<any, R<Datasource>>('/api/v1/datasources', data)
}

export function updateDatasource(data: Datasource) {
  return request.put<any, R<Datasource>>(`/api/v1/datasources/${data.id}`, data)
}

export function deleteDatasource(id: string | number) {
  return request.delete(`/api/v1/datasources/${id}`)
}

export function testDatasource(id: string | number) {
  return request.post<any, R<boolean>>(`/api/v1/datasources/${id}/test`)
}
