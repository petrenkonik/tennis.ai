# Porting map: Dart (original) → monorepo

Source of names: `reference/ANALYSIS_FINDINGS.md` + strings from `reference/extracted_assets/libapp_strings.txt`.
Statuses: ✅ done · 🔶 skeleton ready, logic TODO · ⬜ not started.

| Original Dart service | Ported to | Status |
|---|---|---|
| `pose_service.dart` (ML Kit 33 landmark) | `apps/miniapp/src/services/poseService.ts` + `videoPipeline.ts` (`@mediapipe/tasks-vision@1.1.1`, VIDEO mode = built-in tracking, GPU→CPU fallback, lite float16 model) | ✅ v0; smoke test on live video ⬜ |
| `yolo_detector_service.dart` / `ball_detection_service.dart` | same place → onnxruntime-web (int8) | 🔶 interface ready |
| `calibration_service.dart` (px → cm) | `Calibration.cmPerPixel` in `@tennis-ai/shared` | 🔶 field exists, calibration ⬜ |
| `moment_detector*.dart` (M0–M6) | `packages/engine/src/phases.ts` | 🔶 heuristic v0 |
| `metrics_calculator.dart` | `packages/engine/src/measurements.ts` + `metrics.ts` | 🔶 17 of 24 calculators; thresholds from the reverse |
| Firestore `metric_definitions` thresholds | `packages/shared/src/metrics/metrics.json` | ✅ recovered ones; the rest — unknown-range |
| Scoring green/red + overall | `packages/engine/src/scoring.ts` | ✅ rule; weights = v0 approximation |
| `video_annotation_service.dart` (FFmpeg) | client canvas + MediaRecorder → server fallback | ⬜ |
| Video compression (FFmpeg Kit) | server (fluent-ffmpeg, 720p/crf 28) | ⬜ worker in the API |
| `stroke_detection_service.dart` | `POST /api/analysis/detect-stroke` (Gemini vision) | ✅ |
| `quota_service.dart` (9/20) | `apps/api/src/modules/users/users.service.ts` | ✅ |
| `drill_recommendation_service.dart` | `apps/api/src/modules/drills` | ✅ v0 (intersect weaknesses×targetMetrics) |
| `training_plan_service.dart` | `apps/api/src/modules/plans` | ✅ v0 |
| `axel/*` (Gemini function calling, memory) | `apps/api/src/modules/coach` | 🔶 proxy + tools; coach memory ⬜ |
| LIVE CORRECT (realtime) | waiting for the `getUserMedia` spike in the Telegram WebView (DECISIONS §6.3) | ⬜ |
| Firebase Auth/Storage/Firestore | Nest + Mongo + S3 (deliberate replacement, not a port) | ✅ skeleton |
| RevenueCat + Play Billing | Telegram Stars (deliberate replacement) | ✅ skeleton |
| pairip license check | not needed (TMA = initData signature) | — |
| UI clones of the original screens (CLONE_SPEC) | Android: 1:1 (SECTION_AUDIT); TMA: reskin 2026-10-18 | ✅ v0 TMA; screenshot comparison ⬜ |

## VIDEO PREVIEW + native TTS (2026-10-18)

Port of `video_preview_screen.dart` to the TMA (evidence: `reference/extracted_assets/`
libapp_strings, en.json, long_strings):

- Flow: video selection in `/record` → `/preview` — looping `<video>` (port of
  `setLooping`), quality gate («CHECKING VIDEO...», landscape with the verbatim text
  «rotating the file afterwards does not fix it», square crop, <360px →
  «VIDEO TOO LOW QUALITY» + RETAKE), «TRIM YOUR STROKE» — RangeSlider with two
  handles (port of `_previewTrim`/`trimStartSec`), three actions ANALYZE/RETAKE.
- The trim window cuts the pose frame sampling (startSec/endSec in `videoPipeline`),
  the original itself is loaded in full; `trimStartSec/trimEndSec` are stored in the session
  (`shared/api.ts` + analyses schema/DTO/service).
- The original's UX defect reproduced: there is no on-screen caption saying "the
  handles are being dragged" — it is spoken by voice (see TTS).
