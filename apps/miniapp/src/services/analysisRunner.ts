import type { AnalysisInput } from '@tennis-ai/engine';
import type { AnalysisOutcome } from '@tennis-ai/shared';

/**
 * runAnalysis из packages/engine исполняется в Web Worker (DECISIONS §7.2:
 * «анализ целиком on-device»), чтобы математика по 150 кадрам не блокировала UI-поток.
 * MediaPipe (GPU) живёт на main thread — см. docs/PORTING.md, поэтому сюда приходят
 * уже готовые PoseFrame[].
 */
export function runEngineAnalysis(input: AnalysisInput): Promise<AnalysisOutcome> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('../workers/analysis.worker.ts', import.meta.url), { type: 'module' });
    worker.onmessage = (e: MessageEvent<{ type: 'outcome'; outcome: AnalysisOutcome }>) => {
      if (e.data?.type !== 'outcome') return;
      worker.terminate();
      resolve(e.data.outcome);
    };
    worker.onerror = (e) => {
      worker.terminate();
      reject(new Error(`analysis worker failed: ${e.message}`));
    };
    worker.postMessage({ type: 'analyze', ...input });
  });
}
