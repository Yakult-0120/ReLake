import request, { type R } from './request'
import type { PageResult } from './types'

export interface Task {
  id?: string | number
  name: string
  datasourceId: string | number
  targetId: string | number
  engineType: string
  sourceTables: string
  status?: string
  configJson?: string
  cronExpr?: string
  errorMessage?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export interface Metrics {
  recordsIn: number
  recordsOut: number
  bytesIn: number
  bytesOut: number
  errorCount: number
  latencyMs: number
}

export function getTasks(params: { page: number; size: number; keyword?: string }) {
  return request.get<any, R<PageResult<Task>>>('/api/v1/tasks', { params })
}

export function getTaskList() {
  return request.get<any, R<Task[]>>('/api/v1/tasks/list')
}

export function getTask(id: string | number) {
  return request.get<any, R<Task>>(`/api/v1/tasks/${id}`)
}

export function createTask(data: Task) {
  return request.post<any, R<Task>>('/api/v1/tasks', data)
}

export function updateTask(data: Task) {
  return request.put<any, R<Task>>(`/api/v1/tasks/${data.id}`, data)
}

export function deleteTask(id: string | number) {
  return request.delete(`/api/v1/tasks/${id}`)
}

export function validateTask(id: string | number) {
  return request.post(`/api/v1/tasks/${id}/validate`)
}

export function startTask(id: string | number) {
  return request.post(`/api/v1/tasks/${id}/start`)
}

export function stopTask(id: string | number) {
  return request.post(`/api/v1/tasks/${id}/stop`)
}

export function getTaskStatus(id: string | number) {
  return request.get<any, R<{ status: string }>>(`/api/v1/tasks/${id}/status`)
}

export function getTaskMetrics(id: string | number) {
  return request.get<any, R<Metrics>>(`/api/v1/tasks/${id}/metrics`)
}

export function getTaskLog(id: string | number) {
  return request.get<any, R<string>>(`/api/v1/tasks/${id}/log`)
}
