# Architecture

All recorded decisions — see [reference/DECISIONS.md](../reference/DECISIONS.md).
This is a brief map for the developer.

```
┌──────────────────────────┐         ┌──────────────────────────────────┐
│  Telegram Mini App       │  HTTPS  │  Nest.js API (prefix /api)       │
│  Vite + Vue 3 + Pinia    │ ──────► │  Auth: initData → JWT            │
│  @telegram-apps/sdk      │  WSS    │  Users: profile, quotas          │
│                          │ ──────► │  Uploads: presigned PUT (S3)     │
│  CV pipeline (worker):   │         │  Analyses: CRUD + results        │
│   MediaPipe Pose (33 lm) │         │  Payments: Telegram Stars (XTR)  │
│   YOLO11 ONNX (ball/rac.)│         │  Drills + Plans (by weaknesses)  │
│   packages/engine:       │         │  Coach: Axel on GLM-5.3-Flash    │
│   phases M0–M6 → metrics │         │  WS: progress / completed        │
│   green/red → score      │         └──────────────┬───────────────────┘
└──────────────────────────┘                        │
                                             MongoDB (users, analyses, videos,
                                             drills, plans, subscriptions,
                                             metric_definitions, knowledge)
```

## Analysis flow (port of original v2.0.19)

1. User records a video (portrait, ≤5 s, one stroke) — `<input type="file" capture>`.
2. Frame → `POST /api/analysis/detect-stroke` → stroke type (or manual selection).
3. `POST /api/uploads/presign` → client uploads the original directly to S3 →
   `POST /api/analyses` (Stars quota check: free = 9 lifetime, pro = 20/month).
4. Client in a Web Worker: frame extraction (30 fps, 640px) → pose → YOLO →
   `runAnalysis()` from `packages/engine` → green/red metrics + overall_score.
5. `POST /api/analyses/:id/results` — report is saved (+ WS notification).
6. Annotated video: client-side canvas overlay + MediaRecorder → upload →
   `POST /api/analyses/:id/annotated` (fallback — server-side render, DECISIONS §7.2).
7. Drills/plan are selected by `topWeaknesses`; the Axel coach answers with analysis context.

## Scoring

green = value inside the pro range, red = outside — rule carried over 1:1.
`overall_score` v0 = share of green metrics (equal weights); the original's exact
weights were not recovered (DECISIONS §6.4) — replacement point in `packages/engine/src/scoring.ts`.

## Deliberately NOT ported

- Firebase (Auth/Storage/Firestore/FCM) → replaced with Nest + Mongo + S3 + Bot API.
- RevenueCat/Play Billing → Telegram Stars (periods without auto-renew).
- Cloud Run analyzer and CF functions → thin API; analysis on-device in the TMA.
- Native Android app → out of scope (reference/ only).
