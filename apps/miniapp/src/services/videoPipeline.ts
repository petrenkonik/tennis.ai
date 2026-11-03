import type { ObjectDetection, PoseFrame } from '@tennis-ai/shared';
import { disposePoseLandmarker, hasDetectablePose, loadPoseLandmarker, toPoseFrame } from './poseService';

/**
 * Client-side CV pipeline (port of on_device_analysis_service.dart, DECISIONS §7.2):
 *   1. analyzeVideoPoses — streaming: frames at fps=30, scale≈640px → Pose Landmarker → PoseFrame[]
 *      (frames are not buffered: seek → canvas → detect; only a ~2 KB PoseFrame goes into memory)
 *   2. detectObjects     — TODO: YOLO11 ONNX (onnxruntime-web, int8): ball + racket; [] for now —
 *                          the engine degrades to heuristic phases (analyze.ts notes)
 *   3. runAnalysis       — implemented in packages/engine, called from the analysis.worker.ts worker
 */

export interface ExtractOptions {
  /** Frame sampling rate (original: 30 fps). */
  targetFps?: number;
  /** Target frame width (original: scale=640:-2). */
  maxWidth?: number;
  /** Hard limit of frames per session (5 s × 30 fps = 150). */
  maxFrames?: number;
  /** Start of the trim window from VIDEO PREVIEW, seconds (port of the original's trimStartSec). */
  startSec?: number;
  /** End of the trim window, seconds; undefined = until the end of the video. */
  endSec?: number;
}

export interface VideoPosesResult {
  /** All sampled frames; frames without a detected pose contain a landmark with visibility 0 and are ignored by the engine. */
  poses: PoseFrame[];
  frameWidth: number;
  frameHeight: number;
  fps: number;
  /** How many frames actually detected a pose (body core visible) — 0 means "video without a person". */
  poseFrames: number;
  totalFrames: number;
}

export async function analyzeVideoPoses(
  file: File | Blob,
  opts: ExtractOptions & { onProgress?: (done: number, total: number) => void } = {},
): Promise<VideoPosesResult> {
  const targetFps = opts.targetFps ?? 30;
  const maxWidth = opts.maxWidth ?? 640;
  const maxFrames = opts.maxFrames ?? 150;

  const landmarker = await loadPoseLandmarker();
  const url = URL.createObjectURL(file);
  const video = document.createElement('video');
  video.muted = true;
  video.playsInline = true;
  video.src = url;

  try {
    await new Promise<void>((resolve, reject) => {
      video.onloadedmetadata = () => resolve();
      video.onerror = () => reject(new Error('video load failed'));
    });

    const scale = Math.min(1, maxWidth / (video.videoWidth || maxWidth));
    const w = Math.round((video.videoWidth || maxWidth) * scale);
    const h = Math.round((video.videoHeight || maxWidth) * scale);
    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx2d = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx2d) throw new Error('canvas 2d context unavailable');

    const stepMs = 1000 / targetFps;
    // Some webm streams report duration = Infinity — fall back to the frame limit
    const durationSec = Number.isFinite(video.duration) ? video.duration : maxFrames / targetFps;
    // Trim window: only it is sampled; session duration is computed from the window (like _previewTrim)
    const fromSec = Math.min(Math.max(opts.startSec ?? 0, 0), Math.max(0, durationSec - 0.1));
    const toSec = Math.min(opts.endSec ?? durationSec, durationSec);
    const windowSec = Math.max(0.2, toSec - fromSec);
    const total = Math.min(maxFrames, Math.max(1, Math.ceil(windowSec * targetFps)));
    const poses: PoseFrame[] = [];
    let poseFrames = 0;

    for (let i = 0; i < total; i++) {
      await seek(video, fromSec + (i * stepMs) / 1000);
      ctx2d.drawImage(video, 0, 0, w, h);
      const result = landmarker.detectForVideo(canvas, i * stepMs);
      const frame = toPoseFrame(i, i * stepMs, w, h, result);
      poses.push(frame);
      if (hasDetectablePose(frame)) poseFrames++;
      opts.onProgress?.(i + 1, total);
    }

    return { poses, frameWidth: w, frameHeight: h, fps: targetFps, poseFrames, totalFrames: poses.length };
  } finally {
    URL.revokeObjectURL(url);
    await disposePoseLandmarker();
  }
}

function seek(video: HTMLVideoElement, timeSec: number): Promise<void> {
  return new Promise((resolve, reject) => {
    const onSeeked = (): void => {
      cleanup();
      resolve();
    };
    const onError = (): void => {
      cleanup();
      reject(new Error('seek failed'));
    };
    const cleanup = (): void => {
      video.removeEventListener('seeked', onSeeked);
      video.removeEventListener('error', onError);
    };
    video.addEventListener('seeked', onSeeked);
    video.addEventListener('error', onError);
    video.currentTime = timeSec;
  });
}

/**
 * TODO(step 3): YOLO11 ONNX → ball and racket detections (port of yolo_detector_service /
 * ball_detection_service; re-pass on a miss — [YOLO-REPASSE]). Empty for now: the engine
 * builds heuristic phases and flags that in the analysis result notes.
 */
export async function detectObjects(): Promise<ObjectDetection[]> {
  return [];
}
