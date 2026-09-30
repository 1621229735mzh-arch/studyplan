/** 仅格式化，统计与剩余比例来自服务器。 */
export function compactMinutes(value: number): string {
  const minutes = Math.round(value * 10) / 10
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = Math.round((minutes - hours * 60) * 10) / 10
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}
