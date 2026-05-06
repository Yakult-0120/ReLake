import request, { type R } from './request'

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

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
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
  return request.put<any, R<Datasource>>('/api/v1/datasources', data)
}

export function deleteDatasource(id: string | number) {
  return request.delete(`/api/v1/datasources/${id}`)
}

export function testDatasource(id: string | number) {
  return request.post<any, R<boolean>>(`/api/v1/datasources/${id}/test`)
}
