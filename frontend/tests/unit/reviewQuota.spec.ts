import { describe, expect, it } from 'vitest'

import { quotaClaim, quotaOverviewText } from '@/features/review/quota'

/**
 * 复习额度表述。
 *
 * 后端只给出额度、已安排时长、剩余额度与 warnings，不给“是否符合额度”的结论；
 * 这里验证：缺额度或缺用时绝不宣称符合额度。
 */
describe('复习额度表述', () => {
  it('未设置额度时不能宣称符合额度', () => {
    const claim = quotaClaim(null, 15, [])
    expect(claim.claimsFit).toBe(false)
    expect(claim.reason).toContain('未设置')
  })

  it('缺少预计用时时不能判断是否符合额度', () => {
    const claim = quotaClaim(60, null, [])
    expect(claim.claimsFit).toBe(false)
    expect(claim.reason).toContain('缺少预计用时')
  })

  it('后端给出 warnings 时不宣称符合额度，并原样转述原因', () => {
    const claim = quotaClaim(60, 15, ['有 1 项缺少预计用时，确认安排时需要补充'])
    expect(claim.claimsFit).toBe(false)
    expect(claim.reason).toContain('缺少预计用时')
  })

  it('额度与用时齐全且无 warnings 时才可宣称额度可用', () => {
    expect(quotaClaim(60, 15, []).claimsFit).toBe(true)
  })

  it('整体说明在缺额度时明确说明不能宣称符合额度', () => {
    const text = quotaOverviewText(null, 0, null, ['尚未设置每日复习额度，无法判断建议是否在额度内'])
    expect(text).toContain('未设置')
    expect(text).toContain('不能宣称符合额度')
  })

  it('整体说明在额度与剩余都有时给出剩余额度，但明确以确认结果为准', () => {
    const text = quotaOverviewText(60, 20, 40, [])
    expect(text).toContain('剩余 40 分钟')
    expect(text).toContain('以确认结果为准')
  })
})
