import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';

// 2x 最近邻放大，提升 OCR 识别率
const [input, outDir] = process.argv.slice(2);
const png = PNG.sync.read(fs.readFileSync(input));
const ch = png.colorType === 6 ? 4 : 3;

const out = new PNG({ width: png.width * 2, height: png.height * 2, colorType: 2 });
for (let y = 0; y < png.height; y++) {
  for (let x = 0; x < png.width; x++) {
    const s = (y * png.width + x) * ch;
    for (let dy = 0; dy < 2; dy++) {
      for (let dx = 0; dx < 2; dx++) {
        const d = ((y * 2 + dy) * png.width * 2 + x * 2 + dx) * 3;
        out.data[d] = png.data[s];
        out.data[d + 1] = png.data[s + 1];
        out.data[d + 2] = png.data[s + 2];
      }
    }
  }
}
const file = path.join(outDir, 'big-' + path.basename(input, path.extname(input)) + '.png');
fs.writeFileSync(file, PNG.sync.write(out, { colorType: 2, inputHasAlpha: false }));
console.log('已生成:', file);
