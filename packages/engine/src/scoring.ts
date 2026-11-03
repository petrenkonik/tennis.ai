import type { MetricEvaluation, OverallScore } from '@tennis-ai/shared';

/**
 * v1 approximation of overall_score: equal weights, share of green metrics.
 * The exact original weights were not recovered from AOT (DECISIONS §6.4) —
 * the metrics_calculator.dart formula is ported here once it becomes known.
 */
export function computeOverall(metrics: MetricEvaluation[]): OverallScore {
  let greens = 0;
  let reds = 0;
  let unknown = 0;
  for (const m of metrics) {
    if (m.status === 'green') greens++;
    else if (m.status === 'red') reds++;
    else unknown++;
  }
  const evaluable = greens + reds;
  return {
    score: evaluable > 0 ? Math.round((greens / evaluable) * 100) : null,
    greens,
    reds,
    unknown,
  };
}

/** Weaknesses: red metrics sorted by relative out-of-range deviation. */
export function topWeaknesses(metrics: MetricEvaluation[], limit = 5): string[] {
  const score = (m: MetricEvaluation): number => {
    if (m.value === null) return 0;
    const [lo, hi] = parseRangeLabel(m.proRangeLabel);
    if (lo === null && hi === null) return 0;
    const span = (hi ?? lo ?? 0) - (lo ?? hi ?? 0) || Math.abs(hi ?? lo ?? 1) || 1;
    const nearest = Math.max(lo ?? -Infinity, Math.min(hi ?? Infinity, m.value));
    return Math.abs(m.value - nearest) / Math.abs(span);
  };
  return metrics
    .filter((m) => m.status === 'red')
    .sort((a, b) => score(b) - score(a))
    .slice(0, limit)
    .map((m) => m.code);
}

function parseRangeLabel(label: string): [number | null, number | null] {
  const nums = label.match(/-?\d+(\.\d+)?/g);
  if (!nums || nums.length === 0) return [null, null];
  if (nums.length === 1) return [null, Number(nums[0])];
  return [Number(nums[0]), Number(nums[1])];
}
