// 模块：餐次工具 —— 从基础数据中读取餐次名称和图标。

import { catalog } from './catalog'

export type MealKey = string

export function mealLabel(key: string) {
  return catalog.meals.find((meal) => meal.key === key)?.label ?? '用餐'
}

export function mealEmoji(key: string) {
  return catalog.meals.find((meal) => meal.key === key)?.emoji ?? '🍽️'
}
