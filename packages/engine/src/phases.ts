import type { DetectedPhase, ObjectDetection, PhaseCode, PoseFrame, StrokeType } from '@tennis-ai/shared';
import { boxCenter, dist } from './geometry';

// Phase names — verbatim port from the reverse engineering (moment_detector.dart / results screen).
export const PHASE_NAMES: Record<StrokeType, Record<PhaseCode, string>> = {
  serve: {
    M0: 'Ready',
    M1: 'Stance',
    M2: 'Toss & Backswing',
    M3: 'Loading / Trophy',
    M4: 'Extension',
    M5: 'Contact Point',
    M6: 'Recovery',
  },
  forehand: {
    M0: 'Ready Position',
    M1: 'Directional Split Step',
    M2: 'Footwork / Unit Turn',
    M3: 'Drive / Trophy',
    M4: 'Contact Point',
    M5: 'Follow-Through',
    M6: 'Recovery',
  },
  backhand: {
    M0: 'Ready Position',
    M1: 'Directional Split Step',
    M2: 'Footwork / Unit Turn',
    M3: 'Drive / Trophy',
    M4: 'Contact Point',
    M5: 'Follow-Through',
    M6: 'Recovery',
  },
};

/**
 * Contact frame: the frame with the minimal distance between ball and racket box centers.
 * Port of the [YOLO-REPASSE]/racket_x1..y2 idea from the reverse engineering;
 * the exact original logic was not recovered.
 */
export function detectContactFrame(detections: ObjectDetection[]): number | null {
  const byFrame = new Map<number, { ball?: { x: number; y: number }; racket?: { x: number; y: number } }>();
  for (const d of detections) {
    const entry = byFrame.get(d.frameIndex) ?? {};
    entry[d.label === 'ball' ? 'ball' : 'racket'] = boxCenter(d.box);
    byFrame.set(d.frameIndex, entry);
  }
  let best: { frame: number; d: number } | null = null;
  for (const [frame, { ball, racket }] of byFrame) {
    if (!ball || !racket) continue;
    const d = dist(ball, racket);
    if (!best || d < best.d) best = { frame, d };
  }
  return best ? best.frame : null;
}

/**
 * Heuristic v0: M5 (contact) comes from ball/racket detection, the other phases are
 * interpolated by relative shares of the window. Replaced by the ported
 * moment_detector_ondevice logic (see docs/PORTING.md).
 */
export function heuristicPhases(strokeType: StrokeType, frameCount: number, contactFrame: number | null): DetectedPhase[] {
  const n = Math.max(1, frameCount);
  const c = contactFrame === null ? Math.floor(n * 0.6) : Math.min(n - 1, Math.max(1, contactFrame));
  const pre = c; // backswing duration
  const post = n - c; // follow-through duration

  const cuts: Array<[PhaseCode, number, number]> =
    strokeType === 'serve'
      ? [
          ['M0', 0, 0.25],
          ['M1', 0.25, 0.45],
          ['M2', 0.45, 0.7],
          ['M3', 0.7, 0.9],
          ['M4', 0.9, 1],
        ]
      : [
          ['M0', 0, 0.2],
          ['M1', 0.2, 0.4],
          ['M2', 0.4, 0.65],
          ['M3', 0.65, 0.9],
          ['M4', 0.9, 1],
        ];

  const phases: DetectedPhase[] = cuts.map(([code, f0, f1], idx) => ({
    code,
    startFrame: idx === 0 ? 0 : Math.round(f0 * pre),
    endFrame: Math.max(idx === 0 ? 1 : Math.round(f0 * pre) + 1, Math.round(f1 * pre)),
    ...(code === 'M4' ? {} : {}),
  }));

  // M5 — contact, M6 — finish
  const contactEnd = Math.min(n - 1, c + Math.round(post * 0.3));
  phases.push({ code: 'M5', startFrame: Math.max(0, c - 1), endFrame: Math.max(c, contactEnd), contactFrame: c });
  phases.push({ code: 'M6', startFrame: contactEnd + 1 <= n - 1 ? contactEnd + 1 : n - 1, endFrame: n - 1 });

  // Remove negative/crossing boundaries
  for (const p of phases) {
    p.startFrame = Math.max(0, Math.min(p.startFrame, n - 1));
    p.endFrame = Math.max(p.startFrame + 1 > n - 1 ? p.startFrame : p.startFrame + 1, Math.min(p.endFrame, n - 1));
  }
  return phases;
}
