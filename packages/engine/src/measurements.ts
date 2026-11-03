import type { Calibration, DetectedPhase, ObjectDetection, PhaseCode, PoseFrame } from '@tennis-ai/shared';
import { angleAtDeg, boxCenter, dist, linesAngleDeg, mid, lineToHorizontalDeg, lineToVerticalDeg } from './geometry';
import { LM, lmPx, sideIndex, type Side } from './landmarks';

export interface CalcContext {
  frames: PoseFrame[];
  /** Phase the metric is attached to (the originating phase from the metric definition). */
  phase: PhaseCode;
  phases: DetectedPhase[];
  detections: ObjectDetection[];
  calibration: Calibration;
  /** Frame rate of the source video — needed by speed metrics. */
  fps: number;
}

export type MeasurementFn = (ctx: CalcContext) => number | null;

function phaseFrameIndex(frames: PoseFrame[], phases: DetectedPhase[], phase: PhaseCode): number {
  const p = phases.find((x) => x.code === phase);
  if (p) return Math.floor((p.startFrame + p.endFrame) / 2);
  return Math.floor(frames.length / 2);
}

function sideOf(calibration: Calibration): Side {
  return calibration.handedness === 'left' ? 'left' : 'right';
}

function opposite(side: Side): Side {
  return side === 'left' ? 'right' : 'left';
}

function angle(frames: PoseFrame[], i: number, a: number, v: number, b: number): number | null {
  const f = frames[i];
  if (!f) return null;
  const pa = lmPx(f, a);
  const pv = lmPx(f, v);
  const pb = lmPx(f, b);
  return pa && pv && pb ? angleAtDeg(pa, pv, pb) : null;
}

// ---- Calculator registry: name from metrics.json → measurement function ----

