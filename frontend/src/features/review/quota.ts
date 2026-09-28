/**
 * 复习额度表述的唯一来源。
 *
 * 后端（ReviewSuggestionResponse）只给出 `dailyReviewMinutes`、`scheduledMinutes`、
 * `remainingMinutes` 与 `warnings`，并不给出“是否符合额度”的结论。因此：
 *
 * - `dailyReviewMinutes == null`（未设额度）→ 不能宣称符合额度；
 * - `estimatedMinutes == null`（缺用时）→ 无法判断；
 * - `warnings` 非空 → 后端明确说明缺少输入，同样不能宣称符合额度。
 *
 * 这里只做纯函数判断，便于单元测试，也避免各组件各自写一套措辞。
 */

/** 对“能否宣称符合额度”的结论。 */
export interface QuotaClaim {
  /** 是否可以在界面上宣称这条建议符合额度 */
  claimsFit: boolean
  /** 不能宣称时的原因（中文，可直接展示） */
  reason: string | null
}

const QUOTA_MISSING = '尚未设置每日复习额度，无法判断是否符合额度，也不能宣称符合额度'
const ESTIMATE_MISSING = '缺少预计用时，无法判断是否在额度内'

/**
 * 判断单条建议能否宣称“符合额度”。
 *
 * @param dailyReviewMinutes 每日复习额度；null/undefined 表示未设置
 * @param estimatedMinutes   该条建议的预计用时；null/undefined 表示缺少用时
 * @param warnings           后端返回的缺失输入说明
 */
export function quotaClaim(
  dailyReviewMinutes: number | null | undefined,
  estimatedMinutes: number | null | undefined,
  warnings: readonly string[] = []
): QuotaClaim {
  if (dailyReviewMinutes === null || dailyReviewMinutes === undefined) {
    return { claimsFit: false, reason: QUOTA_MISSING }
  }
  if (estimatedMinutes === null || estimatedMinutes === undefined) {
    return { claimsFit: false, reason: ESTIMATE_MISSING }
  }
  if (warnings.length > 0) {
    return { claimsFit: false, reason: warnings.join('；') }
  }
  return { claimsFit: true, reason: null }
}

/** 建议列表整体的额度说明（用于页面顶部的提示条）。 */
export function quotaOverviewText(
  dailyReviewMinutes: number | null | undefined,
  scheduledMinutes: number | null | undefined,
  remainingMinutes: number | null | undefined,
  warnings: readonly string[] = []
): string {
  if (dailyReviewMinutes === null || dailyReviewMinutes === undefined) {
    return `${QUOTA_MISSING}（请到“设置”里填写每日复习额度）`
  }
  const scheduledText = scheduledMinutes === null || scheduledMinutes === undefined ? '未知' : `${scheduledMinutes} 分钟`
  if (remainingMinutes === null || remainingMinutes === undefined) {
    return `每日复习额度 ${dailyReviewMinutes} 分钟，当天已安排 ${scheduledText}；后端未给出剩余额度，不能判断是否符合额度`
  }
  if (warnings.length > 0) {
    return `每日复习额度 ${dailyReviewMinutes} 分钟，当天已安排 ${scheduledText}，剩余 ${remainingMinutes} 分钟；但${warnings.join('；')}，仍不能据此宣称符合额度`
  }
  return `每日复习额度 ${dailyReviewMinutes} 分钟，当天已安排 ${scheduledText}，剩余 ${remainingMinutes} 分钟；单条是否放得下以确认结果为准`
}
