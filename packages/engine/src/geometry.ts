// 2D geometry in the pixel space of the frame.
// IMPORTANT: angles are computed in pixels (x*W, y*H), not in normalized
// coordinates — otherwise frame aspect-ratio distortion breaks all angles.

export interface Pt {
  x: number;
  y: number;
}

const DEG = 180 / Math.PI;

/** Angle (0..180°) at vertex v between rays v→a and v→b. */
export function angleAtDeg(a: Pt, v: Pt, b: Pt): number {
  const v1x = a.x - v.x;
  const v1y = a.y - v.y;
  const v2x = b.x - v.x;
  const v2y = b.y - v.y;
  const n = Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y);
  if (n === 0) return 0;
  let c = (v1x * v2x + v1y * v2y) / n;
  c = Math.max(-1, Math.min(1, c));
  return Math.acos(c) * DEG;
}

/** Angle of line a→b relative to horizontal, normalized to 0..90°. */
export function lineToHorizontalDeg(a: Pt, b: Pt): number {
  const deg = Math.atan2(Math.abs(b.y - a.y), Math.abs(b.x - a.x)) * DEG;
  return deg > 90 ? 180 - deg : deg;
}

/** Angle of line a→b relative to vertical, normalized to 0..90°. */
export function lineToVerticalDeg(a: Pt, b: Pt): number {
  const deg = Math.atan2(Math.abs(b.x - a.x), Math.abs(b.y - a.y)) * DEG;
  return deg > 90 ? 180 - deg : deg;
}

/** Angle between two lines (a1→b1 and a2→b2), normalized to 0..90°. */
export function linesAngleDeg(a1: Pt, b1: Pt, a2: Pt, b2: Pt): number {
  const ang1 = Math.atan2(b1.y - a1.y, b1.x - a1.x);
  const ang2 = Math.atan2(b2.y - a2.y, b2.x - a2.x);
  let d = Math.abs(ang1 - ang2) * DEG;
  if (d > 180) d = 360 - d;
  return d > 90 ? 180 - d : d;
}

export function dist(a: Pt, b: Pt): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

export function mid(a: Pt, b: Pt): Pt {
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
}

export function boxCenter(b: { x1: number; y1: number; x2: number; y2: number }): Pt {
  return { x: (b.x1 + b.x2) / 2, y: (b.y1 + b.y2) / 2 };
}