export const CALCULATORS: Record<string, MeasurementFn> = {
  feetShoulderRatio(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const al = lmPx(f, LM.LEFT_ANKLE);
    const ar = lmPx(f, LM.RIGHT_ANKLE);
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    const shoulder = sl && sr ? dist(sl, sr) : null;
    return al && ar && shoulder && shoulder > 0 ? dist(al, ar) / shoulder : null;
  },

  frontFootAngleDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const side = sideOf(ctx.calibration);
    const heel = lmPx(f, sideIndex(side, 'HEEL'));
    const toe = lmPx(f, sideIndex(side, 'FOOT_INDEX'));
    // v0 approximation: the baseline is assumed horizontal in the frame.
    return heel && toe ? lineToHorizontalDeg(heel, toe) : null;
  },

  armsPositionDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const l = angle(ctx.frames, i, LM.LEFT_SHOULDER, LM.LEFT_ELBOW, LM.LEFT_WRIST);
    const r = angle(ctx.frames, i, LM.RIGHT_SHOULDER, LM.RIGHT_ELBOW, LM.RIGHT_WRIST);
    return l !== null && r !== null ? (l + r) / 2 : (l ?? r);
  },

  kneeFlexionDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const l = angle(ctx.frames, i, LM.LEFT_HIP, LM.LEFT_KNEE, LM.LEFT_ANKLE);
    const r = angle(ctx.frames, i, LM.RIGHT_HIP, LM.RIGHT_KNEE, LM.RIGHT_ANKLE);
    return l !== null && r !== null ? (l + r) / 2 : (l ?? r);
  },

  domKneeFlexionDeg(ctx) {
    const side = sideOf(ctx.calibration);
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'HIP'), sideIndex(side, 'KNEE'), sideIndex(side, 'ANKLE'));
  },

  ndomKneeFlexionDeg(ctx) {
    const side = opposite(sideOf(ctx.calibration));
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'HIP'), sideIndex(side, 'KNEE'), sideIndex(side, 'ANKLE'));
  },

  shoulderHipSeparationDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    const hl = lmPx(f, LM.LEFT_HIP);
    const hr = lmPx(f, LM.RIGHT_HIP);
    return sl && sr && hl && hr ? linesAngleDeg(sl, sr, hl, hr) : null;
  },

  hipExtensionDeg(ctx) {
    const side = sideOf(ctx.calibration);
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'ANKLE'), sideIndex(side, 'HIP'), sideIndex(side, 'SHOULDER'));
  },

  heelLegAngleDeg(ctx) {
    const side = sideOf(ctx.calibration);
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'HEEL'), sideIndex(side, 'HIP'), sideIndex(side, 'SHOULDER'));
  },

  trunkTiltDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    const hl = lmPx(f, LM.LEFT_HIP);
    const hr = lmPx(f, LM.RIGHT_HIP);
    if (!sl || !sr || !hl || !hr) return null;
    return lineToVerticalDeg(mid(hl, hr), mid(sl, sr));
  },

  shouldersHorizontalTiltDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    return sl && sr ? lineToHorizontalDeg(sl, sr) : null;
  },

  trunkRotationCm(ctx) {
    // v0 approximation: difference between shoulder width and hip width × scale.
    // The exact original formula from Dart AOT was not recovered.
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    const cm = ctx.calibration.cmPerPixel;
    if (!f || !cm) return null;
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    const hl = lmPx(f, LM.LEFT_HIP);
    const hr = lmPx(f, LM.RIGHT_HIP);
    if (!sl || !sr || !hl || !hr) return null;
    return Math.abs(dist(sl, sr) - dist(hl, hr)) * cm;
  },

  tossArmElbowDeg(ctx) {
    const side = opposite(sideOf(ctx.calibration));
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'SHOULDER'), sideIndex(side, 'ELBOW'), sideIndex(side, 'WRIST'));
  },

  tossArmWristSpeed(ctx) {
    return peakWristSpeed(ctx, opposite(sideOf(ctx.calibration)), ctx.phase);
  },

  dominantWristSpeed(ctx) {
    return peakWristSpeed(ctx, sideOf(ctx.calibration), 'M5');
  },

  tossHeightRatio(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const side = opposite(sideOf(ctx.calibration));
    const head = lmPx(f, LM.NOSE);
    let ankle: { x: number; y: number } | null = null;
    for (const fr of ctx.frames) {
      const p = lmPx(fr, LM.LEFT_ANKLE) ?? lmPx(fr, LM.RIGHT_ANKLE);
      if (p && (!ankle || p.y > ankle.y)) ankle = p; // lowest foot point — base of the height measurement
    }
    const wrist = lmPx(f, sideIndex(side, 'WRIST'));
    if (!head || !ankle || !wrist) return null;
    const bodyHeight = ankle.y - head.y;
    if (bodyHeight <= 0) return null;
    return (ankle.y - wrist.y) / bodyHeight;
  },

  contactArmAlignmentDeg(ctx) {
    const side = sideOf(ctx.calibration);
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    return angle(ctx.frames, i, sideIndex(side, 'SHOULDER'), sideIndex(side, 'ELBOW'), sideIndex(side, 'WRIST'));
  },

  armToVerticalDeg(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    if (!f) return null;
    const side = sideOf(ctx.calibration);
    const sh = lmPx(f, sideIndex(side, 'SHOULDER'));
    const el = lmPx(f, sideIndex(side, 'ELBOW'));
    return sh && el ? lineToVerticalDeg(sh, el) : null;
  },

  ballShoulderDistanceCm(ctx) {
    const i = phaseFrameIndex(ctx.frames, ctx.phases, ctx.phase);
    const f = ctx.frames[i];
    const cm = ctx.calibration.cmPerPixel;
    if (!f || !cm) return null;
    const ball = ctx.detections.find((d) => d.label === 'ball' && d.frameIndex === i);
    if (!ball) return null;
    const sl = lmPx(f, LM.LEFT_SHOULDER);
    const sr = lmPx(f, LM.RIGHT_SHOULDER);
    if (!sl || !sr) return null;
    const c = boxCenter(ball.box);
    return dist({ x: c.x * f.width, y: c.y * f.height }, mid(sl, sr)) * cm;
  },
};

/** Peak wrist speed (m/s) around the phase: per-frame displacement × fps × scale. */
function peakWristSpeed(ctx: CalcContext, side: Side, phase: PhaseCode): number | null {
  const cm = ctx.calibration.cmPerPixel;
  if (!cm) return null;
  const p = ctx.phases.find((x) => x.code === phase);
  const start = p ? Math.max(1, p.startFrame) : 1;
  const end = p ? Math.min(ctx.frames.length - 1, p.endFrame) : ctx.frames.length - 1;
  const wristIdx = sideIndex(side, 'WRIST');
  let peak: number | null = null;
  for (let i = start; i <= end; i++) {
    const a = lmPx(ctx.frames[i - 1], wristIdx);
    const b = lmPx(ctx.frames[i], wristIdx);
    if (!a || !b) continue;
    const v = (dist(a, b) * cm * ctx.fps) / 100; // px → cm → m; per frame × fps → m/s
    peak = peak === null ? v : Math.max(peak, v);
  }
  return peak;
}