- **Native TTS** (`services/tts.ts`, Web Speech API): one-time voice guide of the
  review screen (port of `axel_recording_guide_shown_`), voicing Axel's answers
  via the speaker toggle (ttsOn in the store), the red stop mutes speech. In the Android clone
  TTS is still a flag without an engine (CLONE_SPEC §6.4) — mirror onto Kotlin TextToSpeech TODO.
- Left for later from the parsed reverse: download/share/RE-ANALYZE in My Videos,
  live elements.

## Results player: seeking by phases (2026-10-18)

Port of the `results_screen.dart` video player into `AnalysisView.vue` (evidence:
`_seekToMoment`, `_applyPlaybackSpeed`, `PlaySpeedReset`, `isLooping`,
`annotated_video_url` in the libapp.so strings):

- Player above the score card: looping `<video>` (port of `isLooping`), muted+playsinline.
- The M0–M6 phase chips now both select a phase and seek the clip to the phase's timecode —
  port of `_seekToMoment`: `currentTime = trimStartSec + startFrame / fps`
  (frames are sampled from the start of the trim window, so in the report `frame/fps`
  counts from `trimStartSec`).
- Speeds 0.25x/0.5x/1x below the player — port of `_applyPlaybackSpeed`
  (`playbackRate`). The original's exact set of speeds was not recovered from the strings —
  closest reconstruction based on `PlaySpeedReset`.
- Player URL: new `GET /api/analyses/:id/video` — presigned GET (S3Service
  `presignGet`), plays `videos.annotated`, until it exists — `videos.original`
  (port of `annotated_video_url`; annotated video — port ⬜, step "server-side
  ffmpeg worker"). The `AnalysisVideoResponse` contract in `shared/api.ts`,
  client — `analysis.fetchVideoUrl(id)`.

## My Videos: session actions (2026-10-18)

Port of the `video_library_screen.dart` actions into `VideosView.vue` (evidence: keys
en.json `re_analyze`, `delete_video`+`delete_video_confirm`, `video_deleted`,
`download_failed`, `video_saved_to`, `share_plus` in the manifest):

- Session card in the bucket: Open + a row of actions RE-ANALYZE / Share /
  Download / Delete Video (red). Busy state «Preparing…» while the blob is
  being downloaded.
- **RE-ANALYZE**: the session's original (presigned GET → File, store
  `analysis.fetchVideoFile`) is put into `pendingPreview` and `/preview` is opened —
  the same review/trim/analyze flow as for a new video; the quota is charged when
  the session is created, as in the original.
- **Share** (port of share_plus): `navigator.share({ files })` when available
  (canShare), otherwise download as fallback; cancelling the system sheet is not an error.
- **Download**: anchor+blob, toast `video_saved_to` / on error
  `download_failed`.
- **Delete Video**: confirmation from the original (`delete_video_confirm`, buttons
  CANCEL/DELETE), toasts `video_deleted`/`delete_failed`; cascade — the existing
  `DELETE /analyses/:id`.
- The same confirmation pattern added to My Analyses (the original's keys
  `delete_session` + `delete_session_confirm` — it used to delete without a dialog).

## LIVE CORRECT: getUserMedia spike + live cycle (2026-10-18)

