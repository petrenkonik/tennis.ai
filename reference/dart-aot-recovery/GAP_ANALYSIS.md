# Porting audit: original analysis elements vs the Android port (tennis-ai-android)

Date: 2026-10-17. Source of truth: the v2.0.19 reverse (`reference/extracted_assets/`,
`reference/dart-aot-recovery/`). Statuses: ✅ ported exactly · ⚠️ ported
approximately · ❌ missing.

## Verdict

The core mechanics are ported correctly (phases M0–M6, the red/green rule against pro ranges,
scoring /100, the "frames → pose → phases → metrics → report" pipeline), but the **metric set
and data coverage is ~40%**: the original has ~40+ metric definitions with paired
dominant/non-dominant ranges, ball and racket metrics (YOLO), stance classification,
cm scaling based on player height, and pro comparison tables —
this is absent from the port or present only in simplified form.

## A. Analysis pipeline

| Element | Original | Port | Status |
|---|---|---|---|
| Recording protocol | portrait 1080p/30, side view, whole body, ≤5 s, one stroke | same texts on AnalyzeScreen | ✅ |
| Calibration checklist | SUBJECT IN FRAME, "phone at waist height, 3m away", "sideline & baseline intersection" (`calibration_service`) | hint text only, no checklist | ⚠️ |
| Clip checks | TOO LOW QUALITY; landscape ("rotating the file afterwards does not fix it"); back to the camera; square crop (distorts px→cm); "seeing your back, not your side" | none | ❌ |
| Clip trimming | draggable handles, `_trim.mp4` | none | ❌ |
| Auto stroke detection | 1 frame → CF `strokeDetect` (serve/forehand/backhand), `AutoDetectFlowScreen` | none (manual chip selection) | ❌ |
| Video compression | FFmpeg 720p/libx264/crf28 → `compressed.mp4` | none (only frame downscale during extraction) | ❌ |
| Frame extraction | fps=30, scale=640:-2, `frame_%04d.jpg` + HQ frames for the overlay | fps guess + ≤150 frames, 640px | ⚠️ |
| Pose detection | ML Kit Pose (MediaPipe, 33 landmarks) with smoothing (`PoseLandmarkWithSmoothing`) | MediaPipe Pose Landmarker full, GPU→CPU, no explicit smoothing | ⚠️ |
| YOLO ball+racket | `yolo11_detection.tflite` + `tennis_ball.tflite` + `tennis_racket.tflite`; racket keypoints x1y1x2y2; second pass "YOLO-REPASSE" | none | ❌ |
| Phase detection | M0–M6 (`moment_detector[_ondevice]`), RT recalibration on M2 | same phase structure; its own heuristics (toss apex / peak wrist speed) | ⚠️ |
| Scoring | green = inside the pro range, red = outside; overall /100, weights from `metric_definitions` (Firestore) | rule is 1:1; weights are its own (originals not recovered) | ⚠️ |
| Annotated video | skeleton on HQ frames → mp4 (`annotated_video_url`) | none | ❌ |
| Progress stages | DETECTING KEYPOINTS → CALCULATING JOINT ANGLES → MAPPING TORSO ROTATION / ANALYZING KNEE FLEXION → FINALIZING REPORT | 3 of 5 stages | ⚠️ |
| Gamification | "You earned 150 XP and unlocked a new badge" (`celebration_screen`) | none | ❌ |
| Quotas | free 9 analyses / pro 20 per month / 1000 per year (`quota_service`) | none (no backend) | ❌ |
| Session history | sessionId, MY VIDEOS, cascade deletion | current result in memory only | ⚠️ |

## B. Metrics (the main gap)

15 definitions are ported (`core/metrics/MetricDefs.kt`). In the original:

**1. Phase codes (1:1 naming):**
`M1_ARMS_POSITION`, `M1_FOOT_ANGLE`, `M2_KNEES_POSITION`, `M2_UNIT_TURN`,
`M2_WEIGHT_TRANSFER`, `M3_TRIPLE_EXTENSION`, `M3_TROPHY_POSITION`,
`M4_TRUNK_ROTATION_BODY_ALIGNMENT`, `M5_CONTACT_POINT_HEIGHT_DISTANCE`,
`M5_RACKET_HEAD_POSITION` + codes `KNEE_FLEXION`, `DOM_KNEE`, `NDOM_KNEE`,
`ALIGNMENT`, `CONTACT_POINT`, `FOLLOW_THROUGH`, `RACQUET_PATH`.
→ The port uses its own names; rename to 1:1.

**2. D/ND range pairs per phase (18 pairs):** `(D10-175/ND40-180)`,
`(D30-110/ND30-95)`, `(D40-120/ND20-135)`, `(D65-160/ND50-145)`,
`(D85-180/ND95-180)`, `(D90-180/ND40-180)`, `(D75-175/ND50-160)`,
`(D60-175/ND20-170)`, `(D65-175/ND65-170)`, `(D65-130/ND65-145)`,
`(D80-165/ND90-175)`, `(D95-180/ND85-170)`, `(D70-165/ND30-180)`,
`(D0-50/ND0-50)`, `(D0-75/ND0-60)`, `(D3-85/ND1-80)`, `(D30-125/ND2-125)`,
`(D5-75/ND5-75)` — the dominant and non-dominant joints are measured separately.
→ In the port, most angles are computed only on the dominant side.

