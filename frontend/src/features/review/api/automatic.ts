import { get, post } from '@/api/http'

export interface AutoReviewBook {
  streamId: number; subjectId: number; title: string; enabled: boolean; version: number
  segmentCount: number; reviewCount: number; overdueCount: number; suspendedCount: number
}
export interface AutoReviewSegment {
  reviewItemId: number; streamId: number; title: string; completedOn: string
  sourceValid: boolean; paused: boolean; enabled: boolean; status: string
  scheduledDate: string | null; nextReviewDate: string | null; repetitions: number; version: number
}
export function getAutoReviewBooks(): Promise<AutoReviewBook[]> { return get('/review/automatic/books') }
export function getAutoReviewSegments(): Promise<AutoReviewSegment[]> { return get('/review/automatic/books/segments') }
export function setAutoReviewPolicy(book: AutoReviewBook, enabled: boolean): Promise<AutoReviewBook> {
  return post(`/review/automatic/books/${book.streamId}`, { enabled, version: book.version })
}
