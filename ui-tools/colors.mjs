import fs from 'node:fs';
import { PNG } from 'pngjs';

const file = process.argv[2];
const png = PNG.sync.read(fs.readFileSync(file));
const { width: W, height: H } = png;
const ch = png.colorType === 6 ? 4 : 3;

const buckets = new Map(); // key -> {count, top, mid, bot, r, g, b}
const key = (r, g, b) => ((r >> 3) << 11) | ((g >> 3) << 6) | (b >> 3);

for (let y = 0; y < H; y++) {
  const row = y * W * ch;
  const band = y < H / 3 ? 'top' : y < (H * 2) / 3 ? 'mid' : 'bot';
  for (let x = 0; x < W; x++) {
    const i = row + x * ch;
    const r = png.data[i], g = png.data[i + 1], b = png.data[i + 2];
    const k = key(r, g, b);
    let e = buckets.get(k);
    if (!e) { e = { r, g, b, count: 0, top: 0, mid: 0, bot: 0 }; buckets.set(k, e); }
    e.count++; e[band]++;
  }
}

const total = W * H;
const sorted = [...buckets.values()].sort((a, b) => b.count - a.count).slice(0, 14);
console.log(`文件: ${file} 尺寸: ${W}x${H} 颜色通道: ${ch}`);
console.log(`总像素: ${total}`);
console.log('排名  颜色hex   占比     上/中/下分布');
sorted.forEach((e, i) => {
  const hex = '#' + [e.r, e.g, e.b].map(v => v.toString(16).padStart(2, '0')).join('').toUpperCase();
  const pct = ((e.count / total) * 100).toFixed(1) + '%';
  const dist = `${Math.round((e.top / e.count) * 100)}/${Math.round((e.mid / e.count) * 100)}/${Math.round((e.bot / e.count) * 100)}`;
  console.log(`${String(i + 1).padStart(2)}    ${hex}   ${pct.padStart(7)}   ${dist}`);
});
