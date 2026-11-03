# AGENTS.md — инструкции для агентов

Ребилд реверс-инжиниренного `com.tennis.ai` v2.0.19 (Flutter/Android). Два продукта:
1. **Telegram Mini App + Nest.js API** — pnpm-монорепа (`apps/`, `packages/`), Node ≥ 22, pnpm 10.
2. **Нативное Android-приложение** (`tennis-ai-android/`) — **полный клон оригинала,
   максимально похожий, но на Kotlin** (Jetpack Compose). Не «вдохновение», а копия
   UI и функциональности оригинала 1:1.

Вся документация в репо — на русском.

## Команды

```bash
pnpm install
pnpm build            # все пакеты (engine/shared собираются дважды: CJS + ESM)
pnpm typecheck        # главная проверка качества
pnpm dev:api          # Nest.js на :3000 (нужен apps/api/.env, шаблон — .env.example)
pnpm dev:miniapp      # Vite на :5173
pnpm seed:drills      # наполнить библиотеку drills
pnpm --filter @tennis-ai/engine build   # точечная сборка пакета
```

**Линтера (ESLint/Prettier) и тестового раннера в репо нет** — `pnpm typecheck`
после изменений обязателен, это единственный автоматический гейт.

## Структура

- `apps/miniapp/` — TMA: Vite + Vue 3 + TS + Pinia, `@telegram-apps/sdk`.
  CV-пайплайн (MediaPipe Pose 33 landmark + YOLO ONNX) живёт в
  `src/workers/analysis.worker.ts`; экраны — `src/views`, состояние — `src/stores`.
- `apps/api/` — Nest.js, глобальный префикс `/api`, Mongoose. Модули в
  `src/modules`: auth (initData → JWT), users (квоты: free 9 lifetime / pro 20 в
  мес), uploads (S3 presign), analyses, payments (Telegram Stars, currency
  `XTR`, без авто-renew), drills, plans, coach; прогресс анализа — через WS (`src/ws`).
- `packages/shared/` — доменные типы, API-контракты, пороги метрик из реверса
  (`src/metrics/metrics.json`, неизвестные диапазоны помечены unknown-range).
- `packages/engine/` — биомех-движок: `phases.ts` (фазы M0–M6) →
  `measurements.ts` + `metrics.ts` → `scoring.ts`. Только чистые функции,
  **без DOM/Node API** — исполняется в Web Worker.
- `packages/telegram/` — HMAC-верификация initData, Stars-инвойсы. Только для сервера.
- `reference/` — артефакты реверса оригинала. **Только чтение, не билдится.**
  Тяжёлые бинарники (`apks/`, `rea-outputs/`, `extracted_assets/models/`,
  `*.apk`, `*.so`) в .gitignore — не коммитить.
- `tennis-ai-android/` — **нативное Android-приложение, в скоупе** (Kotlin + Compose,
  `com.rebuild.tennisai`, Gradle 8.9 лежит в `tools/`), не входит в pnpm-workspace.
  Независимая сборка через `gradlew` в этой папке. Требование — полный клон
  оригинала на Kotlin: экраны, флоу и логика повторяют оригинал максимально точно.
  Эталон UI — скриншоты оригинала в `docs/OriginalScrean/`; логика движка портируется
  из `packages/engine`, не пишется заново.
- `docs/ARCHITECTURE.md`, `docs/PORTING.md` — карта портирования Dart → TS со статусами.

## Команда ИИ-агентов

Работу ведёт «Штаб пересборки Tennis.AI» — манифест и состав в `docs/AI_TEAM.md`,
определения агентов в `.zcode/agents/`. Порядок фаз задан владельцем:
**1) Android-клон (активна) → 2) полный продукт (Nest.js API) → 3) Telegram Mini App**.
Не делать всё сразу — работать по частям; после правок прогонять гейты
(`pnpm typecheck`, `gradlew assembleDebug/test`).

## Границы и правила

- Направление зависимостей: `shared → engine → (miniapp, api)`;
  `shared → telegram → api`. Код приложений не импортируется из `packages/*`.
- **Логика движка продублирована в Kotlin** — `tennis-ai-android/.../core/`
  (`metrics/MetricsCalculator.kt`, `phases/MomentDetector.kt`, `engine/PoseEngine.kt`).
  Меняя `packages/engine`, зеркаль правки в Android-порт.
- `overall_score` v0 = доля зелёных метрик с равными весами; точные веса
  оригинала не восстановлены — точка замены `packages/engine/src/scoring.ts`
  (DECISIONS §6.4).
- Фазы и часть метрик — пока heuristic-порт (статусы 🔶/⬜ в `PORTING.md`);
  сверяй статус перед «исправлением» расхождений с оригиналом.
- Miniapp — ESM (`"type": "module"`); `engine`/`shared` шипуют dual CJS+ESM
  (`dist/` + `dist/esm/`) — не ломай двойную сборку.
- ENV: `apps/api/.env` — `MONGO_URI`, `JWT_SECRET`, `TELEGRAM_BOT_TOKEN`,
  `S3_*`, `GEMINI_API_KEY`; `apps/miniapp/.env` — `VITE_API_URL` (по умолчанию
  `http://localhost:3000/api`), опционально `VITE_MEDIAPIPE_WASM_URL` /
  `VITE_POSE_MODEL_URL` (иначе CDN).

## Обязательно к прочтению перед правками

- `reference/DECISIONS.md` — зафиксированные владельцем архитектурные решения;
  менять подход (стек, замены Firebase/RevenueCat, схема оплаты) можно только через него.
  ⚠️ Пункт §2 «нативное Android-приложение не входит в проект» устарел: нативный
  клон на Kotlin — в скоупе (см. выше).
- `reference/ANALYSIS_FINDINGS.md` — знания о продукте из реверса оригинала.
- `docs/PORTING.md` — что портировано, что TODO и ближайшие шаги.
- `docs/OriginalScrean/` — 11 скриншотов оригинального приложения; эталон UI
  для Android-клона (и TMA-экранов).
