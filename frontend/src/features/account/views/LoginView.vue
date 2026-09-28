<script setup lang="ts">
/**
 * 登录页。
 *
 * 只有预设账号，不开放注册；登录失败时保留已输入内容并展示后端返回的中文提示。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElAlert, ElButton, ElForm, ElFormItem, ElInput } from 'element-plus'

import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const form = reactive({ username: '', password: '' })
const fieldErrors = ref<{ username?: string; password?: string }>({})

function validate(): boolean {
  const errors: { username?: string; password?: string } = {}
  if (form.username.trim() === '') {
    errors.username = '请输入登录名'
  }
  if (form.password === '') {
    errors.password = '请输入口令'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function handleSubmit(): Promise<void> {
  if (!validate()) {
    return
  }
  const ok = await session.login({ username: form.username.trim(), password: form.password })
  if (!ok) {
    // 保留 form 中的输入，仅展示 session.errorMessage（后端提示）
    return
  }
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
  if (redirect && redirect !== '/login') {
    await router.replace(redirect)
    return
  }
  await router.replace({ name: 'today' })
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__panel">
      <h1 class="login-page__title">考研学习工作台</h1>
      <p class="login-page__subtitle">预设账号登录，不开放注册。登录后学习数据在电脑与手机之间同步。</p>

      <ElAlert
        v-if="session.errorMessage"
        class="login-page__error"
        type="error"
        show-icon
        :closable="false"
        :title="session.errorMessage"
      />

      <ElForm label-position="top" @submit.prevent>
        <ElFormItem label="登录名" :error="fieldErrors.username">
          <ElInput v-model="form.username" placeholder="登录名" autocomplete="username" />
        </ElFormItem>
        <ElFormItem label="口令" :error="fieldErrors.password">
          <ElInput
            v-model="form.password"
            type="password"
            show-password
            placeholder="口令"
            autocomplete="current-password"
            @keyup.enter="handleSubmit"
          />
        </ElFormItem>
        <ElButton class="login-page__submit" type="primary" :loading="session.loading" @click="handleSubmit">
          登录
        </ElButton>
      </ElForm>

      <p class="hint-text">
        离线时无法登录；但已在此设备登录并同步过的数据，可以在断网后只读查看今日、本周与进度概览。
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--kaoyan-bg);
}

.login-page__panel {
  width: min(420px, 100%);
  padding: 28px;
  background: #fff;
  border: 1px solid var(--kaoyan-border);
  border-radius: var(--kaoyan-card-radius);
}

.login-page__title {
  margin: 0;
  font-size: 20px;
}

.login-page__subtitle {
  margin: 6px 0 20px;
  color: var(--kaoyan-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.login-page__error {
  margin-bottom: 16px;
}

.login-page__submit {
  width: 100%;
}
</style>
