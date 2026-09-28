import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { ElInput, ElSelect } from 'element-plus'
import ReviewItemPanel from '@/features/review/components/ReviewItemPanel.vue'
import { createReviewItem } from '@/features/review/api'

vi.mock('@/features/review/api', () => ({
  listReviewItems: vi.fn(async () => []),
  createReviewItem: vi.fn(async () => ({})),
  createReviewRecord: vi.fn()
}))
vi.mock('@/features/plan/api', () => ({
  listPlanTasks: vi.fn(async () => [
    { id: 1, title: '已学任务' }, { id: 2, title: '未学任务' }
  ])
}))
vi.mock('@/features/learning/api', () => ({
  listLearningRecords: vi.fn(async () => [
    { id: 10, taskId: 1, subjectId: 3, amount: 1, recordDate: '2026-09-20' },
    { id: 11, taskId: null, subjectId: 3, amount: 2, content: '已学笔记', recordDate: '2026-09-20' },
    { id: 12, taskId: null, subjectId: null, durationMinutes: 30, recordDate: '2026-09-20' }
  ])
}))
vi.mock('@/stores/catalog', () => ({
  useCatalogStore: () => ({ load: vi.fn(), subjectNameOf: () => '操作系统' })
}))

describe('复习内容来源', () => {
  beforeEach(() => vi.mocked(createReviewItem).mockClear())

  it('没有任务或来源学习记录时阻止提交', async () => {
    const wrapper = shallowMount(ReviewItemPanel)
    await flushPromises()
    await wrapper.find('el-button-stub').trigger('click')
    expect(createReviewItem).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请选择已有的计划外学习记录')
    wrapper.unmount()
  })

  it('计划外提交关联真实来源，失败后保留选择和标题以供重试', async () => {
    const wrapper = shallowMount(ReviewItemPanel)
    await flushPromises()
    const selectors = wrapper.findAllComponents(ElSelect)
    selectors[1]!.vm.$emit('update:modelValue', 11)
    await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(ElInput).props('modelValue')).toBe('已学笔记')
    vi.mocked(createReviewItem).mockRejectedValueOnce(new Error('保存失败'))
    await wrapper.find('el-button-stub').trigger('click')
    await flushPromises()
    expect(createReviewItem).toHaveBeenCalledWith({ sourceRecordId: 11, title: '已学笔记', estimatedMinutes: null })
    expect(selectors[1]!.props('modelValue')).toBe(11)
    expect(wrapper.findComponent(ElInput).props('modelValue')).toBe('已学笔记')
    wrapper.unmount()
  })
})
