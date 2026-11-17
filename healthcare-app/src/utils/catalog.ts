// 模块：基础数据仓库 —— 从 /api/catalog 加载食物、运动、设备等目录。

import { reactive } from 'vue'
import { api, type CatalogFood, type CatalogResponse } from '../api'

const empty: CatalogResponse = {
  foods: [],
  exerciseTypes: [],
  exerciseLessons: [],
  sleepSounds: [],
  devices: [],
  waterPortions: [],
  meals: [],
  recentFoodNames: [],
  frequentFoodNames: [],
}

// 食物、运动、设备等基础数据统一来自后端数据库，前端只做一次加载和缓存
export const catalog = reactive<CatalogResponse>({ ...empty })

export const catalogStatus = reactive({
  loaded: false,
  loading: false,
  error: '',
})

let pending: Promise<void> | null = null

export function loadCatalog(force = false): Promise<void> {
  if (catalogStatus.loaded && !force) return Promise.resolve()
  if (pending) return pending

  catalogStatus.loading = true
  catalogStatus.error = ''
  pending = api
    .getCatalog()
    .then((data) => {
      Object.assign(catalog, empty, data)
      catalogStatus.loaded = true
    })
    .catch((error) => {
      catalogStatus.error = error instanceof Error ? error.message : '基础数据加载失败'
      throw error
    })
    .finally(() => {
      catalogStatus.loading = false
      pending = null
    })
  return pending
}

/** 按“最近/频繁”名称取食物，没有记录时回退到目录前 N 个 */
export function pickFoods(names: string[], limit: number): CatalogFood[] {
  const byName = new Map(catalog.foods.map((food) => [food.name, food]))
  const picked = names
    .map((name) => byName.get(name))
    .filter((food): food is CatalogFood => food !== undefined)
  return picked.length ? picked.slice(0, limit) : catalog.foods.slice(0, limit)
}
