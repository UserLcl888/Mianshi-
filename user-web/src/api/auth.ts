import request from './request'
import type { LoginResult, UserInfo } from '@/types'

export interface RegisterForm {
  username?: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

export async function loginApi(account: string, password: string): Promise<LoginResult> {
  return request.post('/auth/login', { account, password })
}

export async function registerApi(form: RegisterForm): Promise<void> {
  return request.post('/auth/register', form)
}

export async function logoutApi(): Promise<void> {
  return request.post('/auth/logout')
}

export async function getProfileApi(): Promise<UserInfo> {
  return request.get('/user/profile')
}

export async function changePasswordApi(oldPassword: string, newPassword: string): Promise<void> {
  return request.put('/user/password', { oldPassword, newPassword })
}
