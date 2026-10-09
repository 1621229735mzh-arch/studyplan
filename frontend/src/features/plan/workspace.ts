/** 时间视图只组织服务端数据，不在浏览器计算学习统计。 */
export type PlanViewMode = 'overall' | 'month' | 'week' | 'day'
export interface ScheduleEntry {
  date: string; taskId: number; taskTitle: string; subjectId: number; unitId: number; unitName: string
  plannedAmount: number; completedAmount: number; source: string; estimatedMinutes?: number | null; sortOrder: number
}
export interface PlanAmount {
  subjectId: number; unitId: number; unitName: string; plannedAmount: number; completedAmount: number
}
export interface PlanMonth { month: string; scheduledDays: number; taskCount: number; amounts: PlanAmount[] }
export interface PlanOutline { periodType: 'MONTH' | 'WEEK'; startDate: string; title: string; description?: string }
export interface PlanWorkspace {
  revision: number; months: PlanMonth[]; entries: ScheduleEntry[]; outlines: PlanOutline[]
  imports: { planKey: string; title: string; startDate: string; endDate: string }[]
}
export interface ImportDocument {
  schemaVersion: number; planId: string; title: string; startDate: string; endDate: string
  goals: { key: string; subject: string; title: string; description?: string; startDate: string; endDate: string }[]
  months: { month: string; title: string; description?: string }[]
  weeks: { startDate: string; title: string; description?: string }[]
  tasks: { key: string; subject: string; unit: string; title: string; goalKey?: string; plannedAmount: number }[]
  days: { date: string; items: { taskKey: string; amount: number; estimatedMinutes?: number | null }[] }[]
}
export interface ImportPreview {
  previewToken: string; revision: number; document: ImportDocument
  taskCount: number; dayCount: number; itemCount: number; warnings: string[]
}
export interface QuickDayRequest {
  existingTaskId?: number; subjectId?: number; unitId?: number; title?: string
  plannedAmount: number; estimatedMinutes?: number | null; clientToken: string
}

const utcDate = (date: string) => new Date(`${date}T00:00:00Z`)
export function addDays(date: string, amount: number): string {
  const value = utcDate(date)
  value.setUTCDate(value.getUTCDate() + amount)
  return value.toISOString().slice(0, 10)
}
export function weekStart(date: string): string {
  return addDays(date, -(utcDate(date).getUTCDay() + 6) % 7)
}
export function weekDates(date: string): string[] {
  const start = weekStart(date)
  return Array.from({ length: 7 }, (_, i) => addDays(start, i))
}
export function moveMonth(date: string, amount: number): string {
  const value = utcDate(date.slice(0, 7) + '-01')
  value.setUTCMonth(value.getUTCMonth() + amount)
  return value.toISOString().slice(0, 10)
}
export function monthDates(date: string): string[] {
  const first = date.slice(0, 7) + '-01'
  const start = weekStart(first)
  const last = addDays(moveMonth(first, 1), -1)
  const days = Math.round((utcDate(last).getTime() - utcDate(start).getTime()) / 86400000) + 1
  return Array.from({ length: Math.ceil(days / 7) * 7 }, (_, i) => addDays(start, i))
}
export function dateLabel(date: string): string {
  return new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short', timeZone: 'UTC' }).format(utcDate(date))
}
export function downloadText(name: string, text: string, type = 'text/plain'): void {
  const url = URL.createObjectURL(new Blob([text], { type: `${type};charset=utf-8` }))
  const anchor = document.createElement('a')
  anchor.href = url; anchor.download = name; anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
