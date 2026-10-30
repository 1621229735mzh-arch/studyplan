import type { LearningRecord } from '@/features/learning/types'
export type Direction = 'MATH' | 'ENGLISH' | 'POLITICS' | 'CS408'
export const directions: { key: Direction; title: string; subtitle: string; symbol: string }[] = [
  { key: 'MATH', title: '数学', subtitle: 'MATHEMATICS', symbol: '∑' },
  { key: 'ENGLISH', title: '英语', subtitle: 'ENGLISH', symbol: 'Aa' },
  { key: 'POLITICS', title: '政治', subtitle: 'POLITICS', symbol: '政' },
  { key: 'CS408', title: '408', subtitle: 'COMPUTER SCIENCE', symbol: '408' }
]
export interface ContentCounts { total: number; completed: number; inProgress: number; remaining: number }
export type ProgressMode = 'QUANTITY' | 'CONTENT_PERCENT'
export interface CatalogNode { id: number; key: string; parentKey: string | null; title: string; totalAmount: number | null; completedAmount: number | null; taskIds: number[]; sortOrder: number; completionPercent?: number | null; contentCounts?: ContentCounts | null }
export interface Course { id: number; key: string; direction: Direction; branch: string; title: string; subjectId: number; unitId: number; unitName: string; totalAmount: number | null; completedAmount: number | null; recordedAmount: number | null; nodes: CatalogNode[]; completedProblemNumbers?: number[]; progressMode?: ProgressMode; contentCounts?: ContentCounts | null }
export interface CumulativeQuantity { direction: Direction | 'UNASSIGNED'; subjectId: number; subjectName: string; unitId: number; unitName: string; amount: number }
export interface CountedProgress { taskId: number; title: string; direction: Direction | 'UNASSIGNED'; mode: 'COUNT_STUDY' | 'COUNT_REVIEW'; subjectId: number; subjectName: string; unitId: number; unitName: string; recordedAmount?: number | null; recordCount: number; learningDays: number; unknownQuantityRecords: number }
export interface CatalogLibrary { revision: number; courses: Course[]; cumulative: CumulativeQuantity[]; unclassified: LearningRecord[]; activities?: CountedProgress[] }
export interface NodeInput { key: string; parentKey: string | null; title: string; totalAmount: number | null; sortOrder: number; taskIds: number[] }
export interface CourseInput { key: string; direction: Direction; branch: string; title: string; subjectId: number; unitId: number; nodes: NodeInput[]; progressMode?: ProgressMode }
export interface CatalogDocument { schemaVersion: 1 | 2; courses: CourseInput[] }
export interface CatalogPreview { previewToken: string; revision: number; expiresAt: string; document: CatalogDocument; changes: { title: string; unitName: string; previousTotal: number | null; nextTotal: number | null; addedNodes: number; progressMode?: ProgressMode; previousItems?: number; nextItems?: number }[] }
export interface TaskCandidate { id: number; title: string; subjectId: number; unitId: number; plannedAmount: number; version: number; nodeId: number | null }
export interface CatalogSummary { revision: number; courses: Omit<Course, 'nodes' | 'completedProblemNumbers'>[]; cumulative: CumulativeQuantity[]; activities?: CountedProgress[] }
export function courseDocument(course: Course): CatalogDocument {
  return { schemaVersion: course.progressMode === 'CONTENT_PERCENT' ? 2 : 1, courses: [{ key: course.key, direction: course.direction, branch: course.branch, title: course.title, subjectId: course.subjectId, unitId: course.unitId, ...(course.progressMode === 'CONTENT_PERCENT' ? { progressMode: course.progressMode } : {}),
    nodes: course.nodes.map(n => ({ key: n.key, parentKey: n.parentKey ?? null, title: n.title, totalAmount: n.totalAmount ?? null, sortOrder: n.sortOrder, taskIds: [...n.taskIds] })) }] }
}
