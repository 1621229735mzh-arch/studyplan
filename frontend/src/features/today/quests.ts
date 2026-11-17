import type { TodayChecklistTask, TodaySubjectSection } from './types'

export function isDailyQuest(task: TodayChecklistTask): boolean {
  if (task.kind === 'REVIEW' || task.continuation) return false
  return !!task.sequence || ['COUNT_STUDY', 'COUNT_REVIEW', 'CHECK_ONLY'].includes(task.activity?.mode ?? '')
}

export function questBoard(sections: TodaySubjectSection[]) {
  const select = (daily: boolean) => sections
    .map(section => ({ ...section, tasks: section.tasks.filter(task => isDailyQuest(task) === daily) }))
    .filter(section => section.tasks.length > 0)
  return [
    { id: 'main', title: '主线任务', english: 'MAIN QUEST', icon: 'targets',
      description: '把长期目标，推进到今天。', context: '当前主线 · 考研备考',
      empty: '今天暂无主线安排，按自己的节奏前进。', sections: select(false) },
    { id: 'daily', title: '每日任务', english: 'DAILY QUEST', icon: 'refresh',
      description: '持续练习，让每一次行动慢慢积累。', context: '日常练习 · 按需完成',
      empty: '今天暂无每日任务。', sections: select(true) },
    { id: 'side', title: '支线任务', english: 'SIDE QUEST', icon: 'memo',
      description: '留一些空间，探索新的可能。', context: '独立目标 · 兴趣探索',
      empty: '还没有支线任务。这里留给主线之外的项目与探索。', sections: [] as TodaySubjectSection[] }
  ]
}
