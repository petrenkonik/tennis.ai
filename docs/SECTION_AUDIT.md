# SECTION AUDIT — screen-by-screen comparison of the clone with the original via REA

A journal of screen-by-screen refinement of `tennis-ai-android/` to a 1:1 match with the
original `com.tennis.ai` v2.0.19. Source of truth — the REA reverse artifacts:
`reference/extracted_assets/` (en.json, libapp_strings, all_identifiers),
`reference/dart-aot-recovery/`, photos in `docs/OriginalScrean/`, `docs/CLONE_SPEC.md`.

## Comparison protocol (the same for every section)

1. **Dossier from REA** — the section's strings from `en.json` (verbatim, including
   untranslated i18n keys — the original's defects are reproduced 1:1), widget/service
   names from `all_identifiers.txt`, literals from
   `libapp_strings.txt` / `long_strings.txt`.
2. **Code check** — the clone's screen against the dossier and CLONE_SPEC: structure, colors
   (`ui/theme/Theme.kt`), typography (3 registers), strings, navigation.
3. **Runtime check** — `gradlew :app:assembleDebug` → install on
   Pixel_7 (ADB port 5237, see CLONE_SPEC §7) → screenshots into `shots/` →
   comparison with the original's photos (both scroll states, RU and EN).
4. **Fixes + gate** — divergences are fixed, rebuild, another
   screenshot; also a line in this journal.

Statuses: ✅ checked, no divergences · 🔸 checked, minor deliberate
differences (listed) · ⬜ not checked.

## 1. Home («AXEL AI», tab 1) — ✅ (2026-10-17)

Files: `ui/home/HomeScreen.kt`, `ui/common/Ui.kt`, `core/i18n/T.kt`.
Screenshots: `shots/01_home.png`, `shots/01_home_scrolled.png`.

REA findings:
- There are no `tag_coach`/`tag_new` keys in en.json — the tags in the original are the keys
  `coach` ('COACH') and `new` ('NEW'). The clone's keys were renamed to match the original.
- The values «Coach Mode», «Real-time analysis for coaches», «Auto Detect»,
  «Auto-detect your stroke and analyze it» are absent in en.json, but exist
  as literals in libapp (the `coach_mode`/`auto_detect` keys are confirmed by
  `all_identifiers.txt`) — the clone's values match verbatim.
- `sessions` = 'sessions' → the «0 sessions» label is assembled as «N sessions» ✓.

Fixed (code check + runtime):
1. Bottom of the module card: it was — the line and the label on the same row; now — the line
   at 84% width, the label UNDER it on the right (as in the original).
2. Home top bar: added background `Bar` (#0A0A0A) and a hairline at the bottom.
3. REAL-TIME ANALYSIS button: removed the green fill `0xFF0C1206` — in the
   original there is only the accent border.
4. Analyses tab icon in the bottom-nav: `MonitorHeart` (a frame with a pulse) →
   `AutoMirrored.ShowChart` (a clean pulse line, as in the original).

Remaining (not blocking, verify when convenient):
- The original's card vertical paddings are ~2–4dp larger — within
  tolerance, the photo resolution does not allow measuring more precisely.

## 2. My Videos + My Analyses — ⬜

Known in advance (found during audit 1): `LibraryScreens.kt` uses
`MonitorHeart` in the «MY ANALYSES» top bar and in the no_data tiles — in the original
it is a pulse line everywhere (`ShowChart`). The top bars must be Bar+hairline.
Original photos: `photo_2026-10-17_11-55-25.jpg` (Videos),
`photo_2026-10-17_11-55-29.jpg` (Analyses).

## 3. Dashboard (Select / Player / Coach) — ⬜

Photos: `11-55-16` (Select), `11-55-07` (Player), `11-55-21` (Coach).
Check: the untranslated keys `track_progression`, `manage_students_perf`,
`record_first_video` (verbatim!), the REFERENCE cardAccent banner, the 2×2 grid.

## 4. Profile + Edit Profile — ⬜

Photos: `11-54-53` (Profile), `11-55-11` (Edit/Additional).
Check: «App Version 2.0.1» (the clone's version? the original shows 2.0.1
for package 2.0.19 — reproduce verbatim), Ambassador €0, toggles,
the General|Additional segment, chips with emoji.

## 5. Axel — ⬜

Photos: `11-55-02` (idle), `11-55-47` (conversation).
Check: the Auto/Manual chip, speaker, red stop, 2×2 hints (texts
cropped on the photos — filled in, see CLONE_SPEC §2.2/§6.5), Record/Upload,
chat bubbles, GLM config via «App Version».

## 6. Analyze → Results — ⬜ (core; priorities in GAP_ANALYSIS P1–P3)

Check against `reference/dart-aot-recovery/GAP_ANALYSIS.md`: range D/ND pairs,
metric codes 1:1 (`M1_ARMS_POSITION`…), pro comparison (87 rows),
progress stages (5 of 5), phase texts. This is the largest gap (~40% of the data).

## 7. Live / Real-time — ⬜

GAP_ANALYSIS §C: recalibration at M2, ball window, SUBJECT IN FRAME,
all live metrics (the clone keeps only knee flexion + stance width).

## Infrastructure notes

- ADB on this machine — port **5237** (`ANDROID_ADB_SERVER_PORT=5237`),
  `input tap` coordinates — in physical pixels 1080×2400 (Read may
  show screenshots scaled — take bounds from
  `uiautomator dump`).
- The first start of a fresh AVD spends a couple of minutes on dexopt/Play updates —
  System UI ANR dialogs are possible, unrelated to the app
  (dismiss by tapping «Wait», not «Close app»).
