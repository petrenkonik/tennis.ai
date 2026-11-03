# Tennis AI 2.0 (rebuild) — locked-in decisions

Locked in on: 2026-10-16
Status: approved by the project owner
Source of product knowledge: [ANALYSIS_FINDINGS.md](./ANALYSIS_FINDINGS.md) — reverse engineering of the original `com.tennis.ai` v2.0.19

## 1. What we are building

A web restart of the original app in the form of a **Telegram Mini App**: the full cycle of
"stroke video record/upload → pose detection → phases M0–M6 → biomechanical
metrics → scoring vs pro reference values → corrections → drills / training plan → AI coach".

## 2. Locked-in stack

| Layer | Decision |
|---|---|
| Frontend | **Telegram Mini App**: Vite + Vue 3 + TypeScript + Pinia, `@telegram-apps/sdk` |
| Backend | **Nest.js** (TypeScript, REST + WebSocket) |
| Database | **MongoDB** (Mongoose) |
| Subscription | **Telegram Stars** (Bot API invoice, currency `XTR`) |
| CV engine | Client-side, in a Web Worker: MediaPipe Pose Landmarker (33 landmarks, WASM SIMD/WebGL, WebGPU as an enhancement) + YOLO11 ONNX via onnxruntime-web |
| Native Android app | **Not part of the project**. The original APK is kept in the repository only as a reverse-engineering reference (see below). No native development |

### TMA platform adapters

- **Auth**: `initData` validation (HMAC-SHA256 with the bot token) on Nest.js → JWT issuance. Firebase Auth is not used.
- **Payments**: Telegram Stars. Important: Stars do not support auto-renew — the subscription is implemented by purchasing a period (1/3/12 months), the expiry is stored on the backend, and the bot reminds about renewal.
- **Push**: notifications from the bot.
- **Camera**: base flow — `<input type="file" accept="video/*" capture>` (native recording) + file upload → analysis on the client. LIVE mode (`getUserMedia`) — to be decided by a spike; feature detection.

## 3. Monorepo structure (pnpm)

```
tennis-ai/
├─ apps/
│  ├─ miniapp/          # TMA: Vite + Vue 3 + TS + Pinia
│  └─ api/              # Nest.js: auth, users, payments, analysis, drills, coach
├─ packages/
│  ├─ engine/           # biomech engine (shared): pose, phases, metrics, scoring
│  ├─ shared/           # API contracts, types, metric thresholds (JSON)
│  └─ telegram/         # TMA SDK adapter, initData auth, Stars payments
├─ reference/           # REVERSE OF THE ORIGINAL — read-only, not built
│  ├─ ANALYSIS_FINDINGS.md, DECISIONS.md
│  └─ apks/com.tennis.ai_v2.0.19.apks.apk + apks_extracted/
└─ docs/
```

## 4. MongoDB data schema (initial sketch)

- `users` — telegram_id, profile (level, dominant hand, height), settings
- `analyses` — analysis sessions: strokeType, moments M0–M6, metrics + red/green status, overall_score, video references
- `videos` — file metadata (binaries are NOT in Mongo, see open questions)
- `drills` — library: objective, setup, progression, reps/sets, difficulty, targetMetrics
- `plans` — training plans, linked to analysis weaknesses
- `subscriptions` — Stars payment periods, statuses, history
- `metric_definitions` — thresholds/descriptions/correction texts (port of `metric_definitions` from the reverse)
- `knowledge` — Axel knowledge base (domains: biomechanics, tactics, fitness, mental, drills, coaching)

## 5. Nest.js API modules

`AuthModule` (initData→JWT) · `UsersModule` · `PaymentsModule` (Stars invoice + webhooks) ·
`AnalysisModule` (history, results) · `DrillsModule` + `PlansModule` (weakness-based recommendations) ·
`CoachModule` (Axel: proxy to **GLM-5.3-Flash** (Z.ai, OpenAI-compatible chat completions + tool calling; decided by the owner on 2026-10-16 — replacement for Gemini), tools from the reverse — recovered verbatim from Dart AOT, see `reference/dart-aot-recovery/AXEL_RECOVERY.md`) · `UploadModule` (presigned URLs) · `AdminModule`

