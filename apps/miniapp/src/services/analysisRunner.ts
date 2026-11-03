import type { AnalysisInput } from '@tennis-ai/engine';
import type { AnalysisOutcome } from '@tennis-ai/shared';

/**
 * runAnalysis from packages/engine runs in a Web Worker (DECISIONS §7.2:
 * "analysis fully on-device"), so the math over 150 frames does not block the UI thread.
 * MediaPipe (GPU) lives on the main thread — see docs/PORTING.md — so ready-made
 * PoseFrame[] arrive here.
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
