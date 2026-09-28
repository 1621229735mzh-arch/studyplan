import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useOfflineSnapshotStore } from '@/offline/useOfflineSnapshot'

import { clearAll, readSnapshot, readSnapshotMeta, saveSnapshot } from '@/offline/snapshot'
import { learningRecordFixture } from '../fixtures/learning'
import { weekFixture } from '../fixtures/plan'
import { progressFixture } from '../fixtures/progress'
import { todayFixture } from '../fixtures/today'

/**
 * 离线快照测试。
 *
 * jsdom 没有 IndexedDB，src/offline/db.ts 会自动退回到内存实现，
 * 因此这里验证的是快照模块本身的读写、合并与清理语义。
 */
describe('离线快照', () => {
  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2025-03-10T12:00:00'))
    setActivePinia(createPinia())
    await clearAll()
  })

  afterEach(() => vi.useRealTimers())

  it('浏览历史或未来日期不会覆盖今日快照（包括内存状态）', async () => {
    const store = useOfflineSnapshotStore()
    await store.save({ userName: 'kaoyan', today: todayFixture })
    for (const date of ['2025-03-09', '2025-03-11']) {
      await store.save({ userName: 'kaoyan', today: { ...todayFixture, date, plannedTasks: [] } })
      expect(store.today?.date).toBe('2025-03-10')
      expect((await readSnapshot())?.today?.plannedTasks).toHaveLength(2)
    }
  })

  it('跨日后不把旧快照当成今天，但保留其他切片', async () => {
    await saveSnapshot({ userName: 'kaoyan', today: todayFixture, week: weekFixture })
    vi.setSystemTime(new Date('2025-03-11T00:01:00'))
    expect((await readSnapshot())?.today).toBeNull()
    expect((await readSnapshot())?.week).toEqual(weekFixture)
    const store = useOfflineSnapshotStore()
    await store.load()
    expect(store.today).toBeNull()
  })

  it('没有缓存时返回 null', async () => {
    expect(await readSnapshot()).toBeNull()
    expect(await readSnapshotMeta()).toBeNull()
  })

  it('写入后记录用户名与同步时间，只包含本次写入的切片', async () => {
    const meta = await saveSnapshot({ userName: 'kaoyan', today: todayFixture })
    expect(meta.userName).toBe('kaoyan')
    expect(Number.isNaN(Date.parse(meta.syncedAt))).toBe(false)

    const snapshot = await readSnapshot()
    expect(snapshot).not.toBeNull()
    expect(snapshot?.meta.userName).toBe('kaoyan')
    expect(snapshot?.today?.plannedTasks?.[0]?.taskId).toBe(11)
    expect(snapshot?.week).toBeNull()
    expect(snapshot?.progress).toBeNull()
  })

  it('后续写入只覆盖对应切片，不影响已有切片', async () => {
    await saveSnapshot({ userName: 'kaoyan', today: todayFixture })
    await saveSnapshot({ userName: 'kaoyan', week: weekFixture, progress: progressFixture })

    const snapshot = await readSnapshot()
    expect(snapshot?.today?.plannedTasks?.length).toBe(2)
    expect(snapshot?.week?.weekStartDate).toBe('2025-03-10')
    expect(snapshot?.progress?.items?.length).toBe(2)
  })

  it('快照内容与接口结构一致（记录带单位，不同单位不合并）', async () => {
    await saveSnapshot({ userName: 'kaoyan', today: todayFixture })
    const snapshot = await readSnapshot()
    const records = snapshot?.today?.records ?? []
    // 记录保留自己的单位，不跨单位合并成一条数字
    expect(records.map((item) => item.unitName)).toEqual(['讲'])
    expect(records[0]?.durationMinutes).toBe(90)
    // 学习时长单独保存，不与完成量相加
    expect(snapshot?.today?.studyMinutes).toBe(95)
  })

  it('只缓存三个规定切片，不保存其它个人数据', async () => {
    // learningRecordFixture 只是一个普通对象；快照模块不提供任意 key 的写入能力。
    await saveSnapshot({ userName: 'kaoyan', today: todayFixture })
    const snapshot = await readSnapshot()
    expect(learningRecordFixture.version).toBe(2)
    expect(Object.keys(snapshot ?? {})).toEqual(['meta', 'today', 'week', 'progress'])
  })

  it('clearAll 清空全部本地数据（退出登录时调用）', async () => {
    await saveSnapshot({ userName: 'kaoyan', today: todayFixture, week: weekFixture, progress: progressFixture })
    await clearAll()
    expect(await readSnapshot()).toBeNull()
    expect(await readSnapshotMeta()).toBeNull()
  })
})
