import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';

// 把 RGBA 切成小块并转为 RGB(colorType=2, 白底)，因为当前环境只认 RGB PNG
const [input, outDir, sliceHArg] = process.argv.slice(2);
const png = PNG.sync.read(fs.readFileSync(input));

console.log(`尺寸: ${png.width} x ${png.height} colorType=${png.colorType}`);

fs.mkdirSync(outDir, { recursive: true });

const sliceH = Number(sliceHArg || 750);
const name = path.basename(input, path.extname(input));
let idx = 0;
for (let y = 0; y < png.height; y += sliceH) {
  const h = Math.min(sliceH, png.height - y);
  const out = new PNG({ width: png.width, height: h, colorType: 2 });
  for (let row = 0; row < h; row++) {
    const srcRow = (y + row) * png.width * 4;
    const dstRow = row * png.width * 3;
    for (let x = 0; x < png.width; x++) {
      const s = srcRow + x * 4;
      const a = png.data[s + 3] / 255;
      const d = dstRow + x * 3;
      // 白底合成
      out.data[d]     = Math.round(png.data[s] * a + 255 * (1 - a));
      out.data[d + 1] = Math.round(png.data[s + 1] * a + 255 * (1 - a));
      out.data[d + 2] = Math.round(png.data[s + 2] * a + 255 * (1 - a));
    }
  }
  const file = path.join(outDir, `${name}-rgb-${String(idx++).padStart(2, '0')}.png`);
  fs.writeFileSync(file, PNG.sync.write(out, { colorType: 2, inputHasAlpha: false }));
  console.log(`已生成: ${file} (y=${y}, h=${h})`);
}
console.log('完成');
