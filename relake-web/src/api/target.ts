import request, { type R } from './request'

export interface Target {
  id?: string | number
  name: string
  storageType: string
  endpoint: string
  bucket: string
  accessKey: string
  secretKey?: string
  region?: string
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

export function getTargets(params: { page: number; size: number; keyword?: string }) {
  return request.get<any, R<PageResult<Target>>>('/api/v1/targets', { params })
}

export function getTargetList() {
  return request.get<any, R<Target[]>>('/api/v1/targets/list')
}

export function getTarget(id: string | number) {
  return request.get<any, R<Target>>(`/api/v1/targets/${id}`)
}

export function createTarget(data: Target) {
  return request.post<any, R<Target>>('/api/v1/targets', data)
}

export function updateTarget(data: Target) {
  return request.put<any, R<Target>>('/api/v1/targets', data)
}

export function deleteTarget(id: string | number) {
  return request.delete(`/api/v1/targets/${id}`)
}

export function testTarget(id: string | number) {
  return request.post<any, R<boolean>>(`/api/v1/targets/${id}/test`)
}
