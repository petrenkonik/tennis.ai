import type { LandmarkPoint, PoseFrame } from '@tennis-ai/shared';
import type { PoseLandmarker, PoseLandmarkerResult } from '@mediapipe/tasks-vision';

/**
 * MediaPipe Pose Landmarker (port of pose_service.dart: ML Kit Pose, 33 landmarks with smoothing).
 * VIDEO mode provides built-in temporal tracking between frames — an analogue of ML Kit streaming.
 * The @mediapipe/tasks-vision package is imported dynamically (a separate chunk, not in the main bundle);
 * the WASM runtime and model come from a CDN by default; for production, self-host via
 * VITE_MEDIAPIPE_WASM_URL and VITE_POSE_MODEL_URL (see docs/PORTING.md).
 */

const WASM_BASE_URL =
  import.meta.env.VITE_MEDIAPIPE_WASM_URL ?? 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.1.1/wasm';
const POSE_MODEL_URL =
  import.meta.env.VITE_POSE_MODEL_URL ??
  'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task';

/** The MediaPipe Pose landmark order matches the LM constants in packages/engine/src/landmarks.ts. */
export const LANDMARK_COUNT = 33;

let landmarkerPromise: Promise<PoseLandmarker> | null = null;

async function createLandmarker(): Promise<PoseLandmarker> {
  const vision = await import('@mediapipe/tasks-vision');
  const fileset = await vision.FilesetResolver.forVisionTasks(WASM_BASE_URL);
  const options = (delegate: 'GPU' | 'CPU') => ({
    baseOptions: { modelAssetPath: POSE_MODEL_URL, delegate },
    runningMode: 'VIDEO' as const,
    numPoses: 1,
    minPoseDetectionConfidence: 0.5,
    minPosePresenceConfidence: 0.5,
    minTrackingConfidence: 0.5,
    outputSegmentationMasks: false,
  });
  try {
    return await vision.PoseLandmarker.createFromOptions(fileset, options('GPU'));
  } catch {
    // WebView without a GPU delegate — CPU inference on WASM SIMD
    return await vision.PoseLandmarker.createFromOptions(fileset, options('CPU'));
  }
}

/** Lazy singleton: one landmarker per session (VIDEO mode requires monotonic timestamps). */
export function loadPoseLandmarker(): Promise<PoseLandmarker> {
  landmarkerPromise ??= createLandmarker().catch((err: unknown) => {
    landmarkerPromise = null; // allow a retry on the next call
    throw err;
  });
  return landmarkerPromise;
}

export async function disposePoseLandmarker(): Promise<void> {
  if (!landmarkerPromise) return;
  const landmarker = await landmarkerPromise;
  landmarker.close();
  landmarkerPromise = null;
}

/** MediaPipe frame result → PoseFrame from shared; a frame without a pose = 33 landmarks with visibility 0. */
export function toPoseFrame(
  frameIndex: number,
  timestampMs: number,
  width: number,
  height: number,
  result: PoseLandmarkerResult | null,
): PoseFrame {
  const detected = result?.landmarks[0];
  const landmarks: LandmarkPoint[] = [];
  for (let i = 0; i < LANDMARK_COUNT; i++) {
    const lm = detected?.[i];
    landmarks.push(lm ? { x: lm.x, y: lm.y, z: lm.z, visibility: lm.visibility ?? 1 } : { x: 0, y: 0, visibility: 0 });
  }
  return { frameIndex, timestampMs, width, height, landmarks };
}

/** A frame is fit for analysis if the body "core" (shoulders + hips) is visible — engine calculators depend on them. */
export function hasDetectablePose(frame: PoseFrame): boolean {
  return [11, 12, 23, 24].every((i) => (frame.landmarks[i]?.visibility ?? 0) >= 0.5);
}
