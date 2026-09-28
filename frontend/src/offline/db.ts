/**
 * 离线快照的底层存储。
 *
 * 只用原生 IndexedDB，不引入额外依赖。仅缓存三个有限范围的数据切片
 * （今日、本周、进度概览）与同步元信息，绝不缓存其它认证接口的响应。
 *
 * 当 IndexedDB 不可用（隐私模式、测试环境 jsdom、配额被拒）时退回内存 Map，
 * 保证调用方逻辑一致；内存数据在页面刷新后消失，这符合“离线只读查看”的预期。
 */

const DB_NAME = 'kaoyan-study-offline'
const DB_VERSION = 1
const STORE_NAME = 'kv'

const memoryStore = new Map<string, unknown>()

/** IndexedDB 是否可用。 */
export function isIndexedDbAvailable(): boolean {
  return typeof indexedDB !== 'undefined' && indexedDB !== null
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        // 使用外部键存储：每个 key 对应一个数据切片（meta / today / week / progress）。
        db.createObjectStore(STORE_NAME)
      }
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('无法打开本地离线数据库'))
  })
}

async function withStore<T>(
  mode: IDBTransactionMode,
  action: (store: IDBObjectStore) => IDBRequest<T>
): Promise<T> {
  const db = await openDatabase()
  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = db.transaction(STORE_NAME, mode)
      const store = transaction.objectStore(STORE_NAME)
      const request = action(store)
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error ?? new Error('本地离线数据库操作失败'))
    })
  } finally {
    db.close()
  }
}

/** 读取一个键；不存在返回 null。 */
export async function kvGet<T>(key: string): Promise<T | null> {
  if (!isIndexedDbAvailable()) {
    return (memoryStore.get(key) as T | undefined) ?? null
  }
  try {
    const value = await withStore<unknown>('readonly', (store) => store.get(key))
    return (value as T | undefined) ?? null
  } catch (error) {
    console.warn('[offline] 读取本地快照失败，暂用内存数据', error)
    return (memoryStore.get(key) as T | undefined) ?? null
  }
}

/** 写入一个键。 */
export async function kvPut(key: string, value: unknown): Promise<void> {
  if (!isIndexedDbAvailable()) {
    memoryStore.set(key, value)
    return
  }
  try {
    await withStore<IDBValidKey>('readwrite', (store) => store.put(value, key))
    memoryStore.delete(key)
  } catch (error) {
    console.warn('[offline] 写入本地快照失败，暂用内存数据', error)
    memoryStore.set(key, value)
  }
}

/** 删除一个键。 */
export async function kvDelete(key: string): Promise<void> {
  memoryStore.delete(key)
  if (!isIndexedDbAvailable()) {
    return
  }
  try {
    await withStore<undefined>('readwrite', (store) => store.delete(key))
  } catch (error) {
    console.warn('[offline] 删除本地快照失败', error)
  }
}

/** 清空全部本地数据（退出登录时必须调用）。 */
export async function kvClear(): Promise<void> {
  memoryStore.clear()
  if (!isIndexedDbAvailable()) {
    return
  }
  try {
    await withStore<undefined>('readwrite', (store) => store.clear())
  } catch (error) {
    console.warn('[offline] 清理本地快照失败', error)
  }
}
