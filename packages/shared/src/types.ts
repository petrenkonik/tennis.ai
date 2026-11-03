// Domain types shared by the Mini App, API and CV engine.
// Ported from reverse engineering of com.tennis.ai v2.0.19 (reference/ANALYSIS_FINDINGS.md).

export type StrokeType = 'serve' | 'forehand' | 'backhand';
export type Handedness = 'left' | 'right';
export type PhaseCode = 'M0' | 'M1' | 'M2' | 'M3' | 'M4' | 'M5' | 'M6';
export type MetricStatus = 'green' | 'red' | 'unknown';
export type MeasurementUnit = 'deg' | 'cm' | 'ratio' | 'm_s';

/** MediaPipe Pose Landmarker: 33 landmarks, x/y normalized to frame width/height, z relative to the hips. */
export interface LandmarkPoint {
  x: number; // 0..1 of frame width
  y: number; // 0..1 of frame height
  z?: number;
  visibility?: number; // 0..1
}

export interface PoseFrame {
  frameIndex: number;
  timestampMs: number;
  /** Pixel size of the source frame — normalized coordinates must be scaled before angular calculations. */
  width: number;
  height: number;
  landmarks: LandmarkPoint[]; // length 33
}

export interface BoundingBox {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

export interface ObjectDetection {
  frameIndex: number;
  label: 'ball' | 'racket';
  confidence: number;
  box: BoundingBox; // normalized coordinates
}

export interface DetectedPhase {
  code: PhaseCode;
  startFrame: number;
  endFrame: number;
  /** The contact frame lives inside a phase (M5) — used for the annotation overlay. */
  contactFrame?: number;
  confidence?: number;
}

export interface Calibration {
  handedness: Handedness;
  /** Physical scale from calibration; without it, cm / m·s metrics stay unknown. */
  cmPerPixel?: number;
  playerHeightCm?: number;
}

export interface ProRange {
  min?: number;
  max?: number;
}

/**
 * Metric definition. Thresholds recovered from the original's libapp.so.
 * proRange: null = the range is served from Firestore `metric_definitions` at runtime
 * and was not recovered from the APK (see ANALYSIS_FINDINGS.md, "Analysis limitations").
 */
export interface MetricDefinition {
  code: string;
  label: string;
  unit: MeasurementUnit;
  strokeTypes: StrokeType[];
  phase: PhaseCode;
  proRange: ProRange | null;
  description: string | null;
  /** Correction templates with the {value} placeholder. */
  corrections: { tooLow: string | null; tooHigh: string | null };
  /** Key of the calculator function in the engine registry; null = not implemented yet. */
  calculator: string | null;
  confidence: 'recovered' | 'partial' | 'unknown-range';
}

export interface MetricEvaluation {
  code: string;
  label: string;
  phase: PhaseCode;
  unit: MeasurementUnit;
  value: number | null;
  status: MetricStatus;
  displayValue: string;
  proRangeLabel: string;
  correction: string | null;
  confidence: MetricDefinition['confidence'];
}

export interface OverallScore {
  /** 0..100 — share of green metrics (v1 approximation: exact original weights not recovered, DECISIONS §6.4). */
  score: number | null;
  greens: number;
  reds: number;
  unknown: number;
}

export interface AnalysisOutcome {
  strokeType: StrokeType;
  fps: number;
  frameCount: number;
  phases: DetectedPhase[];
  metrics: MetricEvaluation[];
  overall: OverallScore;
  /** Weakness codes sorted by distance from the pro range. */
  topWeaknesses: string[];
  notes: string[];
}
