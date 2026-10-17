import { get, post } from '@/api/http'
export interface QuantityModePreview {
  taskId: number; title: string; mode: 'COUNT_STUDY' | 'COUNT_REVIEW'
  version: number; workspaceRevision: number; scheduleCount: number; preservedRecordCount: number
}
export const previewQuantityMode = (id: number) => get<QuantityModePreview>(`/plan/tasks/${id}/quantity-mode`)
export const confirmQuantityMode = (preview: QuantityModePreview, title: string) => post<void>(`/plan/tasks/${preview.taskId}/quantity-mode`, {
  title, mode: preview.mode, version: preview.version, workspaceRevision: preview.workspaceRevision
})
