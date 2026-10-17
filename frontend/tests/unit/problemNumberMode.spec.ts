import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElButton, ElInputNumber } from 'element-plus'
import ProblemNumberModeDialog from '@/features/plan/components/ProblemNumberModeDialog.vue'
const mocks=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),online:{value:true}}))
vi.mock('@/api/http',()=>({get:mocks.get,post:mocks.post,toApiError:(e:Error)=>e}))
vi.mock('@/composables/useOnlineStatus',()=>({useOnlineStatus:()=>({online:mocks.online})}))
const wrappers: ReturnType<typeof mount>[]=[]
async function page() {const w=mount(ProblemNumberModeDialog,{props:{taskId:31},global:{plugins:[ElementPlus],stubs:{ElDialog:{template:'<div><slot/><slot name="footer"/></div>'}}}});wrappers.push(w);await flushPromises();return w}
function button(w:ReturnType<typeof mount>,text:string){return w.findAllComponents(ElButton).find(b=>b.text()===text)!}
beforeEach(()=>{vi.clearAllMocks();mocks.online.value=true;mocks.post.mockResolvedValue(undefined);mocks.get.mockResolvedValue({taskId:31,title:'每日一题',version:2,workspaceRevision:8,revision:'r1',history:[31,32,33].map((n,i)=>({problemNumber:n,record:{id:i+1,version:1,recordDate:`2026-10-${14+i}`,amount:1,sequenceTitle:`LeetCode ${n}`}}))})})
afterEach(()=>wrappers.splice(0).forEach(w=>w.unmount()))
it('预览三条记录并将第三条更正为49，携带完整版本且失败保留输入',async()=>{
 const w=await page();expect(mocks.post).not.toHaveBeenCalled();expect(w.text()).toContain('2026-10-16')
 w.findAllComponents(ElInputNumber)[2].vm.$emit('update:modelValue',49);await flushPromises()
 mocks.post.mockRejectedValueOnce(new Error('记录已变化，请重新预览'))
 await button(w,'确认修改').trigger('click');await flushPromises()
 expect(w.emitted('saved')).toBeUndefined();expect(w.findAllComponents(ElInputNumber)[2].props('modelValue')).toBe(49)
 expect(mocks.post.mock.calls[0]).toEqual(['/plan/tasks/31/problem-number-mode',{title:'LeetCode每日一题：自行填写题号',version:2,workspaceRevision:8,revision:'r1',corrections:[{id:1,version:1,problemNumber:31},{id:2,version:1,problemNumber:32},{id:3,version:1,problemNumber:49}]}])
})
it('离线不读取或修改计划',async()=>{mocks.online.value=false;const w=await page();expect(mocks.get).not.toHaveBeenCalled();expect(button(w,'确认修改').attributes('disabled')).toBeDefined();expect(mocks.post).not.toHaveBeenCalled()})
