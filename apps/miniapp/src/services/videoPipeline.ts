import type { ObjectDetection, PoseFrame } from '@tennis-ai/shared';
import { disposePoseLandmarker, hasDetectablePose, loadPoseLandmarker, toPoseFrame } from './poseService';

/**
 * Клиентский CV-пайплайн (порт on_device_analysis_service.dart, DECISIONS §7.2):
 *   1. analyzeVideoPoses — потоково: кадры fps=30, scale≈640px → Pose Landmarker → PoseFrame[]
 *      (кадры не буферизуются: seek → canvas → detect → в память идёт только PoseFrame ~2 КБ)
 *   2. detectObjects     — TODO: YOLO11 ONNX (onnxruntime-web, int8): мяч + ракетка; пока [] —
 *                          engine деградирует на эвристические фазы (analyze.ts notes)
 *   3. runAnalysis       — реализовано в packages/engine, вызывается из воркера analysis.worker.ts
 */

export interface ExtractOptions {
  /** Частота выборки кадров (оригинал: 30 fps). */
  targetFps?: number;
  /** Целевая ширина кадра (оригинал: scale=640:-2). */
  maxWidth?: number;
  /** Жёсткий лимит кадров на сессию (5 c × 30 fps = 150). */
  maxFrames?: number;
  /** Начало окна трима из VIDEO PREVIEW, сек (порт trimStartSec оригинала). */
  startSec?: number;
  /** Конец окна трима, сек; undefined = до конца видео. */
  endSec?: number;
}

export interface VideoPosesResult {
  /** Все кадры выборки; кадры без найденной позы содержат landmark с visibility 0 и игнорируются engine. */
  poses: PoseFrame[];
  frameWidth: number;
  frameHeight: number;
  fps: number;
  /** Сколько кадров с реально найденной позой (ядро тела видимо) — 0 значит «видео без человека». */
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
    // Некоторые webm-потоки отдают duration = Infinity — fallback на лимит кадров
    const durationSec = Number.isFinite(video.duration) ? video.duration : maxFrames / targetFps;
    // Окно трима: сэмплируем только его; длительность сессии считается от окна (как _previewTrim)
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
 * TODO(step 3): YOLO11 ONNX → детекции мяча и ракетки (порт yolo_detector_service /
 * ball_detection_service; репасс при промахе — [YOLO-REPASSE]). Пока пусто: engine строит
 * эвристические фазы и помечает это в notes результата анализа.
 */
export async function detectObjects(): Promise<ObjectDetection[]> {
  return [];
}
