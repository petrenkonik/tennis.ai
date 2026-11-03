const sharp = require('sharp');
const path = require('path');

// Rebuild the collage: npm i sharp && node make_collage.js (sharp is installed next to the script)
const SRC = __dirname;
const OUT = SRC;
const W = 1080, H = 1350, M = 48;
const COL_W = 312, COL_GAP = 24;
const COL_CENTERS = [M + COL_W / 2, M + COL_W + COL_GAP + COL_W / 2, M + 2 * (COL_W + COL_GAP) + COL_W / 2];
const PHONE_H = 650, PHONE_TOP = 300, RADIUS = 24;

const LABELS = [
  { t: 'ORIGINAL · FLUTTER', c: '#b5e853' },
  { t: 'COPY · ANDROID KOTLIN', c: '#60a5fa' },
  { t: 'COPY · WEB · MINI APP', c: '#fb923c' },
];

const PAGES = [
  {
    n: '1 / 2',
    title: 'The “My Videos” screen',
    subtitle: 'Video library: counters and per-stroke cards',
    imgs: ['origina_app2.jpg', 'app2.JPG', 'web_app2.JPG'],
    note1: 'One reverse — three products:',
    note2: 'Flutter (Dart AOT)  →  Kotlin + Compose  →  Vue 3 (Telegram Mini App)',
  },
  {
    n: '2 / 2',
    title: 'Training modules',
    subtitle: 'Analysis entry: coach mode, auto-detect, stroke types',
    imgs: ['origina_app1.jpg', 'app1.JPG', 'web_app1.JPG'],
    note1: 'Clone UI verified against the original screenshots 1:1',
    note2: 'Biomech engine ported from the Dart AOT reverse: M0–M6 phases, metrics',
  },
];

function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

async function roundImg(file, height) {
  const src = path.join(SRC, file);
  const meta = await sharp(src).metadata();
  const cropW = file.startsWith('web_') ? meta.width - 12 : meta.width; // hide browser scrollbar on web captures
  const w = Math.round(height * cropW / meta.height);
  let pipe = sharp(src).rotate();
  if (cropW !== meta.width) pipe = pipe.extract({ left: 0, top: 0, width: cropW, height: meta.height });
  const buf = await pipe.resize(w, height).png().toBuffer();
  const mask = Buffer.from(`<svg width="${w}" height="${height}"><rect width="${w}" height="${height}" rx="${RADIUS}" fill="#fff"/></svg>`);
  const rounded = await sharp(buf).composite([{ input: mask, blend: 'dest-in' }]).png().toBuffer();
  return { rounded, w, h: height };
}

async function makePage(p) {
  const phones = [];
  for (const f of p.imgs) phones.push(await roundImg(f, PHONE_H));

  const layers = [];
  // phones (positioned before bg so bg SVG goes first in composite order)
  const phonePos = phones.map((ph, i) => {
    const x = Math.round(COL_CENTERS[i] - ph.w / 2);
    const y = PHONE_TOP;
    layers.push({ input: ph.rounded, left: x, top: y });
    return { x, y, w: ph.w, h: ph.h };
  });

  const bg = `<svg width="${W}" height="${H}" xmlns="http://www.w3.org/2000/svg">
  <rect width="${W}" height="${H}" fill="#0b0d0a"/>
  <text x="${M}" y="80" font-family="Arial" font-weight="bold" font-size="21" letter-spacing="4" fill="#8a948a">TENNIS.AI · REVERSE ENGINEERING</text>
  <text x="${W - M}" y="80" text-anchor="end" font-family="Arial" font-size="21" fill="#6b736b">${esc(p.n)}</text>
  <text x="${M}" y="146" font-family="Arial" font-weight="bold" font-size="46" fill="#f2f5ee">${esc(p.title)}</text>
  <text x="${M}" y="188" font-family="Arial" font-size="25" fill="#a8b0a8">${esc(p.subtitle)}</text>
  ${LABELS.map((l, i) => `
  <rect x="${COL_CENTERS[i] - 140}" y="256" width="280" height="44" rx="22" fill="#12160f" stroke="${l.c}" stroke-width="1.5"/>
  <text x="${COL_CENTERS[i]}" y="284" text-anchor="middle" font-family="Arial" font-weight="bold" font-size="17" letter-spacing="1" fill="${l.c}">${esc(l.t)}</text>`).join('')}
  ${phonePos.map(r => `<rect x="${r.x - 1}" y="${r.y - 1}" width="${r.w + 2}" height="${r.h + 2}" rx="${RADIUS + 1}" fill="none" stroke="#333a33" stroke-width="2"/>`).join('')}
  <rect x="${M}" y="1040" width="${W - 2 * M}" height="170" rx="24" fill="#12160f" stroke="#26301f" stroke-width="1.5"/>
  <text x="${W / 2}" y="1112" text-anchor="middle" font-family="Arial" font-weight="bold" font-size="27" fill="#e8ede4">${esc(p.note1)}</text>
  <text x="${W / 2}" y="1154" text-anchor="middle" font-family="Arial" font-size="26" fill="#c8d0c0">${esc(p.note2)}</text>
  <text x="${M}" y="1318" font-family="Arial" font-size="21" fill="#6b736b">Flutter → Kotlin + Web · engine ported from the reverse</text>
  <text x="${W - M}" y="1318" text-anchor="end" font-family="Arial" font-weight="bold" font-size="21" letter-spacing="2" fill="#8a948a">REA</text>
</svg>`;

  layers.unshift({ input: Buffer.from(bg), left: 0, top: 0 });
  const out = path.join(OUT, `collage_${p.n[0]}.png`);
  await sharp({ create: { width: W, height: H, channels: 4, background: '#0b0d0a' } })
    .composite(layers).png().toFile(out);
  console.log('OK', out);
}

(async () => { for (const p of PAGES) await makePage(p); })().catch(e => { console.error(e); process.exit(1); });
