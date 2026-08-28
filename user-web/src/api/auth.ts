import request from './request'
import type { LoginResult, UserInfo } from '@/types'

export interface RegisterForm {
  email: string
  code: string
  nickname?: string
  password: string
}

export async function loginApi(account: string, password: string): Promise<LoginResult> {
  return request.post('/auth/login', { account: account.trim(), password })
}

export async function loginByCodeApi(email: string, code: string): Promise<LoginResult> {
  return request.post('/auth/login/code', { email: email.trim(), code: code.trim() })
}

export async function sendEmailCodeApi(email: string, scene: 'login' | 'register' | 'reset'): Promise<void> {
  return request.post('/auth/code/email', { email: email.trim(), scene })
}

export async function resetPasswordByCodeApi(email: string, code: string, newPassword: string): Promise<void> {
  return request.post('/auth/reset-password', { email: email.trim(), code: code.trim(), newPassword })
}

export async function registerApi(form: RegisterForm): Promise<void> {
  return request.post('/auth/register', {
    email: form.email.trim(),
    code: form.code.trim(),
    nickname: form.nickname?.trim() || undefined,
    password: form.password
  })
}

export async function logoutApi(): Promise<void> {
  return request.post('/auth/logout')
}

export async function getProfileApi(): Promise<UserInfo> {
  return request.get('/user/profile')
}

export async function updateNicknameApi(nickname: string): Promise<UserInfo> {
  return request.put('/user/profile', { nickname })
}

export async function changePasswordApi(oldPassword: string, newPassword: string): Promise<void> {
  return request.put('/user/password', { oldPassword, newPassword })
}
