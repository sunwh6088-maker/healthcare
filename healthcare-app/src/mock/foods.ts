// 食物 mock 数据：发现页、搜索页、添加食物页共用
export interface Food {
  name: string
  en: string
  cal: string
  emoji: string
  category: 'main' | 'snack'
}

export const foods: Food[] = [
  { name: '萝卜', en: '1 根胡萝卜', cal: '29 卡路里', emoji: '🥕', category: 'main' },
  { name: '苹果', en: '1 个苹果', cal: '52 卡路里', emoji: '🍎', category: 'snack' },
  { name: '鸡胸肉', en: '100 克鸡胸肉', cal: '165 卡路里', emoji: '🍗', category: 'main' },
  { name: '鸡蛋', en: '1 个鸡蛋', cal: '78 卡路里', emoji: '🥚', category: 'main' },
  { name: '米饭', en: '1 碗米饭', cal: '116 卡路里', emoji: '🍚', category: 'main' },
  { name: '牛奶', en: '1 杯牛奶', cal: '42 卡路里', emoji: '🥛', category: 'main' },
  { name: '西兰花', en: '100 克西兰花', cal: '34 卡路里', emoji: '🥦', category: 'main' },
  { name: '香蕉', en: '1 根香蕉', cal: '89 卡路里', emoji: '🍌', category: 'main' },
  { name: '酸奶', en: '1 杯酸奶', cal: '95 卡路里', emoji: '🥛', category: 'snack' },
]

// 发现页三个 Tab 的列表
export const frequentFoods = foods
export const recentFoods = [...foods].reverse()
export const customFoods = foods.slice(2, 7)

// 添加食物页：以前吃过的主食 + 零食
export const recentlyEaten = foods.filter((f) => f.category === 'main').slice(0, 3)
export const snackFoods = foods.filter((f) => f.category === 'snack')
