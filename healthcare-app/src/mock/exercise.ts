// 运动相关 mock 数据：运动页、历史页共用
export interface Sport {
  key: string
  name: string
  emoji: string
  bg: string
  bar: string
  kcal: number
}

export const sports: Sport[] = [
  {
    key: 'yoga',
    name: '瑜伽',
    emoji: '🧘',
    bg: '#bacac2',
    bar: '#2a6049',
    kcal: 320,
  },
  {
    key: 'walk',
    name: '快走',
    emoji: '🚶',
    bg: '#ffe2d3',
    bar: '#ec744a',
    kcal: 560,
  },
  {
    key: 'run',
    name: '跑步',
    emoji: '🏃',
    bg: '#d2d0e8',
    bar: '#655eb0',
    kcal: 210,
  },
  {
    key: 'ride',
    name: '骑车',
    emoji: '🚴',
    bg: '#fdedd0',
    bar: '#f8c060',
    kcal: 180,
  },
]

export interface HistoryRecord {
  emoji: string
  name: string
  date: string
  dateISO: string
  time: string
  dist: string
  label: string
  kcal: number
  bg: string
}

export const historyRecords: HistoryRecord[] = [
  {
    emoji: '🏃',
    name: '跑步',
    date: '2021 年 1 月 6 日',
    dateISO: '2021-01-06',
    time: '00:15:23',
    dist: '3.978 公里',
    label: '总长跑',
    kcal: 320,
    bg: '#bacac2',
  },
  {
    emoji: '🚴',
    name: '骑车',
    date: '2021 年 1 月 6 日',
    dateISO: '2021-01-06',
    time: '00:28:40',
    dist: '2,789 步',
    label: '总步数',
    kcal: 180,
    bg: '#ffe2d3',
  },
  {
    emoji: '🧘',
    name: '瑜伽',
    date: '2021 年 1 月 4 日',
    dateISO: '2021-01-04',
    time: '00:32:00',
    dist: '320 千卡',
    label: '消耗',
    kcal: 320,
    bg: '#d2d0e8',
  },
  {
    emoji: '🚶',
    name: '快走',
    date: '2020 年 12 月 28 日',
    dateISO: '2020-12-28',
    time: '00:45:10',
    dist: '4.2 公里',
    label: '总步行距离',
    kcal: 210,
    bg: '#fdedd0',
  },
]
