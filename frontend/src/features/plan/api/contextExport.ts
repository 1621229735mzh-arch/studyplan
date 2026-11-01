import { get } from '@/api/http'

export interface PlanningContextSnapshot {
  schemaVersion: number
  documentType: string
  exportedAt: string
  dataVersion: string
  studyDate: string
  recentFrom: string
  recentTo: string
  rules: string[]
  facts: {
    tasks: unknown[]
    progress: { courses: unknown[] }
    studyRecords: unknown[]
    reviewRecords: unknown[]
    openMemos: unknown[]
    [key: string]: unknown
  }
  feedback: {
    studyRecordCount: number
    learningDays: number
    reviewRecordCount: number
    completedReviews: number
    weaknesses: { reviewItemId: number; title: string; result: string; feedbackDate: string }[]
    [key: string]: unknown
  }
}
export interface PlanningContextExport { snapshot: PlanningContextSnapshot; markdown: string }
export const getPlanningContextExport = () => get<PlanningContextExport>('/plan/context-export')
