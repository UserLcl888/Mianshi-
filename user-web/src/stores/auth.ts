import { defineStore } from 'pinia'
import { loginApi, loginByCodeApi, registerApi, logoutApi, getProfileApi, type RegisterForm } from '@/api/auth'
import type { UserInfo } from '@/types'

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: readStoredUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    async login(account: string, password: string) {
      const res = await loginApi(account, password)
      this.token = res.token
      this.userInfo = res.userInfo
      localStorage.setItem('token', res.token)
      localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    },
    async loginByCode(email: string, code: string) {
      const res = await loginByCodeApi(email, code)
      this.token = res.token
      this.userInfo = res.userInfo
      localStorage.setItem('token', res.token)
      localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    },
    async register(form: RegisterForm) {
      await registerApi(form)
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.clearAuth()
      }
    },
    async fetchProfile() {
      const info = await getProfileApi()
      this.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    clearAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
