import fs from 'node:fs';
import path from 'node:path';

const root = process.argv[2];
const outLines = [];

function colorStr(c) {
  if (!c) return null;
  const a = Math.round(c.alpha * 255).toString(16).padStart(2, '0');
  const r = Math.round(c.red * 255).toString(16).padStart(2, '0');
  const g = Math.round(c.green * 255).toString(16).padStart(2, '0');
  const b = Math.round(c.blue * 255).toString(16).padStart(2, '0');
  return `#${r}${g}${b}${a === 'ff' ? '' : a}`;
}

function walk(layer, depth, artboard) {
  const t = layer._class;
  if (t === 'text') {
    let str = '';
    let style = null;
    const as = layer.attributedString;
    if (as && as.string) {
      str = as.string;
      const attrs = as.attributes && as.attributes[0] && as.attributes[0].attributes;
      if (attrs) {
        style = {
          font: attrs.MSAttributedStringFontAttribute
            ? attrs.MSAttributedStringFontAttribute.attributes.name
            : '?',
          size: attrs.MSAttributedStringFontAttribute
            ? attrs.MSAttributedStringFontAttribute.attributes.size
            : '?',
          color: colorStr(attrs.MSAttributedStringColorAttribute),
        };
      }
    }
    const clean = str.replace(/\n/g, '\\n');
    outLines.push(
      `  TEXT @(${Math.round(layer.frame.x)},${Math.round(layer.frame.y)}) ${Math.round(layer.frame.width)}x${Math.round(layer.frame.height)}: "${clean}"` +
      (style ? ` | font=${style.font} size=${style.size} color=${style.color}` : '')
    );
  }
  if (t === 'shapePath' || t === 'rectangle' || t === 'oval') {
    const fill = layer.style && layer.style.fills && layer.style.fills[0];
    if (fill && fill.isEnabled && fill.color) {
      outLines.push(
        `  SHAPE @(${Math.round(layer.frame.x)},${Math.round(layer.frame.y)}) ${Math.round(layer.frame.width)}x${Math.round(layer.frame.height)} fill=${colorStr(fill.color)}`
      );
    }
  }
  if (layer.layers) {
    for (const child of layer.layers) walk(child, depth + 1, artboard);
  }
}

const pagesDir = path.join(root, 'pages');
const files = fs.readdirSync(pagesDir).filter((f) => f.endsWith('.json'));
for (const f of files) {
  const data = JSON.parse(fs.readFileSync(path.join(pagesDir, f), 'utf8'));
  outLines.push(`\n########## 页面: ${f} ##########`);
  for (const layer of data.layers || []) {
    const isBoard = layer._class === 'artboard' || layer._class === 'symbolMaster';
    if (!isBoard) continue;
    outLines.push(
      `\n=== ${layer._class === 'symbolMaster' ? '[组件] ' : '[画板] '}${layer.name} @(${Math.round(layer.frame.x)},${Math.round(layer.frame.y)}) ${Math.round(layer.frame.width)}x${Math.round(layer.frame.height)}`
    );
    const bg = layer.backgroundColor;
    if (bg) outLines.push(`  背景色: ${colorStr(bg)}`);
    if (layer.layers) {
      for (const child of layer.layers) walk(child, 1, layer);
    }
  }
}

const outPath = path.join(root, 'parsed-design.txt');
fs.writeFileSync(outPath, outLines.join('\n'), 'utf8');
console.log(`写出 ${outLines.length} 行到 ${outPath}`);
