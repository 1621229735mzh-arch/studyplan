/**
 * 设置模块接口（对应 backend 的 settings 三个 Controller）。
 */
import { del, get, post, put } from '@/api/http'
import type { StudyUnit, Subject } from '@/types/domain'
import type { StudySettings, StudySettingsRequest, StudyUnitRequest, SubjectRequest } from '../types'

/** 科目列表。 */
export function listSubjects(enabledOnly = false): Promise<Subject[]> {
  return get<Subject[]>('/settings/subjects', { params: { enabledOnly } })
}

/** 新增科目。 */
export function createSubject(payload: SubjectRequest): Promise<Subject> {
  return post<Subject>('/settings/subjects', payload)
}

/** 修改科目。 */
export function updateSubject(id: number, payload: SubjectRequest): Promise<Subject> {
  return put<Subject>(`/settings/subjects/${id}`, payload)
}

/** 删除科目；被任务引用时后端会拒绝并返回错误。 */
export function deleteSubject(id: number): Promise<void> {
  return del<void>(`/settings/subjects/${id}`)
}

/** 计量单位列表。 */
export function listUnits(enabledOnly = false): Promise<StudyUnit[]> {
  return get<StudyUnit[]>('/settings/units', { params: { enabledOnly } })
}

/** 新增单位。 */
export function createUnit(payload: StudyUnitRequest): Promise<StudyUnit> {
  return post<StudyUnit>('/settings/units', payload)
}

/** 修改单位。 */
export function updateUnit(id: number, payload: StudyUnitRequest): Promise<StudyUnit> {
  return put<StudyUnit>(`/settings/units/${id}`, payload)
}

/** 删除单位；被引用时后端会拒绝。 */
export function deleteUnit(id: number): Promise<void> {
  return del<void>(`/settings/units/${id}`)
}

/** 读取学习设置（考试时间、学习预算、复习额度、复习阶段）。 */
export function getStudySettings(): Promise<StudySettings> {
  return get<StudySettings>('/settings/study')
}

/** 更新学习设置；必须携带当前 version。 */
export function updateStudySettings(payload: StudySettingsRequest): Promise<StudySettings> {
  return put<StudySettings>('/settings/study', payload)
}
