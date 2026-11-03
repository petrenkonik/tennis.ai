# AGENTS.md — instructions for agents

Rebuild of the reverse-engineered `com.tennis.ai` v2.0.19 (Flutter/Android). Two products:
1. **Telegram Mini App + Nest.js API** — pnpm monorepo (`apps/`, `packages/`), Node ≥ 22, pnpm 10.
2. **Native Android app** (`tennis-ai-android/`) — **a full clone of the original,
   as close as possible, but in Kotlin** (Jetpack Compose). Not "inspiration" — a 1:1
   copy of the original UI and functionality.

## Language policy

**All documentation and code comments must be in English only** — README, `docs/`,
`reference/` docs, `AGENTS.md`, agent definitions (`.zcode/agents/`), commit messages,
and comments in source code (`//`, `/* */`, `<!-- -->`, `#`). Never add new Russian
text to documentation or comments. Runtime product content (UI strings, i18n values,
API error messages) is exempt — it follows the app's own i18n.

## Commands

```bash
pnpm install
pnpm build            # all packages (engine/shared are built twice: CJS + ESM)
pnpm typecheck        # the main quality gate
pnpm dev:api          # Nest.js on :3000 (needs apps/api/.env, template: .env.example)
pnpm dev:miniapp      # Vite on :5173
pnpm seed:drills      # populate the drills library
pnpm --filter @tennis-ai/engine build   # targeted package build
```

**There is no linter (ESLint/Prettier) or test runner in the repo** — `pnpm typecheck`
after changes is mandatory; it is the only automated gate.

## Structure

- `apps/miniapp/` — TMA: Vite + Vue 3 + TS + Pinia, `@telegram-apps/sdk`.
  The CV pipeline (MediaPipe Pose 33 landmarks + YOLO ONNX) lives in
  `src/workers/analysis.worker.ts`; screens are in `src/views`, state in `src/stores`.
- `apps/api/` — Nest.js, global prefix `/api`, Mongoose. Modules in
  `src/modules`: auth (initData → JWT), users (quotas: free 9 lifetime / pro 20 per
  month), uploads (S3 presign), analyses, payments (Telegram Stars, currency
  `XTR`, no auto-renew), drills, plans, coach; analysis progress is delivered
  via WS (`src/ws`).
- `packages/shared/` — domain types, API contracts, metric thresholds from the reverse
  (`src/metrics/metrics.json`, unknown ranges are marked unknown-range).
- `packages/engine/` — biomech engine: `phases.ts` (phases M0–M6) →
  `measurements.ts` + `metrics.ts` → `scoring.ts`. Pure functions only,
  **no DOM/Node APIs** — runs inside a Web Worker.
- `packages/telegram/` — HMAC verification of initData, Stars invoices. Server-only.
- `reference/` — reverse-engineering artifacts of the original. **Read-only, not built.**
  Heavy binaries (`apks/`, `rea-outputs/`, `extracted_assets/models/`,
  `*.apk`, `*.so`) are gitignored — do not commit them.
- `tennis-ai-android/` — **native Android app, in scope** (Kotlin + Compose,
  `com.rebuild.tennisai`, Gradle 8.9 lives in `tools/`), not part of the pnpm workspace.
  Independent builds via `gradlew` in that folder. The requirement is a full Kotlin
  clone of the original: screens, flows and logic replicate the original as closely
  as possible. The UI reference is the original screenshots in `docs/OriginalScrean/`;
  engine logic is ported from `packages/engine`, not rewritten from scratch.
- `docs/ARCHITECTURE.md`, `docs/PORTING.md` — Dart → TS porting map with statuses.

## AI agent team

The work is done by the "Tennis.AI Rebuild HQ" team — manifest and roster in
`docs/AI_TEAM.md`, agent definitions in `.zcode/agents/`. The phase order is set by
the owner: **1) Android clone (active) → 2) full product (Nest.js API) → 3) Telegram
Mini App**. Do not do everything at once — work in parts; after edits run the gates
(`pnpm typecheck`, `gradlew assembleDebug/test`).

## Boundaries and rules

- Dependency direction: `shared → engine → (miniapp, api)`;
  `shared → telegram → api`. App code must not import from `packages/*`.
- **Engine logic is duplicated in Kotlin** — `tennis-ai-android/.../core/`
  (`metrics/MetricsCalculator.kt`, `phases/MomentDetector.kt`, `engine/PoseEngine.kt`).
  When changing `packages/engine`, mirror the change in the Android port.
- `overall_score` v0 = share of green metrics with equal weights; the exact weights
  of the original were not recovered — replacement point `packages/engine/src/scoring.ts`
  (DECISIONS §6.4).
- Phases and some metrics are still a heuristic port (statuses 🔶/⬜ in `PORTING.md`);
  check the status before "fixing" mismatches with the original.
- Miniapp is ESM (`"type": "module"`); `engine`/`shared` ship dual CJS+ESM
  (`dist/` + `dist/esm/`) — do not break the dual build.
- ENV: `apps/api/.env` — `MONGO_URI`, `JWT_SECRET`, `TELEGRAM_BOT_TOKEN`,
  `S3_*`, `GEMINI_API_KEY`; `apps/miniapp/.env` — `VITE_API_URL` (default
  `http://localhost:3000/api`), optional `VITE_MEDIAPIPE_WASM_URL` /
  `VITE_POSE_MODEL_URL` (otherwise CDN).

## Required reading before changes

- `reference/DECISIONS.md` — owner-approved architectural decisions; the approach
  (stack, Firebase/RevenueCat replacements, payment scheme) may only be changed
  through that file. ⚠️ Item §2 "the native Android app is not part of the project"
  is outdated: the native Kotlin clone is in scope (see above).
- `reference/ANALYSIS_FINDINGS.md` — product knowledge recovered from the original.
- `docs/PORTING.md` — what is ported, what is TODO, and the next steps.
- `docs/OriginalScrean/` — 11 screenshots of the original app; the UI reference
  for the Android clone (and TMA screens).
