/**
 * 展示层格式化工具。
 *
 * 只做格式化，不做统计：所有数量、额度、进度都直接展示后端返回值。
 * 特别注意：不同单位绝不求和，因此这里没有“汇总数量”的辅助函数。
 */
import type { MasteryLevel, ReviewPhase } from '../types/domain'
import type { PlanSource } from '../features/plan/types'
import type { ReviewStatus } from '../features/review/types'

const ISO_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/

/** 解析 YYYY-MM-DD，按本地时区构造，避免 UTC 偏移导致日期串位。 */
export function parseIsoDate(value: string): Date | null {
  const matched = ISO_DATE_PATTERN.exec(value)
  if (matched === null) {
    return null
  }
  const year = Number.parseInt(matched[1], 10)
  const month = Number.parseInt(matched[2], 10)
  const day = Number.parseInt(matched[3], 10)
  if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) {
    return null
  }
  return new Date(year, month - 1, day)
}

/** Date → YYYY-MM-DD（本地时区）。 */
export function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** 今天。 */
export function todayIso(): string {
  return toIsoDate(new Date())
}

/** 日期加减天数。 */
export function addDays(isoDate: string, days: number): string {
  const date = parseIsoDate(isoDate)
  if (date === null) {
    return isoDate
  }
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

/** 所在周的周一（后端周计划以 weekStart 标识）。 */
export function weekStartOf(isoDate: string): string {
  const date = parseIsoDate(isoDate)
  if (date === null) {
    return isoDate
  }
  // getDay(): 0 表示周日，转换为“距离本周一的天数”。
  const offset = (date.getDay() + 6) % 7
  date.setDate(date.getDate() - offset)
  return toIsoDate(date)
}

/** 展示日期：2025-01-07 → 2025-01-07（周一）。 */
export function formatIsoDate(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = parseIsoDate(value)
  if (date === null) {
    return value
  }
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${value}（${weekday}）`
}

/** 展示时间戳：ISO 时间 → YYYY-MM-DD HH:mm。 */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${toIsoDate(date)} ${hours}:${minutes}`
}

/** 时长（分钟）展示：90 → 1 小时 30 分钟。 */
export function formatMinutes(minutes?: number | null): string {
  if (minutes === null || minutes === undefined) {
    return '未记录'
  }
  if (minutes < 60) {
    return `${minutes} 分钟`
  }
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分钟`
}

/** 掌握程度：不会 / 模糊 / 掌握（对应后端 MasteryResult）。 */
const MASTERY_LABELS: Record<MasteryLevel, string> = {
  FORGOT: '不会',
  VAGUE: '模糊',
  MASTERED: '掌握'
}

/** 掌握程度展示；未知取值原样显示，不猜测含义。 */
export function masteryLabel(level?: string | null): string {
  if (!level) {
    return '未反馈'
  }
  return MASTERY_LABELS[level as MasteryLevel] ?? level
}

/** 复习项状态：待安排 / 已安排 / 已归档（对应后端 ReviewStatus）。 */
const REVIEW_STATUS_LABELS: Record<ReviewStatus, string> = {
  PENDING: '待安排',
  SCHEDULED: '已安排',
  ARCHIVED: '已归档'
}

/** 复习项状态展示；未知取值原样显示。 */
export function reviewStatusLabel(status?: string | null): string {
  if (!status) {
    return '—'
  }
  return REVIEW_STATUS_LABELS[status as ReviewStatus] ?? status
}

/** 每日安排来源：手动安排 / 周计划 / 复习（对应后端 PlanSource）。 */
const PLAN_SOURCE_LABELS: Record<PlanSource, string> = {
  MANUAL: '手动安排',
  WEEKLY: '周计划',
  REVIEW: '复习'
}

/** 每日安排来源展示；未知取值原样显示。 */
export function planSourceLabel(source?: string | null): string {
  if (!source) {
    return '—'
  }
  return PLAN_SOURCE_LABELS[source as PlanSource] ?? source
}

const REVIEW_PHASE_LABELS: Record<ReviewPhase, string> = {
  MAIN: '主学习阶段',
  INTENSIVE: '集中复习阶段'
}

/** 复习阶段展示。 */
export function reviewPhaseLabel(phase?: string | null): string {
  if (!phase) {
    return '未设置'
  }
  return REVIEW_PHASE_LABELS[phase as ReviewPhase] ?? phase
}

/** 数量 + 单位；缺少单位时明确标注，避免读者误以为是无单位数字。 */
export function amountText(amount?: number | null, unit?: string | null): string {
  if (amount === null || amount === undefined) {
    return '—'
  }
  return unit ? `${amount} ${unit}` : `${amount}（单位未知）`
}

/**
 * 预算类字段展示。
 *
 * 后端用 null 表示“尚未确定预算”，必须显示为“未设置”，不能当作 0。
 */
export function formatBudget(minutes?: number | null): string {
  if (minutes === null || minutes === undefined) {
    return '未设置'
  }
  return formatMinutes(minutes)
}

/** 分钟展示：缺失时使用指定的缺失文案（用于区分“未记录”“未提供”等语义）。 */
export function formatMinutesOr(minutes: number | null | undefined, missing: string): string {
  if (minutes === null || minutes === undefined) {
    return missing
  }
  return formatMinutes(minutes)
}

/**
 * 复习间隔（天）展示。
 *
 * 后端用 null 表示“本人尚未配置该档位的间隔”，必须显示为“未设置”，不能当作 0 天。
 */
export function formatIntervalDays(days?: number | null): string {
  if (days === null || days === undefined) {
    return '未设置'
  }
  return `${days} 天`
}
