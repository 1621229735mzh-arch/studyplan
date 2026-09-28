/**
 * 无敏感信息的测试数据：今日页面。
 *
 * 结构与后端 TodayResponse 一致：plannedTasks 是 DailyPlanItemView、
 * records 是 StudyRecordView、reviewTasks 是 ReviewItemResponse，
 * 没有 unrecordedItems / completions / mastery。
 */
import type { TodayResponse } from '@/features/today/types'

export const todayFixture: TodayResponse = {
  date: '2025-03-10',
  plannedTasks: [
    {
      id: 501,
      taskId: 11,
      taskTitle: '操作系统第 1—3 讲',
      subjectId: 2,
      unitId: 3,
      unitName: '讲',
      plannedAmount: 3,
      source: 'MANUAL',
      reviewItemId: null,
      version: 1,
      completedAmount: 2
    },
    {
      id: 502,
      taskId: 12,
      taskTitle: '数据结构线性表习题',
      subjectId: 1,
      unitId: 1,
      unitName: '题',
      plannedAmount: 20,
      source: 'WEEKLY',
      reviewItemId: null,
      version: 1,
      completedAmount: 0
    }
  ],
  records: [
    {
      id: 901,
      taskId: 11,
      taskTitle: '操作系统第 1—3 讲',
      subjectId: 2,
      subjectName: '操作系统',
      unitId: 3,
      unitName: '讲',
      recordDate: '2025-03-10',
      amount: 2,
      durationMinutes: 90,
      content: null,
      note: '进程调度部分需要再看一遍',
      version: 2
    }
  ],
  reviewTasks: [
    {
      id: 301,
      subjectId: 2,
      taskId: null,
      title: '进程与线程的区别',
      status: 'SCHEDULED',
      nextReviewDate: '2025-03-12',
      lastResult: 'VAGUE',
      intervalDays: 2,
      estimatedMinutes: 15,
      scheduledDate: '2025-03-10',
      version: 1
    }
  ],
  // null 表示本人尚未设置每日学习预算（不是 0）
  dailyStudyMinutes: null,
  dailyReviewMinutes: 60,
  studyMinutes: 95,
  warnings: ['尚未设置每日学习预算，无法判断今日学习量是否在预算内']
}
