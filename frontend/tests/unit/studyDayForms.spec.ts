import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import RecordForm from '@/features/learning/components/RecordForm.vue'
import RecordEditorForm from '@/features/learning/components/RecordEditorForm.vue'
import type { LearningRecordFormModel } from '@/features/learning/types'

afterEach(() => vi.useRealTimers())

describe('记录表单学习日默认值', () => {
  it.each([['今日补记', RecordForm], ['记录页补记', RecordEditorForm]] as const)(
    '%s 凌晨归前一天，手选日期保持，九点后重置使用新日期', (_name, component) => {
      vi.useFakeTimers({ toFake: ['Date'] })
      vi.setSystemTime(new Date('2026-10-19T06:00:00+08:00'))
      const wrapper = mount(component, { global: { plugins: [ElementPlus] } })
      try {
        const form = wrapper.vm as unknown as {
          form: LearningRecordFormModel
          fillFrom: (model: Partial<LearningRecordFormModel>) => void
          reset: () => void
        }
        expect(form.form.recordDate).toBe('2026-10-18')
        form.fillFrom({ recordDate: '2026-10-16', content: '手动补记' })
        expect(form.form.recordDate).toBe('2026-10-16')
        vi.setSystemTime(new Date('2026-10-19T09:00:00+08:00'))
        expect(form.form.recordDate).toBe('2026-10-16')
        form.reset()
        expect(form.form.recordDate).toBe('2026-10-19')
      } finally {
        wrapper.unmount()
      }
    }
  )
})
