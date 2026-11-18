// 生成健康追踪 PWA 图标：深绿圆角底 + 白色爱心。
const { PNG } = require('pngjs')
const fs = require('fs')
const path = require('path')

const outDir = path.resolve(__dirname, '../healthcare-app/public')
const green = [0x2a, 0x60, 0x49]
const white = [0xff, 0xff, 0xff]

function makeIcon(size, filename) {
  const png = new PNG({ width: size, height: size })
  const radius = size * 0.22
  const half = size / 2
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const dx = Math.max(Math.abs(x + 0.5 - half) - (half - radius), 0)
      const dy = Math.max(Math.abs(y + 0.5 - half) - (half - radius), 0)
      const inside = Math.sqrt(dx * dx + dy * dy) <= radius

      const nx = (x + 0.5 - half) / (size * 0.3)
      const ny = -(y + 0.5 - size * 0.47) / (size * 0.29)
      const heart =
        Math.pow(nx * nx + ny * ny - 1, 3) - nx * nx * Math.pow(ny, 3) <= 0

      const color = heart ? white : green
      const idx = (size * y + x) << 2
      png.data[idx] = color[0]
      png.data[idx + 1] = color[1]
      png.data[idx + 2] = color[2]
      png.data[idx + 3] = inside ? 255 : 0
    }
  }
  fs.writeFileSync(path.join(outDir, filename), PNG.sync.write(png))
  console.log('wrote', filename)
}

makeIcon(192, 'icon-192.png')
makeIcon(512, 'icon-512.png')
