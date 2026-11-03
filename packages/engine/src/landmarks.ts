import type { PoseFrame } from '@tennis-ai/shared';
import type { Pt } from './geometry';

// Indices of the 33 landmarks of the MediaPipe Pose Landmarker model (same as the original's ML Kit).
export const LM = {
  NOSE: 0,
  LEFT_EYE_INNER: 1,
  LEFT_EYE: 2,
  LEFT_EYE_OUTER: 3,
  RIGHT_EYE_INNER: 4,
  RIGHT_EYE: 5,
  RIGHT_EYE_OUTER: 6,
  LEFT_EAR: 7,
  RIGHT_EAR: 8,
  MOUTH_LEFT: 9,
  MOUTH_RIGHT: 10,
  LEFT_SHOULDER: 11,
  RIGHT_SHOULDER: 12,
  LEFT_ELBOW: 13,
  RIGHT_ELBOW: 14,
  LEFT_WRIST: 15,
  RIGHT_WRIST: 16,
  LEFT_PINKY: 17,
  RIGHT_PINKY: 18,
  LEFT_INDEX: 19,
  RIGHT_INDEX: 20,
  LEFT_THUMB: 21,
  RIGHT_THUMB: 22,
  LEFT_HIP: 23,
  RIGHT_HIP: 24,
  LEFT_KNEE: 25,
  RIGHT_KNEE: 26,
  LEFT_ANKLE: 27,
  RIGHT_ANKLE: 28,
  LEFT_HEEL: 29,
  RIGHT_HEEL: 30,
  LEFT_FOOT_INDEX: 31,
  RIGHT_FOOT_INDEX: 32,
} as const;

export type Side = 'left' | 'right';

const MIN_VISIBILITY = 0.3;

/** Landmark in normalized coordinates, or null when visibility is low. */
export function lmNorm(f: PoseFrame, index: number): Pt | null {
  const p = f.landmarks[index];
  if (!p) return null;
  if (p.visibility !== undefined && p.visibility < MIN_VISIBILITY) return null;
  return { x: p.x, y: p.y };
}

/** Landmark in frame pixels — the base space for all angular measurements. */
export function lmPx(f: PoseFrame, index: number): Pt | null {
  const p = lmNorm(f, index);
  if (!p) return null;
  return { x: p.x * f.width, y: p.y * f.height };
}

export function sideIndex(side: Side, base: 'SHOULDER' | 'ELBOW' | 'WRIST' | 'HIP' | 'KNEE' | 'ANKLE' | 'HEEL' | 'FOOT_INDEX'): number {
  return side === 'left' ? LM[`LEFT_${base}`] : LM[`RIGHT_${base}`];
}
