import fs from 'node:fs';

for (const f of process.argv.slice(2)) {
  const buf = fs.readFileSync(f);
  console.log(`\n=== ${f} (${buf.length} bytes) ===`);
  console.log('签名:', buf.subarray(0, 8).toString('hex'));
  let off = 8;
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.subarray(off + 4, off + 8).toString('ascii');
    if (type === 'IHDR') {
      const w = buf.readUInt32BE(off + 8);
      const h = buf.readUInt32BE(off + 12);
      const depth = buf[off + 16];
      const colorType = buf[off + 17];
      const interlace = buf[off + 20];
      console.log(`IHDR: ${w}x${h} depth=${depth} colorType=${colorType} interlace=${interlace}`);
    } else {
      console.log(`chunk: ${type} len=${len}`);
    }
    off += 12 + len;
    if (type === 'IEND') break;
  }
}
