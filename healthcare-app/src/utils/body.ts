export interface BodyInfo {
  weightKg: number
  heightCm: number
  age: number
}

export function validateBodyInfo(info: BodyInfo): string | null {
  if (!Number.isFinite(info.weightKg) || info.weightKg < 20 || info.weightKg > 300) {
    return '体重需在 20–300 公斤之间'
  }
  if (!Number.isFinite(info.heightCm) || info.heightCm < 50 || info.heightCm > 250) {
    return '身高需在 50–250 厘米之间'
  }
  if (!Number.isFinite(info.age) || info.age < 1 || info.age > 120) {
    return '年龄需在 1–120 岁之间'
  }
  return null
}
