import type { LandmarkPoint, PoseFrame } from '@tennis-ai/shared';
import type { PoseLandmarker, PoseLandmarkerResult } from '@mediapipe/tasks-vision';

/**
 * MediaPipe Pose Landmarker (порт pose_service.dart: ML Kit Pose, 33 landmark со сглаживанием).
 * Режим VIDEO даёт встроенный темпоральный трекинг между кадрами — аналог ML Kit streaming.
 * Пакет @mediapipe/tasks-vision импортируется динамически (отдельный чанк, не в основном бандле);
 * WASM-рантайм и модель по умолчанию с CDN, для прода самохостинг через VITE_MEDIAPIPE_WASM_URL
 * и VITE_POSE_MODEL_URL (см. docs/PORTING.md).
 */

const WASM_BASE_URL =
  import.meta.env.VITE_MEDIAPIPE_WASM_URL ?? 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@1.1.1/wasm';
const POSE_MODEL_URL =
  import.meta.env.VITE_POSE_MODEL_URL ??
  'https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task';

/** Порядок landmark'ов MediaPipe Pose совпадает с константами LM в packages/engine/src/landmarks.ts. */
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
    // WebView без GPU-делегата — CPU-инференс на WASM SIMD
    return await vision.PoseLandmarker.createFromOptions(fileset, options('CPU'));
  }
}

/** Ленивый синглтон: один landmarker на сессию (VIDEO-режим требует монотонные таймстампы). */
export function loadPoseLandmarker(): Promise<PoseLandmarker> {
  landmarkerPromise ??= createLandmarker().catch((err: unknown) => {
    landmarkerPromise = null; // разрешить повторную попытку при следующем вызове
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

/** MediaPipe-результат кадра → PoseFrame из shared; кадр без позы = 33 landmark с visibility 0. */
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

/** Кадр годен для анализа, если видимо «ядро» тела (плечи + бёдра) — калькуляторы engine от них зависят. */
export function hasDetectablePose(frame: PoseFrame): boolean {
  return [11, 12, 23, 24].every((i) => (frame.landmarks[i]?.visibility ?? 0) >= 0.5);
}
