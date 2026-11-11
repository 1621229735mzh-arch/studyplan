/** Match the subject affiliation, never the task's free-text title. */
export function canRecordMathDuration(subjectName?: string | null, activityMode?: string | null): boolean {
  return !!subjectName && ['数学', '高数', '高等数学', '线代', '线性代数', '概率'].some(name => subjectName.includes(name))
    && !['CHECK_ONLY', 'COUNT_REVIEW', 'RECAP'].includes(activityMode ?? '')
}

export function durationError(minutes?: number | null, allowLegacyZero = false): string | null {
  if (minutes == null) return null
  return Number.isInteger(minutes) && minutes >= (allowLegacyZero ? 0 : 1) && minutes <= 2147483647
    ? null : '用时请填写正整数分钟，或留空'
}
