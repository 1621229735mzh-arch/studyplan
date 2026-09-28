/**
 * 学习记录数据类型（对应 backend 的 com.kaoyan.study.learning.dto）。
 *
 * 学习记录决定实际完成量（创建计划不增加完成量）；修改/删除记录后统计由后端重算。
 * 记录支持 3—5 分钟快速填写，因此除日期、完成量、用时外的字段都允许留空。
 */

/** 学习记录（对应后端 StudyRecordView）。 */
export interface LearningRecord {
  id: number
  /** 关联任务；允许计划外学习（不关联任务） */
  taskId?: number | null
  taskTitle?: string | null
  subjectId?: number | null
  subjectName?: string | null
  unitId?: number | null
  unitName?: string | null
  recordDate: string
  /** 完成量；后端字段为 amount */
  amount?: number | null
  /** 用时（分钟）；后端字段名为 durationMinutes（不是 minutes） */
  durationMinutes?: number | null
  /** 学习内容 */
  content?: string | null
  /** 复盘 */
  note?: string | null
  version: number
}

/** 提交学习记录（后端 StudyRecordCreateRequest）。 */
export interface LearningRecordCreateRequest {
  taskId?: number | null
  subjectId?: number | null
  unitId?: number | null
  recordDate: string
  amount?: number | null
  /** 用时（分钟） */
  durationMinutes?: number | null
  content?: string | null
  note?: string | null
  /**
   * 防重复提交用的幂等标识（后端字段名为 clientToken，不是 clientRequestId）；
   * 每次填写生成一次，重试时复用。
   */
  clientToken?: string | null
}

/** 修改学习记录（后端 StudyRecordUpdateRequest）；version 必填。 */
export interface LearningRecordUpdateRequest {
  taskId?: number | null
  subjectId?: number | null
  unitId?: number | null
  recordDate: string
  amount?: number | null
  durationMinutes?: number | null
  content?: string | null
  note?: string | null
  version: number
}

/** 记录列表查询条件。 */
export interface LearningRecordQuery {
  from?: string
  to?: string
  taskId?: number
  subjectId?: number
}

/** 记录表单的本地校验字段（表单字段名与请求字段保持一致）。 */
export interface LearningRecordFormModel {
  recordDate: string
  taskId: number | null
  subjectId: number | null
  unitId: number | null
  amount: number | null
  /** 用时（分钟） */
  durationMinutes: number | null
  note: string
}

/** 表单里的任务选项（只取快速记录需要的字段，避免依赖计划模块的完整类型）。 */
export interface LearningRecordTaskOption {
  id: number
  title: string
  subjectId?: number | null
  subjectName?: string | null
  unitId?: number | null
  unitName?: string | null
}

/** 表单校验结果。 */
export interface LearningRecordValidationResult {
  valid: boolean
  /** 字段级错误，直接绑定到 el-form-item 的 error */
  errors: Partial<Record<keyof LearningRecordFormModel, string>>
  /** 表单级提示 */
  summary: string | null
}

/** 表单提交事件载荷：由调用方补上 version（修改时）。 */
export interface LearningRecordFormSubmission {
  model: LearningRecordFormModel
  /** 幂等标识（对应后端 clientToken）：一次填写生成一次，失败重试复用，避免重复记录 */
  clientToken: string
}
