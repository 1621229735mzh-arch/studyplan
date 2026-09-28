/**
 * 无敏感信息的测试数据：进度概览。
 *
 * 结构与后端 ProgressOverviewResponse 一致：from/to/items/trend，
 * 没有 subjects/asOf/totalStudyMinutes；plannedAmount 为 null 表示未设置计划量。
 */
import type { ProgressOverview } from '@/features/progress/types'

export const progressFixture: ProgressOverview = {
  from: '2025-02-09',
  to: '2025-03-10',
  items: [
    {
      subjectId: 1,
      subjectName: '数据结构',
      unitId: 1,
      unitName: '题',
      plannedAmount: 60,
      completedAmount: 42,
      studyMinutes: 150
    },
    {
      subjectId: 2,
      subjectName: '操作系统',
      unitId: 3,
      unitName: '讲',
      // 未设置计划量：null 不是 0
      plannedAmount: null,
      completedAmount: 5,
      studyMinutes: 170
    }
  ],
  trend: [
    {
      date: '2025-03-09',
      studyMinutes: 60,
      amounts: [{ unitId: 1, unitName: '题', amount: 12 }]
    },
    {
      date: '2025-03-10',
      studyMinutes: 35,
      amounts: [{ unitId: 3, unitName: '讲', amount: 2 }]
    }
  ]
}
