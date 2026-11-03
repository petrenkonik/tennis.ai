# Tennis AI 2.0 — rebuild (Telegram Mini App)

Rebuild of the original Tennis AI app v2.0.19 (Flutter/Android) as a Telegram Mini App.
Product knowledge was recovered via reverse engineering (REA) — see [reference/ANALYSIS_FINDINGS.md](./reference/ANALYSIS_FINDINGS.md).
Owner-approved decisions — [reference/DECISIONS.md](./reference/DECISIONS.md).

A static build of the miniapp is deployed to GitHub Pages:
**https://petrenkonik.github.io/tennis.ai/** (UI only — a live API server is required for data).

## Structure

```
apps/
  miniapp/        TMA: Vite + Vue 3 + TS + Pinia, @telegram-apps/sdk
  api/            Nest.js: auth, users, uploads (S3 presign), analyses, payments (Stars), drills, plans, coach
packages/
  engine/         biomech engine: pose → phases M0–M6 → metrics → scoring (pure functions, Web Worker-compatible)
  shared/         domain types, API contracts, metric thresholds (JSON) from the reverse
  telegram/       initData HMAC verification, Telegram Stars invoices
tennis-ai-android/ native Android clone (Kotlin + Compose), built independently via gradlew
reference/        REVERSE OF THE ORIGINAL — read-only, not built
docs/             architecture, porting map
```

## Commands

```bash
pnpm install
pnpm build          # build all packages
pnpm typecheck      # the main quality gate
pnpm dev:api        # Nest.js on :3000 (needs MONGO_URI in apps/api/.env)
pnpm dev:miniapp    # Vite dev server on :5173
pnpm seed:drills    # populate the drills library
```

## Environment data

`apps/api/.env` (template: `apps/api/.env.example`): `MONGO_URI`, `JWT_SECRET`,
`TELEGRAM_BOT_TOKEN`, `S3_*`, `GEMINI_API_KEY`.

`apps/miniapp/.env`: `VITE_API_URL` (default `http://localhost:3000/api`).

## Deployment

Pushes to `main` automatically deploy the miniapp build to GitHub Pages via
`.github/workflows/deploy-pages.yml`. To point the Pages build at a live API,
set the `VITE_API_URL` repository variable (Settings → Secrets and variables →
Actions → Variables) and pass it to the build step in the workflow.
