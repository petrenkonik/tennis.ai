export * from './types';
export * from './api';

import metricsJson from './metrics/metrics.json';
import type { MetricDefinition, StrokeType } from './types';

/** Все определения метрик, портированные из реверса (пороги + тексты поправок). */
export const METRIC_DEFINITIONS = metricsJson.metrics as unknown as MetricDefinition[];

export function metricsForStroke(strokeType: StrokeType): MetricDefinition[] {
  return METRIC_DEFINITIONS.filter((m) => m.strokeTypes.includes(strokeType));
}

export function metricByCode(code: string): MetricDefinition | undefined {
  return METRIC_DEFINITIONS.find((m) => m.code === code);
}
