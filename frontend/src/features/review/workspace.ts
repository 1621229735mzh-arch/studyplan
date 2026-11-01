import type { MasteryLevel } from '@/types/domain'

export interface ReviewTotals {
  itemCount: number; reviewedItems: number; completedReviews: number; reviewDays: number; recordCount: number
}
export interface ReviewEntry {
  streamId?: number | null; bookTitle?: string | null; learnedOn?: string | null; round?: number
  reviewItemId: number; subjectId: number; title: string; date: string
  completionPercent: number; result: MasteryLevel | null; scheduled: boolean; version: number
}
export interface ReviewTrace {
  id: number; reviewItemId: number; taskId: number | null; subjectId: number; subjectName: string
  title: string; reviewDate: string; progressDelta: number; result: MasteryLevel | null
  durationMinutes: number | null; note: string | null
}
export interface ReviewWorkspace {
  months: { month: string; scheduledDays: number; entryCount: number; completedCount: number }[]
  entries: ReviewEntry[]; totals: ReviewTotals
}
export interface ReviewProgress { direction: string; subjectId: number; subjectName: string; totals: ReviewTotals }
