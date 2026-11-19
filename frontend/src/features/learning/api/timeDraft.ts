import { del, get, put } from '@/api/http'

export interface TimeDraft { startedAt: string; revision: string }
export interface StudyTimeRange { revision: string; endedAt: string }

const path = (context: string) => `/learning/time-drafts/${context}`

export async function readTimeDraft(context: string): Promise<TimeDraft | null> {
  return (await get<{ draft?: TimeDraft }>(path(context))).draft ?? null
}

export function saveTimeDraft(context: string, startedAt: string, revision: string | null) {
  return put<TimeDraft>(path(context), { startedAt, revision })
}

export function discardTimeDraft(context: string, revision: string) {
  return del(path(context), { params: { revision } })
}
