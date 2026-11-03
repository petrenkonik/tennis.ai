import type {
  AnalysisOutcome,
  DetectedPhase,
  MetricEvaluation,
  OverallScore,
  StrokeType,
} from './types';

// ---- Auth (initData → JWT) ----

export interface TelegramAuthRequest {
  initData: string;
}

export interface UserDto {
  id: string;
  telegramId: number;
  firstName?: string;
  username?: string;
  photoUrl?: string;
  profile: {
    level?: string;
    dominantHand?: 'left' | 'right';
    heightCm?: number;
  };
  language: string;
  xp: number;
  quota: QuotaState;
}

export interface TelegramAuthResponse {
  accessToken: string;
  user: UserDto;
}

// ---- Quotas (port of quota_service: free = 9 video analyses lifetime; pro = 20/month) ----

export interface QuotaState {
  plan: 'free' | 'pro';
  analysesUsed: number;
  analysesLimit: number | null;
  periodEndsAt?: string | null;
}

// ---- Uploads (DECISIONS §7.2: presigned PUT to S3-compatible storage) ----

export type UploadKind = 'original' | 'frame' | 'annotated';

export interface PresignRequest {
  kind: UploadKind;
  contentType: string;
  sessionId?: string;
}

export interface PresignResponse {
  sessionId: string;
  key: string;
  uploadUrl: string;
  expiresInSec: number;
}

// ---- Analyses (DECISIONS §7.3) ----

export interface CreateAnalysisRequest {
  strokeType: StrokeType;
  videoKey: string;
  /** Trim window from VIDEO PREVIEW (port of the original trimStartSec); undefined = no trim. */
  trimStartSec?: number;
  trimEndSec?: number;
}

export interface CreateAnalysisResponse {
  id: string;
  status: 'created';
  quotaRemaining: number | null;
}

export interface AnalysisResultsRequest {
  fps: number;
  frameCount: number;
  phases: DetectedPhase[];
  metrics: MetricEvaluation[];
  overall: OverallScore;
  topWeaknesses: string[];
  notes?: string[];
}

export interface AttachAnnotatedRequest {
  annotatedKey: string;
}

export interface DetectStrokeRequest {
  frameKey: string;
}

export interface DetectStrokeResponse {
  strokeType: StrokeType;
  confidence?: number;
}

export interface AnalysisDto {
  id: string;
  strokeType: StrokeType;
  status: 'created' | 'analyzing' | 'completed' | 'failed';
  overallScore?: number | null;
  videos: { original?: string; compressed?: string; annotated?: string };
  createdAt: string;
}

/** GET /analyses/:id/video — presigned URL for the player (annotated, otherwise original). */
export interface AnalysisVideoResponse {
  url: string | null;
  expiresInSec: number;
}

// ---- Payments (Telegram Stars) ----

export type ProductId = 'pro_1m' | 'pro_3m' | 'pro_12m';

export interface InvoiceRequest {
  productId: ProductId;
}

export interface InvoiceResponse {
  link: string;
}

// ---- Coach ----

export interface CoachChatRequest {
  message: string;
  analysisId?: string;
}

export interface CoachChatResponse {
  reply: string;
}
