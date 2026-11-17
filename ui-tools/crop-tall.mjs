import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';

// 从原图裁出高比例的切片(750 x cropH)，测试是否可被识别
const [input, outDir, cropHArg] = process.argv.slice(2);
const png = PNG.sync.read(fs.readFileSync(input));
const cropH = Number(cropHArg || 1750);

for (let y = 0; y < png.height; y += cropH) {
  const h = Math.min(cropH, png.height - y);
  if (h < 200) continue;
  const out = new PNG({ width: png.width, height: h, colorType: 2 });
  for (let row = 0; row < h; row++) {
    const srcStart = ((y + row) * png.width) << 2;
    png.data.copy(out.data, row * png.width * 3, srcStart, srcStart + png.width * 3);
  }
  const file = path.join(outDir, `tall-${y}.png`);
  fs.writeFileSync(file, PNG.sync.write(out, { colorType: 2, inputHasAlpha: false }));
  console.log(`已生成: ${file} (y=${y}, h=${h})`);
}
console.log('完成');
