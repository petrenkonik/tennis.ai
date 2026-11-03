# Tennis.AI Rebuild HQ — AI Agent Team

A team of AI agents for reverse-engineering `com.tennis.ai` v2.0.19. The team
consists of AI models only; the coordinator (the main ZCode session) assigns
tasks and reviews the work. All agents respond and document in Russian.

Agents are defined by files in `.zcode/agents/*.md` — ZCode picks them up as
custom agent types (new/modified agents activate in a new session).
Each can be invoked by name; the coordinator decides who executes a task.

## Team

| Agent slug | Role | Domain | Phase |
|---|---|---|---|
| `rea-analyst` | Reverse analyst | `reference/` — facts about the original (read-only) | all phases |
| `android-dev` | Android developer | `tennis-ai-android/` (Kotlin + Compose, 1:1 clone) | **phase 1 — active** |
| `engine-porter` | Engine porter | `packages/engine` (TS) ↔ `tennis-ai-android/.../core/` (Kotlin), always mirrored | phases 1–2 |
| `api-dev` | Backend developer | `apps/api/` (Nest.js, already started — continue, don't rewrite) | phase 2 |
| `tma-dev` | TMA developer | `apps/miniapp/` (Vite + Vue 3 + Pinia) | phase 3 — dormant |
| `qa-verifier` | Quality controller | gates: `pnpm typecheck`, engine build, `gradlew assembleDebug/test`, screenshot comparison | all phases |

## Phase order (set by the owner — do not change without them)

1. **Phase 1 — Android clone (now).** All effort in `tennis-ai-android/`:
   screens 1:1 per `docs/OriginalScrean/`, logic ported from `packages/engine`.
   The API is not required at this stage (the app works standalone).
2. **Phase 2 — full product.** Finishing the started Nest.js API (`apps/api/`),
   connecting the Android clone to the server (auth, quotas, uploads, analyses, payments).
3. **Phase 3 — Telegram App.** Bringing the Mini App (`apps/miniapp/`) up to clone level.
   Until the phase starts, do not touch the TMA.

Owner's rule: **don't do everything at once — work in parts.** One phase,
one screen/module per pass; after each change — the `qa-verifier` gates.

## Typical work chains

- **New Android screen:** `rea-analyst` (how it is in the original) → `android-dev`
  (implementation) → `qa-verifier` (build + screenshot comparison).
- **Engine change:** `engine-porter` (TS↔Kotlin mirrored) → `qa-verifier`.
- **Server task (phase 2):** `api-dev` → `qa-verifier` (`pnpm typecheck`).
- **"How is it in the original?" dispute:** we stop, `rea-analyst` provides a fact/hypothesis
  with a source; disputes about the approach are settled only by the owner via
  `reference/DECISIONS.md`.

## Team-wide rules

- `reference/` is read-only; heavy binaries are not committed.
- The main automatic gate of the TS repository is `pnpm typecheck` (there is no
  linter or test runner). Android — `./gradlew assembleDebug` and `./gradlew test`.
- `overall_score` v0 = share of green metrics; replacement — only in `scoring.ts`,
  per the owner's decision (DECISIONS §6.4).
- Russian language: documentation, agent responses, RU UI strings by default
  (`tennis-ai-android/.../core/i18n/T.kt`).