`LiveView.vue` rewritten from a stub into a working live mode (ports: the clone's
`LiveAnalysisScreen.kt`, the original's `en.json`/libapp strings):

- **getUserMedia spike**: in Chromium (headless, `--use-fake-device-for-media-stream`)
  the full `/live` flow passes — start → «Get in position. 15 seconds.» →
  live feed with chips → stop → summary; MediaPipe Pose loads via the GPU delegate,
  `detectForVideo` runs without errors. Open question — the same flow inside
  the Telegram WebView on iOS/Android (verify on a real device).
- Flow: idle (stroke selection via chips + «START LIVE ANALYSIS») → prepare 15 s
  («Get in position. 15 seconds.» → «Go!», verbatim) → live → summary.
- Live cycle: `getUserMedia(facingMode: environment)` with a fallback to the front camera
  (mirrored), rAF + `detectForVideo` (~15 fps), BlazePose skeleton on a canvas
  over the feed (FILL_CENTER, port of the clone's SkeletonOverlay), chips REC /
  HD @ 30FPS / AI TRACKING: ACTIVE / SUBJECT IN FRAME ✓.
- Live metrics: `METRIC_DEFINITIONS` of the selected stroke with frame-based
  calculators and recovered proRange; the first metric outside the range —
  a red «LIVE CORRECTION» card with a 15-second window to correct it
  (port of the original's countdown), returning into the range counts as corrected.
  Speeds/ball (YAML null-range and non-frame calculators) do not participate in live.
- Summary (verbatim libapp strings): «All metrics corrected! Excellent form!»
  or «Errors found but none fixable live. Try drills.» + corrected/errors counters,
  buttons Try again / Drills. TTS voices the corrections and the summary
  when the speaker is enabled (`app.ttsOn`).
- Camera refusal/missing API → the former stub + «Camera permission required».
- **Dev-offline** (`VITE_DEV_OFFLINE=1`, DEV build only): the auth store issues a
  mock user without Telegram initData — local `pnpm dev:miniapp` run
  outside Telegram for all screens. The spike was run precisely in this mode.

## TMA reskin per CLONE_SPEC (2026-10-18)

`apps/miniapp` brought to the design system and screens of the clone (sources —
`tennis-ai-android` after the 2026-10-17 audit + `docs/CLONE_SPEC.md`):

- Design system in `src/styles.css` (palette §1.1, three typographic registers §1.2),
  primitives — `src/components/ui.ts` + `Icon.vue` (port of `ui/common/Ui.kt`).
- Icons — official Material Symbols outlined (weight 400), the same glyphs as the
  clone's `Icons.Outlined.*`: the registry `src/components/materialIcons.ts` is generated
  by `apps/miniapp/scripts/icon-fetch.mjs` from `@material-symbols/svg-400@0.48.0`
  (Apache-2.0). For «MY ANALYSES» `show_chart` is used (pulse line) — per the §2 audit
  SECTION_AUDIT that's how it is in the original.
- Navigation §3: bottom-nav home|videos|analyses|dashboard|profile + Axel FAB;
  select/edit/axel/record/live/analysis/drills/paywall — fullscreen without navigation.
- Screens: Home (6 modules with rings and COACH/NEW/N sessions labels), My Videos,
  My Analyses (buckets serve blue/forehand lime/backhand amber/drills red,
  `no_data` reproduced verbatim), Select/Player/Coach Dashboard
  (untranslated keys `track_progression`/`manage_students_perf`/`record_first_video`
  — verbatim), Profile + Edit Profile (General|Additional, chips, Progress recalculation),
  Axel (hologram, 2×2 hints, bubbles, Auto/Manual chip, red stop = AbortController).
- i18n: `src/i18n/T.ts` — port of `core/i18n/T.kt`; RU by default, EN verbatim from
  `reference/extracted_assets/en.json`; language selection in Settings.
- State: `src/stores/app.ts` — port of `AppState.kt` (profile/settings/lang,
  localStorage ≈ SharedPreferences).

Deliberate TMA deviations from the clone (product replacement, not a divergence from the original):

1. Axel speaks via `POST /api/coach/chat` (key on the server), not the GLM config
   from «App Version»; `set_app_language` switches the app language.
2. The «Free Plan» card leads to the Stars paywall `/paywall` (in the clone — Edit Profile).
3. Dashboard/bucket counters — real API data (list of analyses), not zeros;
   tapping a bucket expands the list of sessions (Open/Delete).
4. Live — a styled stub until the `getUserMedia` spike (see step 2 below).

## Next steps (by priority)

1. ~~Connect `@mediapipe/tasks-vision`~~ ✅ (2026-10-16): `poseService.ts` + streaming
   `analyzeVideoPoses` (seek → canvas → `detectForVideo`), frames are not buffered. WASM/model —
   CDN by default, self-hosting via `VITE_MEDIAPIPE_WASM_URL` / `VITE_POSE_MODEL_URL`.
   Remaining: smoke test on a real video in the Telegram WebView.
2. ~~`getUserMedia` spike~~ ✅ in the browser (2026-10-18, LiveView live cycle);
   remaining: verify the same flow inside the Telegram WebView (iOS/Android/desktop).
3. Port the exact `overall_score` weights once the formula is recovered
   (or fix our own calibration on live data).
4. Server-side ffmpeg worker for compression + annotated video.
5. Replace the heuristic phases of `phases.ts` with a port of the `moment_detector_ondevice`
   logic (motion-threshold sequences over frames from the `libapp_strings.txt` strings).
