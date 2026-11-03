# Tennis AI 2.0 (ребилд) — зафиксированные решения

Дата фиксации: 2026-10-16
Статус: утверждено владельцем проекта
Источник знаний о продукте: [ANALYSIS_FINDINGS.md](./ANALYSIS_FINDINGS.md) — реверс-инжиниринг оригинального `com.tennis.ai` v2.0.19

## 1. Что строим

Веб-рестарт оригинального приложения в виде **Telegram Mini App**: полный цикл
«запись/загрузка видео удара → детекция позы → фазы M0–M6 → биомеханические
метрики → скоринг vs эталоны профи → поправки → drills / training plan → AI-коуч».

## 2. Зафиксированный стек

| Слой | Решение |
|---|---|
| Frontend | **Telegram Mini App**: Vite + Vue 3 + TypeScript + Pinia, `@telegram-apps/sdk` |
| Backend | **Nest.js** (TypeScript, REST + WebSocket) |
| База данных | **MongoDB** (Mongoose) |
| Подписка | **Telegram Stars** (Bot API invoice, currency `XTR`) |
| CV-движок | Клиентский, в Web Worker: MediaPipe Pose Landmarker (33 landmark, WASM SIMD/WebGL, WebGPU как улучшение) + YOLO11 ONNX via onnxruntime-web |
| Нативное Android-приложение | **В проект не входит**. Оригинальный APK лежит в репозитории только как референс реверса (см. ниже). Нативную разработку не ведём |

### Платформенные адаптеры TMA

- **Auth**: валидация `initData` (HMAC-SHA256 токеном бота) на Nest.js → выпуск JWT. Firebase Auth не используется.
- **Платежи**: Telegram Stars. Важно: Stars не поддерживают авто-renew — подписка реализуется покупкой периода (1/3/12 мес.), срок хранится на бэке, бот напоминает о продлении.
- **Пуши**: уведомления от бота.
- **Камера**: базовый флоу — `<input type="file" accept="video/*" capture>` (нативная запись) + загрузка файла → анализ в клиенте. LIVE-режим (`getUserMedia`) — по результату спайка, feature-detection.

## 3. Структура монорепы (pnpm)

```
tennis-ai/
├─ apps/
│  ├─ miniapp/          # TMA: Vite + Vue 3 + TS + Pinia
│  └─ api/              # Nest.js: auth, users, payments, analysis, drills, coach
├─ packages/
│  ├─ engine/           # биомех-движок (общий): pose, phases, metrics, scoring
│  ├─ shared/           # контракты API, типы, пороги метрик (JSON)
│  └─ telegram/         # TMA SDK-адаптер, initData-auth, Stars-платежи
├─ reference/           # РЕВЕРС ОРИГИНАЛА — только чтение, не билдится
│  ├─ ANALYSIS_FINDINGS.md, DECISIONS.md
│  └─ apks/com.tennis.ai_v2.0.19.apks.apk + apks_extracted/
└─ docs/
```

## 4. Схема данных MongoDB (первичный набросок)

- `users` — telegram_id, профиль (уровень, доминирующая рука, рост), настройки
- `analyses` — сессии анализа: strokeType, moments M0–M6, метрики + статус red/green, overall_score, ссылки на видео
- `videos` — метаданные файлов (бинарники — НЕ в Mongo, см. открытые вопросы)
- `drills` — библиотека: objective, setup, progression, reps/sets, difficulty, targetMetrics
- `plans` — training plans, привязка к weaknesses анализа
- `subscriptions` — периоды Stars-платежей, статусы, история
- `metric_definitions` — пороги/описания/тексты поправок (порт `metric_definitions` из реверса)
- `knowledge` — база знаний Axel (домены: biomechanics, tactics, fitness, mental, drills, coaching)

## 5. Модули Nest.js API

`AuthModule` (initData→JWT) · `UsersModule` · `PaymentsModule` (Stars invoice + вебхуки) ·
`AnalysisModule` (история, результаты) · `DrillsModule` + `PlansModule` (рекомендации по weaknesses) ·
`CoachModule` (Axel: прокси к **GLM-5.3-Flash** (Z.ai, OpenAI-совместимый chat completions + tool calling; решено владельцем 2026-10-16 — замена Gemini), инструменты из реверса — восстановлены дословно из Dart AOT, см. `reference/dart-aot-recovery/AXEL_RECOVERY.md`) · `UploadModule` (presigned URLs) · `AdminModule`

