// 食物 mock 数据：发现页、搜索页、添加食物页共用
export interface Food {
  name: string
  en: string
  cal: string
  emoji: string
  category: 'main' | 'snack'
}

export const foods: Food[] = [
  // 常用主食 / 正餐
  { name: '萝卜', en: '1 根胡萝卜', cal: '29 卡路里', emoji: '🥕', category: 'main' },
  { name: '苹果', en: '1 个苹果', cal: '52 卡路里', emoji: '🍎', category: 'snack' },
  { name: '鸡胸肉', en: '100 克鸡胸肉', cal: '165 卡路里', emoji: '🍗', category: 'main' },
  { name: '鸡蛋', en: '1 个鸡蛋', cal: '78 卡路里', emoji: '🥚', category: 'main' },
  { name: '米饭', en: '1 碗米饭', cal: '116 卡路里', emoji: '🍚', category: 'main' },
  { name: '牛奶', en: '1 杯牛奶', cal: '42 卡路里', emoji: '🥛', category: 'main' },
  { name: '西兰花', en: '100 克西兰花', cal: '34 卡路里', emoji: '🥦', category: 'main' },
  { name: '香蕉', en: '1 根香蕉', cal: '89 卡路里', emoji: '🍌', category: 'main' },
  { name: '酸奶', en: '1 杯酸奶', cal: '95 卡路里', emoji: '🥛', category: 'snack' },

  // 更多主食 / 蛋白质 / 蔬菜
  { name: '糙米饭', en: '1 碗糙米饭', cal: '150 卡路里', emoji: '🍚', category: 'main' },
  { name: '全麦面包', en: '1 片全麦面包', cal: '70 卡路里', emoji: '🍞', category: 'main' },
  { name: '燕麦粥', en: '1 碗燕麦粥', cal: '150 卡路里', emoji: '🥣', category: 'main' },
  { name: '面条', en: '1 碗面条', cal: '220 卡路里', emoji: '🍜', category: 'main' },
  { name: '意大利面', en: '1 份意大利面', cal: '300 卡路里', emoji: '🍝', category: 'main' },
  { name: '红薯', en: '1 个红薯', cal: '112 卡路里', emoji: '🍠', category: 'main' },
  { name: '玉米', en: '1 根玉米', cal: '96 卡路里', emoji: '🌽', category: 'main' },
  { name: '土豆', en: '1 个土豆', cal: '130 卡路里', emoji: '🥔', category: 'main' },
  { name: '包子', en: '1 个包子', cal: '220 卡路里', emoji: '🍥', category: 'main' },
  { name: '饺子', en: '6 个饺子', cal: '300 卡路里', emoji: '🥟', category: 'main' },
  { name: '三明治', en: '1 个三明治', cal: '250 卡路里', emoji: '🥪', category: 'main' },
  { name: '寿司', en: '6 贯寿司', cal: '300 卡路里', emoji: '🍣', category: 'main' },
  { name: '牛肉', en: '100 克牛肉', cal: '250 卡路里', emoji: '🥩', category: 'main' },
  { name: '三文鱼', en: '100 克三文鱼', cal: '208 卡路里', emoji: '🐟', category: 'main' },
  { name: '虾', en: '100 克虾', cal: '99 卡路里', emoji: '🍤', category: 'main' },
  { name: '豆腐', en: '100 克豆腐', cal: '76 卡路里', emoji: '🍲', category: 'main' },
  { name: '豆浆', en: '1 杯豆浆', cal: '80 卡路里', emoji: '🥛', category: 'main' },
  { name: '菠菜', en: '100 克菠菜', cal: '23 卡路里', emoji: '🥬', category: 'main' },
  { name: '生菜', en: '100 克生菜', cal: '15 卡路里', emoji: '🥬', category: 'main' },
  { name: '番茄', en: '1 个番茄', cal: '22 卡路里', emoji: '🍅', category: 'main' },
  { name: '黄瓜', en: '1 根黄瓜', cal: '16 卡路里', emoji: '🥒', category: 'main' },
  { name: '南瓜', en: '100 克南瓜', cal: '26 卡路里', emoji: '🎃', category: 'main' },
  { name: '蘑菇', en: '100 克蘑菇', cal: '22 卡路里', emoji: '🍄', category: 'main' },
  { name: '蔬菜沙拉', en: '1 份蔬菜沙拉', cal: '120 卡路里', emoji: '🥗', category: 'main' },
  { name: '芝士', en: '1 片芝士', cal: '70 卡路里', emoji: '🧀', category: 'main' },
  { name: '烤鸡腿', en: '1 个烤鸡腿', cal: '280 卡路里', emoji: '🍗', category: 'main' },
  { name: '炒饭', en: '1 碗炒饭', cal: '330 卡路里', emoji: '🍛', category: 'main' },
  { name: '白粥', en: '1 碗白粥', cal: '90 卡路里', emoji: '🥣', category: 'main' },

  // 水果 / 零食 / 饮品
  { name: '橙子', en: '1 个橙子', cal: '62 卡路里', emoji: '🍊', category: 'snack' },
  { name: '蓝莓', en: '100 克蓝莓', cal: '57 卡路里', emoji: '🫐', category: 'snack' },
  { name: '草莓', en: '100 克草莓', cal: '32 卡路里', emoji: '🍓', category: 'snack' },
  { name: '葡萄', en: '100 克葡萄', cal: '69 卡路里', emoji: '🍇', category: 'snack' },
  { name: '西瓜', en: '1 块西瓜', cal: '46 卡路里', emoji: '🍉', category: 'snack' },
  { name: '芒果', en: '100 克芒果', cal: '60 卡路里', emoji: '🥭', category: 'snack' },
  { name: '猕猴桃', en: '1 个猕猴桃', cal: '42 卡路里', emoji: '🥝', category: 'snack' },
  { name: '梨', en: '1 个梨', cal: '57 卡路里', emoji: '🍐', category: 'snack' },
  { name: '桃子', en: '1 个桃子', cal: '39 卡路里', emoji: '🍑', category: 'snack' },
  { name: '牛油果', en: '半个牛油果', cal: '120 卡路里', emoji: '🥑', category: 'snack' },
  { name: '混合坚果', en: '1 小把混合坚果', cal: '170 卡路里', emoji: '🥜', category: 'snack' },
  { name: '杏仁', en: '10 颗杏仁', cal: '70 卡路里', emoji: '🌰', category: 'snack' },
  { name: '花生', en: '30 克花生', cal: '170 卡路里', emoji: '🥜', category: 'snack' },
  { name: '黑巧克力', en: '1 块黑巧克力', cal: '60 卡路里', emoji: '🍫', category: 'snack' },
  { name: '全麦饼干', en: '2 片全麦饼干', cal: '80 卡路里', emoji: '🍪', category: 'snack' },
  { name: '薯片', en: '1 小包薯片', cal: '150 卡路里', emoji: '🍟', category: 'snack' },
  { name: '冰淇淋', en: '1 球冰淇淋', cal: '140 卡路里', emoji: '🍦', category: 'snack' },
  { name: '蛋糕', en: '1 块蛋糕', cal: '250 卡路里', emoji: '🍰', category: 'snack' },
  { name: '蜂蜜', en: '1 勺蜂蜜', cal: '64 卡路里', emoji: '🍯', category: 'snack' },
  { name: '美式咖啡', en: '1 杯美式咖啡', cal: '5 卡路里', emoji: '☕', category: 'snack' },
  { name: '绿茶', en: '1 杯绿茶', cal: '0 卡路里', emoji: '🍵', category: 'snack' },
  { name: '蛋白棒', en: '1 根蛋白棒', cal: '200 卡路里', emoji: '🍫', category: 'snack' },
  { name: '爆米花', en: '1 小份爆米花', cal: '120 卡路里', emoji: '🍿', category: 'snack' },
  { name: '葡萄干', en: '30 克葡萄干', cal: '90 卡路里', emoji: '🍇', category: 'snack' },
  { name: '拿铁', en: '1 杯拿铁', cal: '120 卡路里', emoji: '☕', category: 'snack' },
  { name: '橙汁', en: '1 杯橙汁', cal: '110 卡路里', emoji: '🧃', category: 'snack' },
  { name: '可乐', en: '1 罐可乐', cal: '140 卡路里', emoji: '🥤', category: 'snack' },
  { name: '椰子水', en: '1 杯椰子水', cal: '45 卡路里', emoji: '🥥', category: 'snack' },
]

// 发现页三个 Tab 的列表
export const frequentFoods = foods.slice(0, 16)
export const recentFoods = [...foods].slice(0, 24).reverse()
export const customFoods = foods.slice(16, 30)

// 添加食物页：以前吃过的主食 + 零食
export const recentlyEaten = foods.filter((f) => f.category === 'main').slice(0, 6)
export const snackFoods = foods.filter((f) => f.category === 'snack')
