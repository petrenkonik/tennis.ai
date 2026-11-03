# Tennis AI 2.0 — rebuild (Telegram Mini App)

Ребилд оригинального `com.tennis.ai` v2.0.19 (Flutter/Android) в виде Telegram Mini App.
Знания о продукте получены реверс-инжинирингом (REA) — см. [reference/ANALYSIS_FINDINGS.md](./reference/ANALYSIS_FINDINGS.md).
Зафиксированные решения — [reference/DECISIONS.md](./reference/DECISIONS.md).

## Структура

```
apps/
  miniapp/        TMA: Vite + Vue 3 + TS + Pinia, @telegram-apps/sdk
  api/            Nest.js: auth, users, uploads (S3 presign), analyses, payments (Stars), drills, plans, coach
packages/
  engine/         биомех-движок: pose → фазы M0–M6 → метрики → скоринг (чистые функции, Web Worker-совместимый)
  shared/         доменные типы, API-контракты, пороги метрик (JSON) из реверса
  telegram/       initData HMAC-верификация, Telegram Stars invoices
reference/        РЕВЕРС ОРИГИНАЛА — только чтение, не билдится
docs/             архитектура, карта портирования
```

## Команды

```bash
pnpm install
pnpm build          # сборка всех пакетов
pnpm dev:api        # Nest.js на :3000 (нужен MONGO_URI в apps/api/.env)
pnpm dev:miniapp    # Vite dev-сервер на :5173
pnpm seed:drills    # наполнить библиотеку drills
```

## Данные окружения

`apps/api/.env` (шаблон — `apps/api/.env.example`): `MONGO_URI`, `JWT_SECRET`,
`TELEGRAM_BOT_TOKEN`, `S3_*`, `GEMINI_API_KEY`.

`apps/miniapp/.env`: `VITE_API_URL` (по умолчанию `http://localhost:3000/api`).
