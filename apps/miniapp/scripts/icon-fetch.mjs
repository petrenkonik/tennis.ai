// Одноразовый скрипт: тянет официальные Material Symbols (outlined, weight 400) —
// те же иконки, что использует клон (Icons.Outlined.*), и генерирует materialIcons.ts.
const ICONS = {
  home: ['home'],
  videocam: ['videocam'],
  chart: ['show_chart'],
  grid: ['grid_view'],
  person: ['person'],
  'arrow-left': ['keyboard_arrow_left'],
  'arrow-right': ['keyboard_arrow_right'],
  'arrow-down': ['keyboard_arrow_down'],
  'chevron-left': ['chevron_left'],
  'chevron-right': ['chevron_right'],
  sensors: ['sensors'],
  clipboard: ['assignment_ind'],
  scan: ['center_focus_weak'],
  play: ['play_arrow'],
  dumbbell: ['fitness_center'],
  bars: ['bar_chart'],
  clock: ['schedule'],
  check: ['check'],
  chat: ['chat_bubble_outline', 'chat_bubble'],
  doc: ['description'],
  globe: ['public'],
  gift: ['redeem'],
  target: ['track_changes'],
  trend: ['trending_up'],
  bell: ['notifications'],
  cpu: ['memory'],
  mail: ['mail', 'email'],
  info: ['info'],
  scale: ['scale'],
  hand: ['pan_tool'],
  sparkles: ['auto_awesome'],
  mic: ['mic'],
  upload: ['upload'],
  send: ['send'],
  volume: ['volume_up'],
};

const BASE = 'https://cdn.jsdelivr.net/npm/@material-symbols/svg-400/outlined';
const version = await (await fetch('https://cdn.jsdelivr.net/npm/@material-symbols/svg-400/package.json')).json();

const out = {};
const missing = [];
for (const [key, candidates] of Object.entries(ICONS)) {
  let done = false;
  for (const name of candidates) {
    const res = await fetch(`${BASE}/${name}.svg`);
    if (!res.ok) continue;
    const svg = await res.text();
    const vb = svg.match(/viewBox="([^"]+)"/)?.[1];
    let body = svg.slice(svg.indexOf('>') + 1, svg.lastIndexOf('</svg>'));
    body = body
      .replace(/xmlns="[^"]*"/g, '')
      .replace(/fill="#[0-9a-fA-F]{3,8}"/g, '') // цвет наследуется от svg (currentColor)
      .replace(/\s+/g, ' ')
      .trim();
    if (!vb || !body.includes('<path')) { continue; }
    out[key] = { vb, body };
    console.log('OK ', key, '<-', name);
    done = true;
    break;
  }
  if (!done) missing.push(key);
}
console.log('version:', version.version, '| total:', Object.keys(out).length, '| missing:', missing.join(',') || 'none');

const ts = `/**
 * Официальные Material Symbols (outlined, weight 400) — те же глифы, что у клона
 * (Icons.Outlined.* в tennis-ai-android и MaterialIcons-шрифт оригинала).
 * Сгенерировано icon-fetch.mjs из @material-symbols/svg-400@${version.version}
 * (Apache-2.0, https://github.com/google/material-design-icons).
 * viewBox у Material Symbols — «0 -960 960 960», пути в 40-кратном масштабе.
 */

export interface MaterialIcon {
  vb: string;
  body: string;
}

export const MATERIAL_ICONS: Record<string, MaterialIcon> = ${JSON.stringify(out, null, 2)};
`;
await import('node:fs').then((fs) => fs.promises.writeFile('src/components/materialIcons.ts', ts));