Coach env: `GLM_API_KEY`, `GLM_BASE_URL` (default `https://api.z.ai/api/paas/v4`), `GLM_MODEL` (default `glm-5.3-flash`), `GLM_VISION_MODEL` (detect-stroke, defaults to GLM_MODEL).

## 6. Open questions (not blocking, to be resolved along the way)

1. **Video file storage**: MongoDB is not used for videos — recommendation: S3-compatible (Cloudflare R2 / MinIO), presigned upload from the Mini App.
2. **Hosting**: default candidate — VPS/cloud (api + Mongo) + CDN/pages for Mini App static assets.
3. **`getUserMedia` spike in the Telegram WebView** (iOS/Android/desktop) — determines the fate of LIVE CORRECT in the TMA.
4. Thresholds/weights of the overall_score aggregation — recover more precisely from the reverse when porting `metrics_calculator`.

## 7. Video pipeline (locked in 2026-10-16)

A replica of the original flow (see ANALYSIS_FINDINGS.md) on the TMA + Nest.js + S3-compatible storage stack.

### 7.1. Original flow (reference)

1. Recording: portrait 1080p/30fps, ≤5 sec, one stroke; import from gallery; trim on the preview (`_trim.mp4`)
2. Stroke type detection: 1 frame (`stroke_detect_frame.jpg`) → CF `strokeDetect`
3. Client-side compression (FFmpeg Kit): `scale='min(720,iw)':-2, libx264, preset fast, crf 28, aac 96k` → `compressed_<ts>.mp4`; on failure — the original
4. Direct upload (client SDK `referencePutFile`) to Firebase Storage → `getDownloadURL` → URL in Firestore (`videoUrl`)
5. On-device analysis: frames `fps=30, scale=640:-2` → `frame_%04d.jpg`; HQ frames for the overlay; pose (ML Kit) + YOLO11 (ball/racket) → moments M0–M6 → metrics → overall_score
6. Annotated video locally from HQ frames: `libx264, crf 23, yuv420p, -an` → `/annotated.mp4` → to Storage → `annotated_video_url`
7. "MY VIDEOS" library; deleting a video cascade-deletes the analysis

### 7.2. Our implementation

| Step | Decision |
|---|---|
| Compression | On the API (ffmpeg + worker, fluent-ffmpeg): 720p / libx264 / crf 28 — same output parameters. The client uploads the original (5 sec ≈ 15–25 MB); ffmpeg.wasm in the WebView is not used |
| Upload | Presigned PUT to S3-compatible storage (R2/MinIO); the API issues keys, the file goes directly from the client |
| Storage layout | `users/{userId}/analyses/{sessionId}/original.mp4` · `compressed.mp4` · `annotated.mp4` (+ `frames/` if needed) |
| Metadata | Mongo: `videos` (files, sizes, keys) + `analyses` (results, references to keys) |
| Stroke detection | `POST /api/analysis/detect-stroke` — frame → serve/forehand/backhand (Gemini vision or client-side YOLO) |
| Annotated video | On-client: skeleton from landmarks on a canvas over the frames → `captureStream` + MediaRecorder → upload. Fallback: server-side render from landmark JSON (ffmpeg) |
| Deletion | Cascade: Mongo records + all session storage objects |
| Analysis | Entirely on-device in `packages/engine` (TMA worker), as in the original v2.0.19. The API stays thin: metadata, files, Stars quotas, drills |

### 7.3. Nest.js endpoints (v1)

- `POST /api/uploads/presign` — presigned PUT (original / frame / annotated)
- `POST /api/analyses` — create an analysis session
- `POST /api/analyses/:id/results` — metrics, phases M0–M6, overall_score, landmarks
- `POST /api/analyses/:id/annotated` — attach the annotated video
- `POST /api/analysis/detect-stroke` — stroke type from a frame
- `GET /api/analyses` — library (history), `GET /api/analyses/:id` — report
- `DELETE /api/analyses/:id` — cascade deletion (Mongo + storage)
