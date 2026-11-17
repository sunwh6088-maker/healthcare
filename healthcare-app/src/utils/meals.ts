export const MEALS = [
  { key: 'breakfast', label: '早餐', emoji: '🍳' },
  { key: 'lunch', label: '午餐', emoji: '🍱' },
  { key: 'dinner', label: '晚餐', emoji: '🍲' },
  { key: 'snack', label: '零食', emoji: '🍪' },
] as const

export type MealKey = (typeof MEALS)[number]['key']

export function mealLabel(key: string) {
  return MEALS.find((meal) => meal.key === key)?.label ?? '用餐'
}

export function mealEmoji(key: string) {
  return MEALS.find((meal) => meal.key === key)?.emoji ?? '🍽️'
}
