/**
 * 科目与计量单位缓存。
 *
 * 这两类数据被计划、学习记录、复习、备忘录、今日等多个功能共用，
 * 因此放在全局 store，只在这里调用 settings 模块的接口。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { toApiError } from '@/api/http'
import { listSubjects, listUnits } from '@/features/settings/api'
import type { ApiError } from '@/types/api'
import type { StudyUnit, Subject } from '@/types/domain'

export const useCatalogStore = defineStore('catalog', () => {
  const subjects = ref<Subject[]>([])
  const units = ref<StudyUnit[]>([])
  const loading = ref(false)
  const error = ref<ApiError | null>(null)

  let loaded = false

  /** 可选科目：默认只让选择启用中的科目。 */
  const enabledSubjects = computed(() => subjects.value.filter((subject) => subject.enabled))
  /** 可选单位。 */
  const enabledUnits = computed(() => units.value.filter((unit) => unit.enabled))

  /** 按 id 取科目名，用于表单回显（展示仍以后端返回的名称为准）。 */
  function subjectNameOf(id?: number | null): string | null {
    if (id === null || id === undefined) {
      return null
    }
    return subjects.value.find((subject) => subject.id === id)?.name ?? null
  }

  /** 按 id 取单位名。 */
  function unitNameOf(id?: number | null): string | null {
    if (id === null || id === undefined) {
      return null
    }
    return units.value.find((unit) => unit.id === id)?.name ?? null
  }

  /** 加载科目与单位；失败时记录错误但不抛断（表单可以提示先配置科目/单位）。 */
  async function load(force = false): Promise<void> {
    if (loaded && !force) {
      return
    }
    loading.value = true
    error.value = null
    try {
      const [subjectList, unitList] = await Promise.all([listSubjects(), listUnits()])
      subjects.value = subjectList
      units.value = unitList
      loaded = true
    } catch (caught) {
      error.value = toApiError(caught)
    } finally {
      loading.value = false
    }
  }

  /** 设置变更后强制刷新。 */
  async function reload(): Promise<void> {
    await load(true)
  }

  return {
    subjects,
    units,
    loading,
    error,
    enabledSubjects,
    enabledUnits,
    subjectNameOf,
    unitNameOf,
    load,
    reload
  }
})
