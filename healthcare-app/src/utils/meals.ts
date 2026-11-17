import { catalog } from './catalog'

export type MealKey = string

export function mealLabel(key: string) {
  return catalog.meals.find((meal) => meal.key === key)?.label ?? '用餐'
}

export function mealEmoji(key: string) {
  return catalog.meals.find((meal) => meal.key === key)?.emoji ?? '🍽️'
}
