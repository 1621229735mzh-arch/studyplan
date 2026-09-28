/**
 * 跨功能共享的领域类型。
 *
 * 只放真正被多个功能共用的类型：科目、计量单位、复习阶段、掌握程度。
 * 单个功能专用的 DTO 放在该功能的 types.ts 里。
 */

/** 科目（settings 模块维护，plan / learning / progress / review / memo / target 都会引用）。 */
export interface Subject {
  id: number
  name: string
  category?: string | null
  color?: string | null
  sortOrder: number
  enabled: boolean
}

/** 计量单位。不同单位不混加，因此计量结果必须带单位一起展示。 */
export interface StudyUnit {
  id: number
  name: string
  sortOrder: number
  enabled: boolean
}

/** 复习阶段（对应后端 ReviewPhase）：主学习阶段 / 集中复习阶段。 */
export type ReviewPhase = 'MAIN' | 'INTENSIVE'

/**
 * 掌握程度（对应后端 `MasteryResult`）：不会 / 模糊 / 掌握。
 *
 * 取值以后端为准；`masteryLabel()` 对未知取值会直接显示原值，不猜测含义。
 */
export type MasteryLevel = 'FORGOT' | 'VAGUE' | 'MASTERED'

/** 某个单位下的数量。跨单位求和是被禁止的，所以统计结果按单位成组返回。 */
export interface UnitAmount {
  unitId: number
  unitName: string
  amount: number
}
