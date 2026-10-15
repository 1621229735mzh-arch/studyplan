import type { LearningRecord } from '@/features/learning/types'
export type Direction = 'MATH' | 'ENGLISH' | 'POLITICS' | 'CS408'
export const directions: { key: Direction; title: string; subtitle: string; symbol: string }[] = [
  { key: 'MATH', title: '数学', subtitle: 'MATHEMATICS', symbol: '∑' },
  { key: 'ENGLISH', title: '英语', subtitle: 'ENGLISH', symbol: 'Aa' },
  { key: 'POLITICS', title: '政治', subtitle: 'POLITICS', symbol: '政' },
  { key: 'CS408', title: '408', subtitle: 'COMPUTER SCIENCE', symbol: '408' }
]
export interface CatalogNode { id: number; key: string; parentKey: string | null; title: string; totalAmount: number | null; completedAmount: number; taskIds: number[]; sortOrder: number }
export interface Course { id: number; key: string; direction: Direction; branch: string; title: string; subjectId: number; unitId: number; unitName: string; totalAmount: number; completedAmount: number; recordedAmount: number; nodes: CatalogNode[] }
export interface CumulativeQuantity { direction: Direction | 'UNASSIGNED'; subjectId: number; subjectName: string; unitId: number; unitName: string; amount: number }
export interface CatalogLibrary { revision: number; courses: Course[]; cumulative: CumulativeQuantity[]; unclassified: LearningRecord[] }
export interface NodeInput { key: string; parentKey: string | null; title: string; totalAmount: number | null; sortOrder: number; taskIds: number[] }
export interface CourseInput { key: string; direction: Direction; branch: string; title: string; subjectId: number; unitId: number; nodes: NodeInput[] }
export interface CatalogDocument { schemaVersion: 1; courses: CourseInput[] }
export interface CatalogPreview { previewToken: string; revision: number; expiresAt: string; document: CatalogDocument; changes: { title: string; unitName: string; previousTotal: number; nextTotal: number; addedNodes: number }[] }
export interface TaskCandidate { id: number; title: string; subjectId: number; unitId: number; plannedAmount: number; version: number; nodeId: number | null }
export interface CatalogSummary { revision: number; courses: Omit<Course, 'nodes'>[]; cumulative: CumulativeQuantity[] }
export function courseDocument(course: Course): CatalogDocument {
  return { schemaVersion: 1, courses: [{ key: course.key, direction: course.direction, branch: course.branch, title: course.title, subjectId: course.subjectId, unitId: course.unitId,
    nodes: course.nodes.map(n => ({ key: n.key, parentKey: n.parentKey ?? null, title: n.title, totalAmount: n.totalAmount ?? null, sortOrder: n.sortOrder, taskIds: [...n.taskIds] })) }] }
}