**3. Thresholds not present in the port:** `(100-180)`, `(109-180)`, `(130-162)`,
`(140-175)`, `(20-170)`, `(75-120)`, `(80-180)`, `(85-160/166/170/175)`,
`(90-165)`, `(R 155-178)`, `(R:100-179)`, `(K110-180/H100-180)`,
`(K120-180/H100-150)`, `Arm cocked 40-120`, `Front chain 2-33`,
`Depth 15-90cm`, `7-30cm`.

**4. Ball (not in the port — YOLO needed):**
- ball release height from the hand: "above eye level (player height −10cm, −5..+25cm)"
- toss apex: "1.8–2.3x player height"
- contact: "H:50–165cm D:10–100cm, 0–100 open" / "D:10–80cm, 0–80 open"
- contact depth/height: `Depth 15–90cm`, `7–30cm`
- serve jump height (`jumpheight`)

**5. Racket (not in the port):** face angle in the takeback ("controls spin type");
face ∥ shoulders <25° at trophy; head over wrist; lag ("wrist leads racket head at
contact"); head drop below the ball; trajectory (`racquetpathtrajectory`); acceleration
(`racketacceleration`).

**6. Stance classification (not in the port):** open / semi-open / closed / neutral at
contact, with pro examples: "Djokovic: Closed | Sinner: Semi-open | Wawrinka:
Closed" (backhand), "Djokovic: Semi-open | Alcaraz: Open | Sinner: Semi-open".

**7. Ready position (M0):** "arms hanging too low… racket at chest height, elbows
slightly away" (forehand), "both hands on the racket at chest height, non-dominant
hand on the throat" (backhand) — in the port M0 only covers knees/stance.

**8. Landing/legs:** "Back leg angle during landing. Stable landing on front
foot", `landingontheexternalfoot`, `landingoutsidefoot`, `rearlegalignment`,
`backfootalignment`, `pushoftheinternalfoot`.

**9. Misc:** `split_step` as a measurable element; `kinetic_chain(_principle)`;
"Body center offset from the baseline. Should be centered and balanced";
"Horizontal alignment of both shoulders… level during stance"; player height
(`height_cm`, `player_height_cm`) — the basis of the cm scale; the port does not ask for height,
cm metrics are absent.

**10. Pro comparison:** **87 rows** of values per metric×phase (Sinner,
Djokovic, Federer, Alcaraz, Wawrinka): "(M3) Alcaraz: 165 | Sinner: 164 |
Djokovic: 168 | Federer: 151", "Alcaraz: −3.8cm | Sinner: 2.2cm | Djokovic:
12.1cm | Federer: 5.0cm", "Alcaraz: 1.0x | Djokovic: 1.1x | Sinner: 1.2x",
"Alcaraz: 28cm (ratio 0.8x) | Sinner: 66cm (ratio 1.9x)".
→ In the port the Results screen shows only an explanation, without values.

**11. Accuracy of ported thresholds:** match 1:1 — knee 110–170,
heel-hip-shoulder 155–179, ankle-hip-shoulder 110–170, toss arm 152–180,
non-dom elbow 102–180, trunk tilt 5–39/10–45, wrist speed 10–25 m/s, toss 1.5–6.0
m/s, shoulders <12°, stance 0.8–1.5x. Approximate (recovered indirectly):
trophy coil, unit turn, contact in front, weight transfer, triple extension.

## C. Real-time

| Element | Original | Port | Status |
|---|---|---|---|
| All metrics live | "Auto-detect stroke & coach all metrics" | knee flexion + stance width only | ⚠️ |
| Recalibration | `[RT-RECALIB] Calibrated on M2` | none | ❌ |
| Ball window | `[RT-BALL-WINDOW]` | none (no YOLO) | ❌ |
| SUBJECT IN FRAME live | yes | none | ❌ |
| Correction cards | fixable-live metrics, "All metrics corrected!" | present (2 metrics) | ⚠️ |

## D. Report (Results)

| Element | Original | Port | Status |
|---|---|---|---|
| Phase tabs + breakdown | M0–M6 tap-through | present | ✅ |
| "WHAT THIS MOMENT IS FOR" texts | its own per phase | its own (not verbatim) | ⚠️ |
| Corrections | per phase, per metric, error direction | present (generic per metric) | ⚠️ |
| Pro comparison with values | 87 rows of values | none (explanation only) | ❌ |
| Analysis progress for Axel | moments + red metrics JSON into the chat | present (Android Axel) | ✅ |

## Priorities for reaching 1:1

- **P1 (data and accuracy):** player height input + cm scaling; D/ND pairs in
  MetricDefs; renaming metrics to the original codes; the full threshold table
  (45 values above); stance classification; pro values (87 rows) in Results.
- **P2 (pipeline):** clip quality checks (landscape/back/quality/crop);
  auto stroke detection — feasible with GLM vision (a client already exists in AxelScreen);
  live: all metrics, recalibration on M2; pose smoothing.
- **P3 (instrumentation):** YOLO ball/racket (train on open datasets), ball/racket
  metrics, annotated video, XP/celebration, trimming, session history.
