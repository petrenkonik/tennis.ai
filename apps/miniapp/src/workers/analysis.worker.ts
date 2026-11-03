/// <reference lib="webworker" />
import { runAnalysis } from '@tennis-ai/engine';
import type { AnalysisOutcome, Calibration, ObjectDetection, PoseFrame, StrokeType } from '@tennis-ai/shared';

/**
 * Analysis worker (DECISIONS §7.2: "the analysis is fully on-device in packages/engine").
 * Input: a sequence of pose frames + ball/racket detections + stroke type.
 * The MediaPipe Pose Landmarker / onnxruntime-web integration lives in src/services/videoPipeline.ts
 * and passes ready-made data here.
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
