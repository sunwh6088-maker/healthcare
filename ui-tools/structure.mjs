import fs from 'node:fs';
import { PNG } from 'pngjs';

// 分析版面结构：寻找贯穿全图的行/列边界（可能是屏幕之间的分隔线）
const file = process.argv[2];
const png = PNG.sync.read(fs.readFileSync(file));
const { width: W, height: H } = png;
const ch = png.colorType === 6 ? 4 : 3;

function px(x, y) {
  const i = (y * W + x) * ch;
  return (png.data[i] << 16) | (png.data[i + 1] << 8) | png.data[i + 2];
}

// 列一致性：每列中，与上一行相同的像素占比
console.log(`=== 列分析（寻找纵向边界，如两屏并排的分界） ===`);
const colUniform = [];
for (let x = 0; x < W; x++) {
  let same = 0;
  for (let y = 1; y < H; y++) {
    if (px(x, y) === px(x, y - 1)) same++;
  }
  colUniform.push(same / (H - 1));
}
// 打印一致性 > 0.9 的连续列段
let seg = null;
for (let x = 0; x <= W; x++) {
  const u = x < W ? colUniform[x] : 0;
  if (u > 0.9) {
    if (!seg) seg = { start: x, max: u };
    else seg.max = Math.max(seg.max, u);
  } else if (seg) {
    console.log(`列 ${seg.start}-${x - 1} 一致(>90%): ${seg.max.toFixed(3)}`);
    seg = null;
  }
}

// 行一致性
console.log(`=== 行分析（寻找横向边界） ===`);
const rowUniform = [];
for (let y = 0; y < H; y++) {
  let same = 0;
  for (let x = 1; x < W; x++) {
    if (px(x, y) === px(x - 1, y)) same++;
  }
  rowUniform.push(same / (W - 1));
}
seg = null;
for (let y = 0; y <= H; y++) {
  const u = y < H ? rowUniform[y] : 0;
  if (u > 0.9) {
    if (!seg) seg = { start: y, max: u };
    else seg.max = Math.max(seg.max, u);
  } else if (seg) {
    console.log(`行 ${seg.start}-${y - 1} 一致(>90%): ${seg.max.toFixed(3)}`);
    seg = null;
  }
}
