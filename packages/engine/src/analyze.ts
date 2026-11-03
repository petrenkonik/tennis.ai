import { metricsForStroke, type AnalysisOutcome, type Calibration, type ObjectDetection, type PoseFrame, type StrokeType } from '@tennis-ai/shared';
import { evaluateMetric } from './metrics';
import { CALCULATORS, type CalcContext } from './measurements';
import { detectContactFrame, heuristicPhases } from './phases';
import { computeOverall, topWeaknesses } from './scoring';

export { PHASE_NAMES, detectContactFrame, heuristicPhases } from './phases';
export { CALCULATORS } from './measurements';
export { computeOverall, topWeaknesses } from './scoring';
export { evaluateMetric, formatRange, formatValue } from './metrics';

export interface AnalysisInput {
  pose: PoseFrame[];
  detections: ObjectDetection[];
  strokeType: StrokeType;
  fps: number;
  calibration: Calibration;
}

/**
 * Полный прогон анализа (v0):
 *   pose-последовательность + детекции → фазы M0–M6 → метрики → green/red → overall_score.
 * Выполняется целиком на клиенте (Web Worker Mini App), как в оригинале v2.0.19.
 */
export function runAnalysis(input: AnalysisInput): AnalysisOutcome {
  const { pose, detections, strokeType, fps, calibration } = input;
  const contactFrame = detectContactFrame(detections);
  const phases = heuristicPhases(strokeType, pose.length, contactFrame);

  const metrics = metricsForStroke(strokeType).map((def) => {
    const calc = def.calculator ? CALCULATORS[def.calculator] : undefined;
    if (!calc) return evaluateMetric(def, null, { reason: 'calculator not implemented in v0 (see docs/PORTING.md)' });
    const ctx: CalcContext = { frames: pose, phase: def.phase, phases, detections, calibration, fps };
    let value: number | null = null;
    try {
      value = calc(ctx);
    } catch {
      value = null;
    }
    return evaluateMetric(def, value);
  });

  return {
    strokeType,
    fps,
    frameCount: pose.length,
    phases,
    metrics,
    overall: computeOverall(metrics),
    topWeaknesses: topWeaknesses(metrics),
    notes: [
      'Phases: heuristic v0 — contact from ball↔racket distance, other phases interpolated. Replace with ported moment_detector logic.',
      'Scoring: equal-weight share of green metrics; original weights not recovered (DECISIONS §6.4).',
      ...(contactFrame === null ? ['Contact frame not detected: no overlapping ball+racket detections.'] : []),
    ],
  };
}
