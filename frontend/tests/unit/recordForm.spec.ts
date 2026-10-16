import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'

import RecordForm from '@/features/learning/components/RecordForm.vue'
import type { LearningRecordFormModel, LearningRecordTaskOption } from '@/features/learning/types'
import { validateLearningRecordForm } from '@/features/learning/validation'
import {
  invalidRecordFormFixture,
  timeOnlyRecordFormFixture,
  validRecordFormFixture
} from '../fixtures/learning'

const taskOptions: LearningRecordTaskOption[] = [
  {
    id: 11,
    title: '操作系统第 1—3 讲',
    subjectId: 2,
    subjectName: '操作系统',
    unitId: 3,
    unitName: '讲'
  }
]

/** 组件通过 defineExpose 暴露的成员；测试里显式声明，避免依赖类型推断细节。 */
interface RecordFormExposed {
  form: LearningRecordFormModel
  submit: () => void
  reset: () => void
  fillFrom: (model: Partial<LearningRecordFormModel>) => void
}

function mountRecordForm() {
  const wrapper = mount(RecordForm, {
    props: { tasks: taskOptions },
    global: { plugins: [ElementPlus] }
  })
  const exposed = wrapper.vm as unknown as RecordFormExposed
  return { wrapper, exposed }
}

describe('学习记录表单校验（纯函数）', () => {
  it('无单位的数量不能提交，未填写用时不报错', () => {
    const result = validateLearningRecordForm(invalidRecordFormFixture)
    expect(result.valid).toBe(false)
    expect(result.errors.durationMinutes).toBeUndefined()
    expect(result.errors.taskId).toBeTruthy()
    expect(result.summary).toContain('请先补全学习记录')
  })

  it('填写完成数量时必须能确定单位（单位来自任务）', () => {
    const result = validateLearningRecordForm({ ...invalidRecordFormFixture, durationMinutes: 30 })
    expect(result.valid).toBe(false)
    expect(result.errors.taskId).toContain('单位')
  })

  it('只记时长的计划外学习可以通过', () => {
    expect(validateLearningRecordForm(timeOnlyRecordFormFixture).valid).toBe(true)
  })

  it('关联任务并填写数量与时用时通过', () => {
    const result = validateLearningRecordForm(validRecordFormFixture)
    expect(result.valid).toBe(true)
    expect(result.summary).toBeNull()
  })

  it('复盘过长时给出提示', () => {
    const longNote = validateLearningRecordForm({ ...validRecordFormFixture, note: 'x'.repeat(501) })
    expect(longNote.valid).toBe(false)
    expect(longNote.errors.note).toBeTruthy()
  })
})

describe('学习记录表单组件', () => {
  it('无需用时即可提交，界面没有用时输入', async () => {
    const { wrapper, exposed } = mountRecordForm()
    exposed.fillFrom({ ...validRecordFormFixture, durationMinutes: null })
    exposed.submit()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).not.toContain('用时')
    expect(wrapper.emitted('submit')).toHaveLength(1)
    expect((wrapper.emitted('submit')![0][0] as {model: LearningRecordFormModel}).model.durationMinutes).toBeNull()
  })
  it('校验失败时不提交，并在界面上显示汇总提示', async () => {
    const { wrapper, exposed } = mountRecordForm()
    exposed.form.amount = 5
    exposed.form.durationMinutes = null

    exposed.submit()
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('请先补全学习记录')
  })

  it('校验通过后提交带幂等令牌的载荷', async () => {
    const { wrapper, exposed } = mountRecordForm()
    exposed.fillFrom(validRecordFormFixture)

    exposed.submit()
    await wrapper.vm.$nextTick()

    const events = wrapper.emitted('submit')
    expect(events).toHaveLength(1)
    const payload = events?.[0]?.[0] as { clientToken: string; model: LearningRecordFormModel }
    expect(payload.model.durationMinutes).toBe(90)
    expect(payload.model.unitId).toBe(3)
    expect(payload.model.taskId).toBe(11)
    expect(payload.clientToken.length).toBeGreaterThan(0)
  })

  it('提交后不清空内容，只有 reset() 才清空（保存失败可重试）', async () => {
    const { wrapper, exposed } = mountRecordForm()
    exposed.fillFrom(validRecordFormFixture)

    exposed.submit()
    await wrapper.vm.$nextTick()
    expect(exposed.form.durationMinutes).toBe(90)
    expect(exposed.form.amount).toBe(2)

    exposed.reset()
    expect(exposed.form.durationMinutes).toBeNull()
    expect(exposed.form.amount).toBeNull()
    expect(exposed.form.recordDate.length).toBe(10)
  })
})
