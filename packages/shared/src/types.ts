// Доменные типы, общие для Mini App, API и CV-движка.
// Портировано из реверс-инжиниринга com.tennis.ai v2.0.19 (reference/ANALYSIS_FINDINGS.md).

export type StrokeType = 'serve' | 'forehand' | 'backhand';
export type Handedness = 'left' | 'right';
export type PhaseCode = 'M0' | 'M1' | 'M2' | 'M3' | 'M4' | 'M5' | 'M6';
export type MetricStatus = 'green' | 'red' | 'unknown';
export type MeasurementUnit = 'deg' | 'cm' | 'ratio' | 'm_s';

/** MediaPipe Pose Landmarker: 33 landmark, x/y нормализованы к ширине/высоте кадра, z — относительно бёдер. */
export interface LandmarkPoint {
  x: number; // 0..1 ширины кадра
  y: number; // 0..1 высоты кадра
  z?: number;
  visibility?: number; // 0..1
}

export interface PoseFrame {
  frameIndex: number;
  timestampMs: number;
  /** Пиксельный размер исходного кадра — нормализованные координаты нужно масштабировать до угловых расчётов. */
  width: number;
  height: number;
  landmarks: LandmarkPoint[]; // длина 33
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
  box: BoundingBox; // нормализованные координаты
}

export interface DetectedPhase {
  code: PhaseCode;
  startFrame: number;
  endFrame: number;
  /** Кадр контакта живёт внутри фазы (M5) — используется для оверлея аннотации. */
  contactFrame?: number;
  confidence?: number;
}

export interface Calibration {
  handedness: Handedness;
  /** Физический масштаб из калибровки; без него метрики в см/м·с остаются unknown. */
  cmPerPixel?: number;
  playerHeightCm?: number;
}

export interface ProRange {
  min?: number;
  max?: number;
}

/**
 * Определение метрики. Пороги, восстановленные из libapp.so оригинала.
 * proRange: null = диапазон отдаётся из Firestore `metric_definitions` в рантайме
 * и из APK восстановлен не был (см. ANALYSIS_FINDINGS.md «Ограничения анализа»).
 */
export interface MetricDefinition {
  code: string;
  label: string;
  unit: MeasurementUnit;
  strokeTypes: StrokeType[];
  phase: PhaseCode;
  proRange: ProRange | null;
  description: string | null;
  /** Шаблоны поправок с плейсхолдером {value}. */
  corrections: { tooLow: string | null; tooHigh: string | null };
  /** Ключ функции-калькулятора в реестре engine; null = ещё не реализовано. */
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
  /** 0..100 — доля зелёных метрик (v1-аппроксимация: точные веса оригинала не восстановлены, DECISIONS §6.4). */
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
  /** Коды слабых мест, отсортированные по удалённости от про-диапазона. */
  topWeaknesses: string[];
  notes: string[];
}
