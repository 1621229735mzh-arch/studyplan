import { toApiError } from '@/api/http'

export function todayReadFailure(error: unknown, online: boolean) {
  const api = toApiError(error)
  if (!online) return { title: '设备已离线，当前仅可查看快照', recovery: '请连接网络，再点击“重新读取今日”；成功读取服务器数据后才能记录。' }
  if (api.code === 'TIMEOUT') return { title: '服务器请求超时，当前仅可查看快照', recovery: '本次请求未在时限内完成。请点击“重新读取今日”，若持续超时请稍后再试。' }
  if (api.status === 401 || api.code === 'UNAUTHORIZED') return { title: '登录已失效，请重新登录', recovery: '重新登录后会返回今日页，并读取最新数据。' }
  if (api.status === 403) return { title: '当前账号无权读取今日数据', recovery: '请重新登录并确认账号权限，再读取今日数据。' }
  if (api.status >= 500) return { title: '学习服务暂时异常，当前仅可查看快照', recovery: `请稍后点击“重新读取今日”。原请求：${api.message}` }
  if (api.code === 'NETWORK_ERROR') return { title: '无法连接学习服务，当前仅可查看快照', recovery: '设备显示已联网，但未能连接服务器。请检查网络或代理，再点击“重新读取今日”。' }
  return { title: '今日数据读取失败，当前仅可查看快照', recovery: `请点击“重新读取今日”。原请求：${api.message}` }
}
