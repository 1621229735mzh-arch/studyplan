/**
 * 无敏感信息的测试数据：周计划。
 *
 * 结构与后端 WeeklyPlanResponse 一致：weekStartDate + items（WeeklyPlanItemView），
 * 没有 tasks/weekEnd/days。
 */
import type { WeekPlanResponse } from '@/features/plan/types'

export const weekFixture: WeekPlanResponse = {
  weekStartDate: '2025-03-10',
  note: '本周以操作系统为主',
  version: 4,
  items: [
    {
      taskId: 11,
      taskTitle: '操作系统第 1—3 讲',
      subjectId: 2,
      unitId: 3,
      unitName: '讲',
      plannedAmount: 6,
      completedAmount: 2
    }
  ]
}
