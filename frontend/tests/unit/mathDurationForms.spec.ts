import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import RecordForm from '@/features/learning/components/RecordForm.vue'
import RecordEditorForm from '@/features/learning/components/RecordEditorForm.vue'
import LearningDurationInput from '@/features/learning/components/LearningDurationInput.vue'
import type { LearningRecordFormModel } from '@/features/learning/types'

const wrappers: ReturnType<typeof mount>[] = []
afterEach(() => wrappers.splice(0).forEach(w => w.unmount()))
const task = { id: 1, title: '当前内容', subjectId: 1, subjectName: '数学一', unitId: 1, unitName: '段' }
const model = { recordDate: '2026-11-11', taskId: 1, subjectId: 1, unitId: 1, amount: 1, content: '', note: '', durationMinutes: null }
type Exposed = { fillFrom: (model: Partial<LearningRecordFormModel>) => void; submit: () => void }

describe.each([['今日补记', RecordForm], ['记录页', RecordEditorForm]] as const)('%s 的数学用时', (_name, component) => {
  function page(mode: 'create' | 'edit' = 'create') {
    const wrapper = mount(component, { props: { tasks: [task], mode }, global: { plugins: [ElementPlus] } })
    wrappers.push(wrapper)
    const form = wrapper.vm as unknown as Exposed
    return { wrapper, form }
  }
  it('可留空，填写后随记录发送；切换非数学任务不携带新用时', async () => {
    const { wrapper, form } = page()
    form.fillFrom(model); await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(LearningDurationInput).props('modelValue')).toBeNull()
    form.submit()
    wrapper.findComponent(LearningDurationInput).vm.$emit('update:modelValue',45)
    await wrapper.vm.$nextTick(); form.submit()
    const submissions = wrapper.emitted('submit')!.map(e => e[0] as { model: LearningRecordFormModel; clientToken: string })
    expect(submissions[0].model.durationMinutes).toBeNull()
    expect(submissions[1].model.durationMinutes).toBe(45)
    expect(submissions[1].clientToken).not.toBe(submissions[0].clientToken)
    await wrapper.setProps({ tasks: [{ ...task, subjectName: '操作系统' }] })
    expect(wrapper.findComponent(LearningDurationInput).exists()).toBe(false)
    form.submit()
    expect((wrapper.emitted('submit')!.at(-1)![0] as { model: LearningRecordFormModel }).model.durationMinutes).toBeNull()
  })
  it('编辑原样回显，可以补填、更正或清空，不改变原进度', async () => {
    const { wrapper, form } = page('edit')
    form.fillFrom({ ...model, durationMinutes: 35 }); await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(LearningDurationInput).props('modelValue')).toBe(35)
    wrapper.findComponent(LearningDurationInput).vm.$emit('update:modelValue',50)
    await wrapper.vm.$nextTick(); form.submit()
    expect((wrapper.emitted('submit')![0][0] as { model: LearningRecordFormModel }).model).toMatchObject({ amount: 1, durationMinutes: 50 })
    wrapper.findComponent(LearningDurationInput).vm.$emit('update:modelValue',null)
    await wrapper.vm.$nextTick(); form.submit()
    expect((wrapper.emitted('submit')![1][0] as { model: LearningRecordFormModel }).model.durationMinutes).toBeNull()
  })
  it('非法分钟数阻止提交，旧记录的零值编辑时仍保留', async () => {
    const { wrapper, form } = page()
    for (const durationMinutes of [0, -1, 1.5, Number.NaN, Number.POSITIVE_INFINITY, 2147483648]) {
      form.fillFrom({ ...model, durationMinutes }); form.submit()
    }
    expect(wrapper.emitted('submit')).toBeUndefined()
    await wrapper.setProps({ mode: 'edit' })
    form.fillFrom({ ...model, durationMinutes: 0 }); form.submit()
    expect((wrapper.emitted('submit')![0][0] as { model: LearningRecordFormModel }).model.durationMinutes).toBe(0)
  })
})

it('接续记录修改用时保留百分比增量，计划外数学也能填写', async () => {
  const wrapper = mount(RecordEditorForm, { props: { tasks: [{ ...task, activityMode: 'CONTINUED_STUDY' }],
    mode: 'edit', continued: true, progressDelta: 40 }, global: { plugins: [ElementPlus] } })
  wrappers.push(wrapper)
  const form = wrapper.vm as unknown as Exposed
  form.fillFrom({ ...model, amount: null, durationMinutes: 45 }); await wrapper.vm.$nextTick()
  expect(wrapper.text()).toContain('本次增加进度：40%')
  expect(wrapper.findComponent(LearningDurationInput).exists()).toBe(true)
  form.submit()
  expect((wrapper.emitted('submit')![0][0] as { model: LearningRecordFormModel }).model).toMatchObject({ amount: null, durationMinutes: 45 })
  await wrapper.setProps({ tasks: [], continued: false, mode: 'create',
    subjects: [{ id: 1, name: '数学一', enabled: true, sortOrder: 0 }] })
  form.fillFrom({ ...model, taskId: null, amount: null, content: '计划外数学学习' }); await wrapper.vm.$nextTick()
  expect(wrapper.findComponent(LearningDurationInput).exists()).toBe(true)
})
