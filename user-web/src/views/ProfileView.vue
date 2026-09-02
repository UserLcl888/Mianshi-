<template>
  <div class="page">
    <AppHeader />
    <div class="page-body">
      <main class="content">
        <el-breadcrumb class="breadcrumb-bar" separator="/">
          <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>个人中心</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="app-card profile-card">
          <h3 class="section-title">个人信息</h3>

          <div class="user-head">
            <div class="avatar-wrap" @click="pickAvatar">
              <div class="avatar">
                <img v-if="auth.userInfo?.avatar" :src="auth.userInfo.avatar" alt="头像" />
                <span v-else>{{ avatarText }}</span>
              </div>
              <div class="avatar-mask">
                <span class="avatar-cam">📷</span>
                <span>更换头像</span>
              </div>
            </div>
            <div class="user-meta">
              <div class="user-name">{{ auth.userInfo?.nickname || '未设置昵称' }}</div>
              <span class="role-badge">{{ auth.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}</span>
            </div>
          </div>
          <input
            ref="avatarFileInput"
            type="file"
            accept="image/png,image/jpeg"
            class="hidden-file"
            @change="onAvatarChange"
          />

          <div class="info-list">
            <div class="info-row">
              <span class="info-label">昵称</span>
              <span class="info-value">{{ auth.userInfo?.nickname || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ auth.userInfo?.email || '未绑定' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ auth.userInfo?.phone || '未绑定' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ auth.userInfo?.createdAt }}</span>
            </div>
          </div>

          <div class="actions">
            <button type="button" class="btn-main" @click="openNickDialog">
              <el-icon :size="16"><EditPen /></el-icon>
              <span>修改昵称</span>
            </button>
            <router-link to="/profile/applies" class="btn-ghost">
              <el-icon :size="16"><Tickets /></el-icon>
              <span>我的申请</span>
            </router-link>
            <router-link to="/profile/password" class="btn-subtle">
              <el-icon :size="16"><Lock /></el-icon>
              <span>修改密码</span>
            </router-link>
          </div>
        </div>

        <el-dialog v-model="avatarDialogVisible" title="更换头像" width="420px" append-to-body>
          <div class="avatar-dialog-body">
            <img v-if="pendingAvatar" :src="pendingAvatar" class="avatar-dialog-img" alt="头像预览" />
            <div class="avatar-dialog-tip">仅支持 png / jpg，最大 10MB</div>
          </div>
          <template #footer>
            <el-button @click="cancelAvatar">取消</el-button>
            <el-button type="primary" :loading="avatarUploading" @click="confirmAvatar">保存</el-button>
          </template>
        </el-dialog>

        <el-dialog v-model="nickDialogVisible" title="修改昵称" width="420px" append-to-body>
          <el-form ref="nickFormRef" :model="nickForm" :rules="nickRules" label-position="top">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="nickForm.nickname" placeholder="请输入昵称" maxlength="50" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="nickDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="nickSaving" @click="saveNickname">保存</el-button>
          </template>
        </el-dialog>
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { EditPen, Lock, Tickets } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { useAuthStore } from '@/stores/auth'
import { updateNicknameApi, updateAvatarApi } from '@/api/auth'
import { dataUrlToFile } from '@/utils/file'

const auth = useAuthStore()
const avatarText = computed(() => (auth.userInfo?.nickname || 'U').trim().charAt(0).toUpperCase())
const avatarFileInput = ref<HTMLInputElement | null>(null)
const avatarUploading = ref(false)
const avatarDialogVisible = ref(false)
const pendingAvatar = ref('')
const nickDialogVisible = ref(false)
const nickSaving = ref(false)
const nickFormRef = ref<FormInstance>()
const nickForm = reactive({ nickname: '' })
const nickRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

onMounted(() => {
  if (!auth.userInfo) auth.fetchProfile()
})

function openNickDialog() {
  nickForm.nickname = auth.userInfo?.nickname || ''
  nickDialogVisible.value = true
}

function pickAvatar() {
  avatarFileInput.value?.click()
}

async function onAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const ok = ['image/png', 'image/jpeg'].includes(file.type)
  if (!ok) {
    ElMessage.warning('仅支持 png/jpg 格式')
    input.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 10MB')
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    pendingAvatar.value = String(reader.result || '')
    avatarDialogVisible.value = true
  }
  reader.readAsDataURL(file)
  input.value = ''
}

async function confirmAvatar() {
  if (!pendingAvatar.value) return
  avatarUploading.value = true
  try {
    const ext = pendingAvatar.value.startsWith('data:image/jpeg') ? 'jpg' : 'png'
    await updateAvatarApi(dataUrlToFile(pendingAvatar.value, `avatar.${ext}`))
    await auth.fetchProfile()
    ElMessage.success('头像更新成功')
    avatarDialogVisible.value = false
    pendingAvatar.value = ''
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    avatarUploading.value = false
  }
}

function cancelAvatar() {
  avatarDialogVisible.value = false
  pendingAvatar.value = ''
}

async function saveNickname() {
  const valid = await nickFormRef.value?.validate().catch(() => false)
  if (!valid) return
  nickSaving.value = true
  try {
    await updateNicknameApi(nickForm.nickname.trim())
    await auth.fetchProfile()
    ElMessage.success('昵称修改成功')
    nickDialogVisible.value = false
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    nickSaving.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-body {
  flex: 1;
  width: 100%;
  padding: 16px 24px;
  display: flex;
  justify-content: center;
}

.content {
  width: 100%;
  max-width: 720px;
}

.section-title {
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e9b862;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: linear-gradient(180deg, #f2a82e, #e89a1f);
  box-shadow: 0 0 8px rgba(232, 154, 31, 0.5);
}

.profile-card {
  padding: 28px 32px;
  box-shadow: 0 8px 30px rgba(217, 167, 22, 0.1);
}

.user-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--app-border);
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid var(--app-accent);
  background: linear-gradient(135deg, rgba(232, 154, 31, 0.4), rgba(34, 211, 238, 0.25));
  color: #ffffff;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-wrap {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #ffffff;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.15s;
}

.avatar-wrap:hover .avatar-mask {
  opacity: 1;
}

.avatar-cam {
  font-size: 16px;
}

.hidden-file {
  display: none;
}

.avatar-dialog-body {
  text-align: center;
  padding: 4px 0 8px;
}

.avatar-dialog-img {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--app-accent);
}

.avatar-dialog-tip {
  margin-top: 12px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.user-meta {
  min-width: 0;
}

.user-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--app-text);
}

.role-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--app-accent);
  background: var(--app-accent-soft);
  border: 1px solid rgba(232, 154, 31, 0.35);
}

.info-list {
  margin-top: 4px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 2px;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.info-value {
  color: var(--app-text);
  font-weight: 500;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.btn-main,
.btn-ghost,
.btn-subtle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
  transition: all 0.15s;
  color: var(--app-text);
}

.btn-main {
  background: linear-gradient(135deg, #f7bf4d, #f0a82c);
  color: #141a26;
  box-shadow: 0 4px 14px rgba(232, 154, 31, 0.3);
}

.btn-main:hover {
  filter: brightness(1.05);
}

.btn-ghost {
  background: var(--app-accent-soft);
  border-color: rgba(232, 154, 31, 0.4);
  color: var(--app-accent);
}

.btn-ghost:hover {
  background: rgba(232, 154, 31, 0.18);
  border-color: var(--app-accent);
}

.btn-subtle {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.btn-subtle:hover {
  border-color: var(--app-accent);
  color: var(--app-accent);
}
</style>
