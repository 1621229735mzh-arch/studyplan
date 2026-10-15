import { get, post, put } from '@/api/http'
import type { CatalogLibrary, CatalogPreview, TaskCandidate } from '../catalog'
export function getCatalog(signal?: AbortSignal) { return get<CatalogLibrary>('/progress/catalog', { signal }) }
export function getTaskCandidates() { return get<TaskCandidate[]>('/progress/catalog/candidates') }
export function previewCatalog(document: string) { return post<CatalogPreview>('/progress/catalog/preview', { document }) }
export function confirmCatalog(previewToken: string) { return post<CatalogLibrary>('/progress/catalog/confirm', { previewToken }) }
export function classifyRecord(id: number, nodeId: number | null, recordVersion: number, revision: number) { return put<CatalogLibrary>(`/progress/catalog/records/${id}`, { nodeId, recordVersion, revision }) }
export function linkNodeTasks(id: number, taskIds: number[], revision: number) { return put<CatalogLibrary>(`/progress/catalog/nodes/${id}/tasks`, { taskIds, revision }) }
