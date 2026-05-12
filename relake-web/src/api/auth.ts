import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  username: string
  displayName?: string
}

export interface LoginResponse {
  code: number
  message: string
  data: LoginResult
  timestamp: number
}

export function loginApi(params: LoginParams) {
  return request.post<any, any>('/api/v1/auth/login', params)
}
