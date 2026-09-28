/**
 * 学习记录表单的本地校验。
 *
 * 为什么自己写而不是只用 Element Plus 的异步校验：
 * 1) 记录表单的目标是 3—5 分钟填完，需要立即给出明确的中文提示；
 * 2) 纯函数便于单元测试，且离线判断、单位跟随任务的规则集中在一处；
 * 这里只做“能不能提交”的判断，数量是否合理（例如进度是否超额）由后端负责。
 */
import type { LearningRecordFormModel, LearningRecordValidationResult } from './types'

const MAX_NOTE_LENGTH = 500
const MAX_MINUTES_PER_RECORD = 24 * 60

function hasValue(value: number | null | undefined): value is number {
  return value !== null && value !== undefined && Number.isFinite(value)
}

/** 校验记录表单；返回字段级错误与表单级提示。 */
export function validateLearningRecordForm(model: LearningRecordFormModel): LearningRecordValidationResult {
  const errors: Partial<Record<keyof LearningRecordFormModel, string>> = {}

  if (!model.recordDate) {
    errors.recordDate = '请选择记录日期'
  }

  if (!hasValue(model.durationMinutes) || model.durationMinutes <= 0) {
    errors.durationMinutes = '请填写本次用时（分钟，需大于 0）'
  } else if (model.durationMinutes > MAX_MINUTES_PER_RECORD) {
    errors.durationMinutes = `单次用时不能超过 ${MAX_MINUTES_PER_RECORD} 分钟，请检查是否填错`
  }

  const amount = model.amount
  const taskId = model.taskId
  const unitId = model.unitId
  const subjectId = model.subjectId

  // 后端 StudyRecordCreateRequest 的规则：
  // - 关联任务时科目与单位以任务为准；
  // - 计划外且填了完成量时，必须同时给出科目与单位（否则无法计入任何单位的进度）；
  // - 不填完成量时只记时长，不再要求科目与单位。
  if (hasValue(amount) && amount <= 0) {
    errors.amount = '完成数量需大于 0'
  }
  if (hasValue(amount) && !hasValue(taskId) && (!hasValue(unitId) || !hasValue(subjectId))) {
    errors.taskId = '填写完成数量时，请关联任务，或选择科目与计量单位'
  }
  if (hasValue(taskId) && !hasValue(unitId)) {
    errors.taskId = '所选任务缺少计量单位，请先在“设置”中为该任务的单位补充配置'
  }
  if (model.note.length > MAX_NOTE_LENGTH) {
    errors.note = `复盘内容请控制在 ${MAX_NOTE_LENGTH} 字以内`
  }

  const valid = Object.keys(errors).length === 0
  return {
    valid,
    errors,
    summary: valid ? null : `请先补全学习记录：${Object.values(errors).join('；')}`
  }
}
