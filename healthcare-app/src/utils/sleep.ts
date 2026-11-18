// 模块：睡眠时间计算 —— 就寝/醒来时间与跨午夜时长。

/** "HH:mm" → 从 0 点起的分钟数；格式非法时返回 null。 */
export function timeToMinutes(value: string): number | null {
  const match = /^(\d{1,2}):(\d{2})$/.exec(value.trim())
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null
  return hours * 60 + minutes
}

/** 从 0 点起的分钟数 → "HH:mm"（支持跨天取模）。 */
export function minutesToTime(total: number): string {
  const normalized = ((Math.round(total) % 1440) + 1440) % 1440
  const hours = Math.floor(normalized / 60)
  const minutes = normalized % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

/** 就寝到醒来的时长（分钟），自动处理跨午夜；相同时间视为无效返回 0。 */
export function sleepDuration(bedMinutes: number, wakeMinutes: number): number {
  const diff = (((wakeMinutes - bedMinutes) % 1440) + 1440) % 1440
  return diff
}

/** 合理睡眠时长：1-16 小时。 */
export function isReasonableSleep(durationMinutes: number): boolean {
  return durationMinutes >= 60 && durationMinutes <= 16 * 60
}
