import { ref } from 'vue';
import { defineStore } from 'pinia';
import { api, uploadToPresigned } from '../api/client';
import { runEngineAnalysis } from '../services/analysisRunner';
import { analyzeVideoPoses, detectObjects } from '../services/videoPipeline';
import { useAuthStore } from './auth';
import type {
  AnalysisDto,
  AnalysisVideoResponse,
  CreateAnalysisResponse,
  DetectStrokeResponse,
  PresignResponse,
  StrokeType,
  UploadKind,
} from '@tennis-ai/shared';

/** Этапы клиентского пайплайна перед созданием сессии на API. */
export type AnalyzeStage = 'pose' | 'engine' | 'upload';

export const useAnalysisStore = defineStore('analysis', () => {
  const list = ref<AnalysisDto[]>([]);
  const loading = ref(false);
  /** Выбранный клип между RecordView и PreviewView (объект не живёт в URL роутера). */
  const pendingPreview = ref<{ file: File; url: string; strokeType: StrokeType } | null>(null);

  function setPendingPreview(file: File, strokeType: StrokeType): string {
    if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value.url);
    const url = URL.createObjectURL(file);
    pendingPreview.value = { file, url, strokeType };
    return url;
  }

  function clearPendingPreview(): void {
    if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value.url);
    pendingPreview.value = null;
  }

  async function detectStrokeFromFrame(file: Blob): Promise<StrokeType> {
    // 1) presign под кадр 2) PUT 3) классификация
    const presign = await api<PresignResponse>('/uploads/presign', {
      method: 'POST',
      body: JSON.stringify({ kind: 'frame', contentType: 'image/jpeg' } satisfies { kind: UploadKind; contentType: string }),
    });
    await uploadToPresigned(presign.uploadUrl, file, 'image/jpeg');
    const det = await api<DetectStrokeResponse>('/analysis/detect-stroke', {
      method: 'POST',
      body: JSON.stringify({ frameKey: presign.key }),
    });
    return det.strokeType;
  }

  async function createSession(
    strokeType: StrokeType,
    video: File,
    trim?: { startSec: number; endSec: number },
  ): Promise<CreateAnalysisResponse> {
    // 1) presign под оригинал (sessionId генерирует API) 2) PUT видео 3) создать сессию
    const presign = await api<PresignResponse>('/uploads/presign', {
      method: 'POST',
      body: JSON.stringify({ kind: 'original', contentType: video.type || 'video/mp4' }),
    });
    await uploadToPresigned(presign.uploadUrl, video, video.type || 'video/mp4');
    return api<CreateAnalysisResponse>('/analyses', {
      method: 'POST',
      body: JSON.stringify({
        strokeType,
        videoKey: presign.key,
        ...(trim
          ? {
              trimStartSec: Math.round(trim.startSec * 100) / 100,
              trimEndSec: Math.round(trim.endSec * 100) / 100,
            }
          : {}),
      }),
    });
  }

  /**
   * Полный клиентский цикл (как в оригинале v2.0.19 — анализ on-device ДО загрузки):
   *   1. MediaPipe Pose по кадрам видео (on-device, прогресс)
   *   2. runAnalysis в воркере: фазы M0–M6, метрики, overall_score
   *   3. upload оригинала + создание сессии (тут списывается квота) + POST результатов
   * Если поза не найдена ни в одном кадре — сессия не создаётся и квота не тратится.
   */
  async function analyzeVideo(
    file: File,
    strokeType: StrokeType,
    onStage: (stage: AnalyzeStage, done?: number, total?: number) => void,
    trim?: { startSec: number; endSec: number },
  ): Promise<CreateAnalysisResponse> {
    const auth = useAuthStore();

    onStage('pose');
    const video = await analyzeVideoPoses(
      file,
      {
        onProgress: (done, total) => onStage('pose', done, total),
        ...(trim ? { startSec: trim.startSec, endSec: trim.endSec } : {}),
      },
    );
    if (video.poseFrames === 0) {
      throw new Error('No pose detected. Make sure the whole body is visible in the frame.');
    }

    onStage('engine');
    const outcome = await runEngineAnalysis({
      pose: video.poses,
      detections: await detectObjects(),
      strokeType,
      fps: video.fps,
      calibration: { handedness: auth.user?.profile.dominantHand ?? 'right' },
    });

    onStage('upload');
    const res = await createSession(strokeType, file, trim);
    await api(`/analyses/${res.id}/results`, {
      method: 'POST',
      body: JSON.stringify({
        fps: outcome.fps,
        frameCount: outcome.frameCount,
        phases: outcome.phases,
        metrics: outcome.metrics,
        overall: outcome.overall,
        topWeaknesses: outcome.topWeaknesses,
        notes: outcome.notes,
      }),
    });
    return res;
  }

  async function fetchList(): Promise<void> {
    loading.value = true;
    try {
      list.value = await api<AnalysisDto[]>('/analyses');
    } finally {
      loading.value = false;
    }
  }

  async function fetchOne(id: string): Promise<Record<string, unknown>> {
    return api<Record<string, unknown>>(`/analyses/${id}`);
  }

  /** URL плеера Results (порт annotated_video_url): annotated, пока его нет — оригинал. */
  async function fetchVideoUrl(id: string): Promise<string | null> {
    const res = await api<AnalysisVideoResponse>(`/analyses/${id}/video`);
    return res.url;
  }

  /** Оригинал сессии как File — для download/share/RE-ANALYZE в My Videos. */
  async function fetchVideoFile(id: string, strokeType: string): Promise<File> {
    const url = await fetchVideoUrl(id);
    if (!url) throw new Error('video url is null');
    const res = await fetch(url);
    if (!res.ok) throw new Error(`download failed: ${res.status}`);
    const blob = await res.blob();
    return new File([blob], `tennis-${strokeType}-${id.slice(-6)}.mp4`, { type: blob.type || 'video/mp4' });
  }

  async function remove(id: string): Promise<void> {
    await api(`/analyses/${id}`, { method: 'DELETE' });
    list.value = list.value.filter((a) => a.id !== id);
  }

  return {
    list,
    loading,
    pendingPreview,
    setPendingPreview,
    clearPendingPreview,
    detectStrokeFromFrame,
    createSession,
    analyzeVideo,
    fetchList,
    fetchOne,
    fetchVideoUrl,
    fetchVideoFile,
    remove,
  };
});
