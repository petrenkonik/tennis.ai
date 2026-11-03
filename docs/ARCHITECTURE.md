# Архитектура

Полные зафиксированные решения — [reference/DECISIONS.md](../reference/DECISIONS.md).
Здесь — краткая карта для разработчика.

```
┌──────────────────────────┐         ┌──────────────────────────────────┐
│  Telegram Mini App       │  HTTPS  │  Nest.js API (prefix /api)       │
│  Vite + Vue 3 + Pinia    │ ──────► │  Auth: initData → JWT            │
│  @telegram-apps/sdk      │  WSS    │  Users: профиль, квоты           │
│                          │ ──────► │  Uploads: presigned PUT (S3)     │
│  CV-пайплайн (worker):   │         │  Analyses: CRUD + результаты     │
│   MediaPipe Pose (33 lm) │         │  Payments: Telegram Stars (XTR)  │
│   YOLO11 ONNX (мяч/рак.) │         │  Drills + Plans (по weaknesses)  │
│   packages/engine:       │         │  Coach: Axel на GLM-5.3-Flash    │
│   фазы M0–M6 → метрики → │         │  WS: progress / completed        │
│   green/red → score      │         └──────────────┬───────────────────┘
└──────────────────────────┘                        │
                                             MongoDB (users, analyses, videos,
                                             drills, plans, subscriptions,
                                             metric_definitions, knowledge)
```

## Поток анализа (порт оригинала v2.0.19)

1. Пользователь снимает видео (портрет, ≤5 с, один удар) — `<input type="file" capture>`.
2. Кадр → `POST /api/analysis/detect-stroke` → тип удара (или выбор вручную).
3. `POST /api/uploads/presign` → клиент льёт оригинал напрямую в S3 →
   `POST /api/analyses` (проверка квоты Stars: free = 9 lifetime, pro = 20/мес).
4. Клиент в Web Worker: извлечение кадров (30 fps, 640px) → pose → YOLO →
   `runAnalysis()` из `packages/engine` → метрики green/red + overall_score.
5. `POST /api/analyses/:id/results` — сохранение отчёта (+ WS-уведомление).
6. Аннотированное видео: клиентский canvas-оверлей + MediaRecorder → upload →
   `POST /api/analyses/:id/annotated` (fallback — серверный рендер, DECISIONS §7.2).
7. Drills/план подбираются по `topWeaknesses`; Axel-коуч отвечает с контекстом анализа.

## Скоринг

green = значение внутри про-диапазона, red = вне — правило перенесено 1:1.
`overall_score` v0 = доля зелёных метрик (равные веса); точные веса оригинала
не восстановлены (DECISIONS §6.4) — точка замены в `packages/engine/src/scoring.ts`.

## Что осознанно НЕ портировано

- Firebase (Auth/Storage/Firestore/FCM) → заменён на Nest + Mongo + S3 + Bot API.
- RevenueCat/Play Billing → Telegram Stars (периоды без авто-renew).
- Cloud Run-анализатор и CF-функции → тонкий API; анализ on-device в TMA.
- Нативное Android-приложение → вне скоупа (reference/ только).
