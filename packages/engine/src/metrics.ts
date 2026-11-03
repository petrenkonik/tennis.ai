import type { MeasurementUnit, MetricDefinition, MetricEvaluation, MetricStatus, ProRange } from '@tennis-ai/shared';

export function formatRange(unit: MeasurementUnit, range: ProRange | null): string {
  if (!range) return 'pro range n/a';
  const suffix = unit === 'deg' ? '°' : unit === 'cm' ? ' cm' : unit === 'm_s' ? ' m/s' : 'x';
  const fmt = (v: number): string => `${v}${suffix}`;
  if (range.min !== undefined && range.max !== undefined) return `${fmt(range.min)}–${fmt(range.max)}`;
  if (range.max !== undefined) return `≤ ${fmt(range.max)}`;
  if (range.min !== undefined) return `≥ ${fmt(range.min)}`;
  return 'pro range n/a';
}

export function formatValue(unit: MeasurementUnit, value: number): string {
  const rounded = Math.round(value * 10) / 10;
  const suffix = unit === 'deg' ? '°' : unit === 'cm' ? ' cm' : unit === 'm_s' ? ' m/s' : 'x';
  return `${rounded}${suffix}`;
}

export interface EvaluateNote {
  reason: string;
}

/** Port of the original scoring rule: green = value inside the pro range, red = outside. */
export function evaluateMetric(def: MetricDefinition, value: number | null, note?: EvaluateNote): MetricEvaluation {
  const base = {
    code: def.code,
    label: def.label,
    phase: def.phase,
    unit: def.unit,
    proRangeLabel: formatRange(def.unit, def.proRange),
    confidence: def.confidence,
  };

  if (value === null || Number.isNaN(value)) {
    return {
      ...base,
      value: null,
      status: 'unknown',
      displayValue: 'n/a',
      correction: null,
    };
  }

  const r = def.proRange;
  if (!r) {
    return {
      ...base,
      value,
      status: 'unknown',
      displayValue: formatValue(def.unit, value),
      correction: note ? `No pro range available: ${note.reason}` : null,
    };
  }

  const tooLow = r.min !== undefined && value < r.min;
  const tooHigh = r.max !== undefined && value > r.max;
  const status: MetricStatus = tooLow || tooHigh ? 'red' : 'green';

  const template = tooLow ? def.corrections.tooLow : tooHigh ? def.corrections.tooHigh : null;
  return {
    ...base,
    value,
    status,
    displayValue: formatValue(def.unit, value),
    correction: template ? template.replace('{value}', String(Math.round(value * 10) / 10)) : null,
  };
}
