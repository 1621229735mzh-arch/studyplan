import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { ref } from 'vue'
import ElementPlus, { ElButton, ElRadioGroup } from 'element-plus'
import { HttpApiError } from '@/api/http'
import CorrectionDialog from '@/features/review/components/ReviewFeedbackCorrectionDialog.vue'

const mocks = vi.hoisted(() => ({ get: vi.fn(), correct: vi.fn(), online: { value: true } }))
vi.mock('@/features/review/api', () => ({ getLatestReviewFeedback: mocks.get, correctReviewFeedback: mocks.correct }))
vi.mock('@/composables/useOnlineStatus', () => ({ useOnlineStatus: () => ({ online: mocks.online }) }))
const wrappers: ReturnType<typeof mount>[] = []
const context = () => ({ version: 4, allowed: true, reason: '', currentNextReviewDate: '2026-11-10', scheduledDate: '2026-11-10', record: { id: 8, reviewItemId: 3, reviewDate: '2026-11-01', result: 'MASTERED', nextReviewDate: '2026-11-04' } })
async function page() {
  const w = mount(CorrectionDialog, { props: { modelValue: true, itemId: 3, title: '函数' }, global: { plugins: [ElementPlus], stubs: { teleport: true, ElDialog: { template: '<div><slot /><slot name="footer" /></div>' } } } })
  wrappers.push(w); await flushPromises(); return w
}
function button(w: ReturnType<typeof mount>, label: string) { return w.findAllComponents(ElButton).find(b => b.text() === label)! }
beforeEach(() => { vi.clearAllMocks(); mocks.online = ref(true); mocks.get.mockResolvedValue(context()); mocks.correct.mockResolvedValue({}) })
afterEach(() => wrappers.splice(0).forEach(w => w.unmount()))
describe('复习反馈更正', () => {
  it('显示真实反馈和日期，选择更正后携带读取版本，成功才关闭', async () => {
    const w = await page(); expect(w.text()).toContain('2026-11-01'); expect(w.text()).toContain('原反馈：掌握'); expect(w.text()).toContain('当前下次日期：2026-11-10')
    expect(button(w, '更正并重算日期').attributes('disabled')).toBeDefined()
    w.getComponent(ElRadioGroup).vm.$emit('update:modelValue', 'FORGOT'); await flushPromises()
    await button(w, '更正并重算日期').trigger('click'); await flushPromises()
    expect(mocks.correct).toHaveBeenCalledWith(3, expect.objectContaining({ version: 4, recordId: 8, result: 'FORGOT' }))
    expect(w.emitted('saved')).toHaveLength(1); expect(w.emitted('update:modelValue')).toEqual([[false]])
  })
  it('失败保留选项和令牌，修改选项会生成新令牌', async () => {
    mocks.correct.mockRejectedValue(new Error('暂时失败'))
    const w = await page(); w.getComponent(ElRadioGroup).vm.$emit('update:modelValue', 'FORGOT'); await flushPromises()
    await button(w, '更正并重算日期').trigger('click'); await flushPromises()
    const token = mocks.correct.mock.calls[0][1].clientToken
    expect(w.text()).toContain('暂时失败'); expect(w.emitted('saved')).toBeUndefined()
    await button(w, '更正并重算日期').trigger('click'); await flushPromises()
    expect(mocks.correct.mock.calls[1][1].clientToken).toBe(token)
    w.getComponent(ElRadioGroup).vm.$emit('update:modelValue', 'VAGUE'); await flushPromises()
    await button(w, '更正并重算日期').trigger('click'); await flushPromises()
    expect(mocks.correct.mock.calls[2][1].clientToken).not.toBe(token)
  })
  it('编辑时网络断开保留所选反馈，恢复网络后仍需原版本提交', async () => {
    const w = await page(); w.getComponent(ElRadioGroup).vm.$emit('update:modelValue', 'FORGOT'); await flushPromises()
    mocks.online.value = false; await flushPromises()
    expect(button(w, '更正并重算日期').attributes('disabled')).toBeDefined()
    mocks.online.value = true; await flushPromises()
    expect(w.getComponent(ElRadioGroup).props('modelValue')).toBe('FORGOT')
    expect(mocks.get).toHaveBeenCalledTimes(1)
  })
  it('版本冲突后要求重新加载并重新核对' , async () => {
    mocks.correct.mockRejectedValue(new HttpApiError({ status: 409, code: 'REVIEW_ITEM_STALE', message: '复习内容已变化' }))
    const w = await page(); w.getComponent(ElRadioGroup).vm.$emit('update:modelValue', 'FORGOT'); await flushPromises()
    await button(w, '更正并重算日期').trigger('click'); await flushPromises()
    expect(button(w, '更正并重算日期').attributes('disabled')).toBeDefined()
    mocks.get.mockResolvedValue({ ...context(), version: 5 })
    await button(w, '重新加载反馈').trigger('click'); await flushPromises()
    expect(w.getComponent(ElRadioGroup).props('modelValue')).toBe('MASTERED')
  })
  it('有后续进度时展示拒绝原因，离线禁止读取和提交', async () => {
    mocks.get.mockResolvedValue({ ...context(), allowed: false, reason: '已有后续复习进度' })
    const w = await page(); expect(w.text()).toContain('已有后续复习进度'); expect(w.findComponent(ElRadioGroup).exists()).toBe(false)
    mocks.online.value = false; await flushPromises()
    expect(button(w, '更正并重算日期').attributes('disabled')).toBeDefined()
    await page(); expect(mocks.get).toHaveBeenCalledTimes(1); expect(mocks.correct).not.toHaveBeenCalled()
  })
})
