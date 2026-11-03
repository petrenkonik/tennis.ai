/// <reference lib="webworker" />
import { runAnalysis } from '@tennis-ai/engine';
import type { AnalysisOutcome, Calibration, ObjectDetection, PoseFrame, StrokeType } from '@tennis-ai/shared';

/**
 * Воркер анализа (DECISIONS §7.2: «Анализ целиком on-device в packages/engine»).
 * Вход: последовательность pose-кадров + детекции мяча/ракетки + тип удара.
 * Интеграция MediaPipe Pose Landmarker / onnxruntime-web живёт в src/services/videoPipeline.ts
 * и передаёт сюда уже готовые данные.
 */

interface AnalyzeRequest {
  type: 'analyze';
  pose: PoseFrame[];
  detections: ObjectDetection[];
  strokeType: StrokeType;
  fps: number;
  calibration: Calibration;
}

export type AnalyzeResponse = { type: 'outcome'; outcome: AnalysisOutcome };

const ctx = self as unknown as DedicatedWorkerGlobalScope;

ctx.onmessage = (e: MessageEvent<AnalyzeRequest>): void => {
  if (e.data?.type !== 'analyze') return;
  const outcome = runAnalysis(e.data);
  ctx.postMessage({ type: 'outcome', outcome } satisfies AnalyzeResponse);
};
