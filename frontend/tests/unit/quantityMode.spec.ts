import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElButton, ElInput } from 'element-plus'
import QuantityModeDialog from '@/features/plan/components/QuantityModeDialog.vue'
const mocks=vi.hoisted(()=>({preview:vi.fn(),confirm:vi.fn(),online:{value:true}}))
vi.mock('@/features/plan/api/quantity',()=>({previewQuantityMode:mocks.preview,confirmQuantityMode:mocks.confirm}))
vi.mock('@/composables/useOnlineStatus',()=>({useOnlineStatus:()=>({online:mocks.online})}))
const fixture={taskId:12,title:'每日50词',mode:'COUNT_STUDY',version:3,workspaceRevision:8,scheduleCount:123,preservedRecordCount:4}
const wrappers: ReturnType<typeof mount>[]=[]
async function page() { const w=mount(QuantityModeDialog,{props:{taskId:12},global:{plugins:[ElementPlus],stubs:{ElDialog:{template:'<div><slot/><slot name="footer"/></div>'}}}});wrappers.push(w);await flushPromises();return w }
function button(w:ReturnType<typeof mount>,text:string) {return w.findAllComponents(ElButton).find(b=>b.text()===text)!}
beforeEach(()=>{vi.clearAllMocks();mocks.online.value=true;mocks.preview.mockResolvedValue(fixture);mocks.confirm.mockResolvedValue(undefined)})
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
it('先显示实际影响范围，按预览版本确认并保留失败输入',async()=>{
 const w=await page();expect(mocks.confirm).not.toHaveBeenCalled();expect(w.text()).toContain('123 条日安排');expect(w.text()).toContain('4 条历史记录')
 w.findComponent(ElInput).vm.$emit('update:modelValue','每日背诵新词');await flushPromises()
 mocks.confirm.mockRejectedValueOnce(new Error('任务或日安排已变化，请重新预览'))
 await button(w,'确认修改').trigger('click');await flushPromises();expect(w.emitted('saved')).toBeUndefined();expect(w.findComponent(ElInput).props('modelValue')).toBe('每日背诵新词')
 expect(mocks.confirm).toHaveBeenCalledWith(fixture,'每日背诵新词');expect(w.text()).toContain('请重新预览')
 await button(w,'确认修改').trigger('click');await flushPromises();expect(w.emitted('saved')).toHaveLength(1)
})
it('离线不读取和修改任务',async()=>{
 mocks.online.value=false;const w=await page();expect(mocks.preview).not.toHaveBeenCalled();expect(button(w,'确认修改').attributes('disabled')).toBeDefined();expect(mocks.confirm).not.toHaveBeenCalled()
})
