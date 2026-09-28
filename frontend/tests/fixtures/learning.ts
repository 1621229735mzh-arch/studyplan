/**
 * 无敏感信息的测试数据：科目、单位与学习记录。
 *
 * 学习记录字段与后端 StudyRecordView 一致：用时是 durationMinutes（不是 minutes）。
 */
import type { StudyUnit, Subject } from '@/types/domain'
import type { LearningRecord, LearningRecordFormModel } from '@/features/learning/types'

export const subjectsFixture: Subject[] = [
  { id: 1, name: '数据结构', category: '专业课', color: '#2f6feb', sortOrder: 1, enabled: true },
  { id: 2, name: '操作系统', category: '专业课', color: '#e6a23c', sortOrder: 2, enabled: true }
]

export const unitsFixture: StudyUnit[] = [
  { id: 1, name: '题', sortOrder: 1, enabled: true },
  { id: 2, name: '页', sortOrder: 2, enabled: true },
  { id: 3, name: '讲', sortOrder: 3, enabled: true }
]

export const learningRecordFixture: LearningRecord = {
  id: 901,
  taskId: 11,
  taskTitle: '操作系统第 1—3 讲',
  subjectId: 2,
  subjectName: '操作系统',
  unitId: 3,
  unitName: '讲',
  amount: 2,
  durationMinutes: 90,
  content: null,
  note: '进程调度部分需要再看一遍',
  recordDate: '2025-03-10',
  version: 2
}

/** 合法表单：关联任务 + 完成量 + 用时。 */
export const validRecordFormFixture: LearningRecordFormModel = {
  recordDate: '2025-03-10',
  taskId: 11,
  subjectId: 2,
  unitId: 3,
  amount: 2,
  durationMinutes: 90,
  note: '进程调度部分需要再看一遍'
}

/** 只记时长、不关联任务的计划外学习。 */
export const timeOnlyRecordFormFixture: LearningRecordFormModel = {
  recordDate: '2025-03-10',
  taskId: null,
  subjectId: null,
  unitId: null,
  amount: null,
  durationMinutes: 35,
  note: ''
}

/** 非法表单：既没有用时，也填了无单位的数量。 */
export const invalidRecordFormFixture: LearningRecordFormModel = {
  recordDate: '2025-03-10',
  taskId: null,
  subjectId: null,
  unitId: null,
  amount: 5,
  durationMinutes: null,
  note: ''
}
