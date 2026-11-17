import fs from 'node:fs';
import path from 'node:path';
import { PNG } from 'pngjs';

const [input, outDir, sliceHArg] = process.argv.slice(2);
const png = PNG.sync.read(fs.readFileSync(input));

console.log(`尺寸: ${png.width} x ${png.height}`);

fs.mkdirSync(outDir, { recursive: true });

const sliceH = Number(sliceHArg || 750);
const name = path.basename(input, path.extname(input));
let idx = 0;
for (let y = 0; y < png.height; y += sliceH) {
  const h = Math.min(sliceH, png.height - y);
  const out = new PNG({ width: png.width, height: h });
  for (let row = 0; row < h; row++) {
    const srcStart = ((y + row) * png.width) << 2;
    png.data.copy(out.data, row * png.width * 4, srcStart, srcStart + png.width * 4);
  }
  const file = path.join(outDir, `${name}-${String(idx++).padStart(2, '0')}.png`);
  fs.writeFileSync(file, PNG.sync.write(out));
  console.log(`已生成: ${file} (y=${y}, h=${h})`);
}
console.log('完成');
