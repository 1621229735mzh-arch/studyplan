export function reviewDateExplanation(source?: string | null): string {
  return source === 'MANUAL' ? '本次日期由手动修改'
    : source === 'INITIAL' ? '首轮安排在实际学完次日'
    : source === 'SOURCE' ? '来源重新学完，日期不早于重新学完次日'
    : source === 'FSRS' ? '下次日期由最近真实反馈和复习间隔模型计算'
    : source === 'SETTINGS' ? '下次日期由掌握反馈和当前间隔设置计算'
    : '历史日期来源未记录，可手动调整'
}
