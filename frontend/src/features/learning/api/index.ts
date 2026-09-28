/**
 * 学习记录接口。
 *
 * 记录决定实际完成量；修改与删除后由后端重算统计。
 * 修改必须回传 version，后端检测到数据已变化时返回冲突错误（前端提示刷新）。
 * 创建时附带 clientToken 以配合后端防重；删除也必须带上 version。
 */
import { asList, cleanParams, del, get, post, put } from '@/api/http'
import type { PageResult } from '@/types/api'
import type {
  LearningRecord,
  LearningRecordCreateRequest,
  LearningRecordQuery,
  LearningRecordUpdateRequest
} from '../types'

/** 记录列表（支持日期区间、任务、科目筛选）。 */
export async function listLearningRecords(query: LearningRecordQuery = {}): Promise<LearningRecord[]> {
  const response = await get<LearningRecord[] | PageResult<LearningRecord>>('/learning/records', {
    params: cleanParams({ ...query })
  })
  return asList(response)
}

/** 新增学习记录（带 clientToken 时重复提交不会生成第二条）。 */
export function createLearningRecord(payload: LearningRecordCreateRequest): Promise<LearningRecord> {
  return post<LearningRecord>('/learning/records', payload)
}

/** 修改学习记录；payload 必须包含 version。 */
export function updateLearningRecord(id: number, payload: LearningRecordUpdateRequest): Promise<LearningRecord> {
  return put<LearningRecord>(`/learning/records/${id}`, payload)
}

/** 删除学习记录（软删除，统计随之重算）；后端要求携带 version。 */
export function deleteLearningRecord(id: number, version: number): Promise<void> {
  return del<void>(`/learning/records/${id}`, { params: { version } })
}