Env для коуча: `GLM_API_KEY`, `GLM_BASE_URL` (по умолчанию `https://api.z.ai/api/paas/v4`), `GLM_MODEL` (по умолчанию `glm-5.3-flash`), `GLM_VISION_MODEL` (detect-stroke, по умолчанию = GLM_MODEL).

## 6. Открытые вопросы (не заблокировано, решить по ходу)

1. **Файловое хранилище видео**: MongoDB для видео не используется — рекомендация: S3-совместимое (Cloudflare R2 / MinIO), presigned upload из Mini App.
2. **Хостинг**: кандидат по умолчанию — VPS/облако (api + Mongo) + CDN/страницы для статики Mini App.
3. **Спайк `getUserMedia` в WebView Telegram** (iOS/Android/desktop) — определяет судьбу LIVE CORRECT в TMA.
4. Порог/веса агрегации overall_score — восстановить точнее из реверса при портировании `metrics_calculator`.

## 7. Видео-пайплайн (зафиксировано 2026-10-16)

Реплика флоу оригинала (см. ANALYSIS_FINDINGS.md) на стеке TMA + Nest.js + S3-совместимое хранилище.

### 7.1. Флоу оригинала (референс)

1. Запись: портрет 1080p/30fps, ≤5 сек, один удар; импорт из галереи; трим на превью (`_trim.mp4`)
2. Определение типа удара: 1 кадр (`stroke_detect_frame.jpg`) → CF `strokeDetect`
3. Сжатие на клиенте (FFmpeg Kit): `scale='min(720,iw)':-2, libx264, preset fast, crf 28, aac 96k` → `compressed_<ts>.mp4`; при ошибке — оригинал
4. Прямой upload (client SDK `referencePutFile`) в Firebase Storage → `getDownloadURL` → URL в Firestore (`videoUrl`)
5. Анализ on-device: кадры `fps=30, scale=640:-2` → `frame_%04d.jpg`; HQ-кадры для оверлея; pose (ML Kit) + YOLO11 (мяч/ракетка) → моменты M0–M6 → метрики → overall_score
6. Аннотированное видео локально из HQ-кадров: `libx264, crf 23, yuv420p, -an` → `/annotated.mp4` → в Storage → `annotated_video_url`
7. Библиотека «MY VIDEOS»; удаление видео каскадно удаляет анализ

### 7.2. Наша реализация

| Шаг | Решение |
|---|---|
| Сжатие | На API (ffmpeg + worker, fluent-ffmpeg): 720p / libx264 / crf 28 — те же выходные параметры. Клиент грузит оригинал (5 сек ≈ 15–25 МБ); ffmpeg.wasm в WebView не используем |
| Upload | Presigned PUT в S3-совместимое хранилище (R2/MinIO); API выдаёт ключи, файл идёт напрямую от клиента |
| Layout хранилища | `users/{userId}/analyses/{sessionId}/original.mp4` · `compressed.mp4` · `annotated.mp4` (+ `frames/` при необходимости) |
| Метаданные | Mongo: `videos` (файлы, размеры, ключи) + `analyses` (результаты, ссылки на ключи) |
| Определение удара | `POST /api/analysis/detect-stroke` — кадр → serve/forehand/backhand (Gemini vision или клиентский YOLO) |
| Аннотированное видео | On-client: скелет из landmark'ов на canvas поверх кадров → `captureStream` + MediaRecorder → upload. Fallback: серверный рендер из landmark-JSON (ffmpeg) |
| Удаление | Каскад: Mongo-записи + все объекты хранилища сессии |
| Анализ | Целиком on-device в `packages/engine` (воркер TMA), как в оригинале v2.0.19. API — тонкий: метаданные, файлы, квоты Stars, drills |

### 7.3. Эндпоинты Nest.js (v1)

- `POST /api/uploads/presign` — presigned PUT (original / frame / annotated)
- `POST /api/analyses` — создать сессию анализа
- `POST /api/analyses/:id/results` — метрики, фазы M0–M6, overall_score, landmark'и
- `POST /api/analyses/:id/annotated` — привязать аннотированное видео
- `POST /api/analysis/detect-stroke` — тип удара по кадру
- `GET /api/analyses` — библиотека (история), `GET /api/analyses/:id` — отчёт
- `DELETE /api/analyses/:id` — каскадное удаление (Mongo + storage)
