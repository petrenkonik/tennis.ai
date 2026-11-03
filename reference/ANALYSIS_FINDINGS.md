# Tennis AI 2.0 (com.tennis.ai) v2.0.19 — reverse-engineering findings

Date: 2026-10-16. Tooling: REA CLI 6.5.0 + jadx-headless-mcp 0.7.1 (Java 21, Android Studio JBR).

## Target identification

- File: `com.tennis.ai_v2.0.19.apks.apk` (243 MB, App Bundle ZIP)
- REA Evidence: `ev_7f8b10ab273ceac2442806b5363b4ca749f9d4b8539ad360c34b927ccf5dd516`
- base.apk SHA-256: `bb423559aaf67256638694641d81a279c3e386b6bf805b0cd6de2338bc1478e3`
- Package: `com.tennis.ai`, versionName 2.0.19, versionCode 102, minSdk 24, targetSdk 36
- 18,860 Java/Kotlin classes; the Android side is a thin Flutter wrapper (`com.tennis.ai.MainActivity` = a standard Flutter activity, `onCreate` only)
- All logic is Dart AOT (`lib/arm64-v8a/libapp.so`, 13 MB, package `tennis_ai_2`)

## Stack

- Flutter (Dart AOT), Riverpod, GoRouter
- ML: google_mlkit_pose_detection (MediaPipe runtime, renamed to `libxeno_native.so`: PoseLandmarkWithSmoothingSubgraph, BlazeFace blaze-2024.07.30), TensorFlow Lite + GPU delegate
- Custom TFLite models in assets: `yolo11_detection.tflite` (38 MB, COCO classes), `tennis_ball.tflite`, `tennis_racket.tflite`
- FFmpeg Kit (video processing: compression, frames `-vf "fps=30,scale=640:-2"`), WebRTC (flutter_webrtc), Firebase (Auth, Storage, Firestore, FCM, Remote Config), RevenueCat + Play Billing, pairip license check

## Backend

- Cloud Run: `https://tennis-ai-analysis-v2-w4eburiouq-uc.a.run.app/v1/analysis/analyze`
- Firebase Functions: `axelModel`, `axelKnowledgeSearch`, `axelSpeak`, `coachAgent`, `drillAgent`, `knowledge`, `strokeDetect`, `trainingPlanAgent`, `translateDrillContent`, `translateUI`, `webSearch`
- Axel (AI coach) = Gemini function calling (`gemini_llm_client.dart`), TTS `gemini-3.1-flash-tts-previews`, knowledge in Firestore (domains: biomechanics, tactics, fitness, mental, drills, coaching)
- Metrics/definitions are read from Firestore: `metric_definitions`, `cohort_stats`, `amine_consignes`

## Video analysis pipeline (on-device)

Order from libapp.so log lines:
1. Recording protocol: portrait, 1080p 30fps, side view from the corner of the court, whole body in frame, 1 stroke, max 5 sec (`recording_screen`, `serve_setup` — calibration checklist "SUBJECT IN FRAME")
2. `[VIDEO] Compressed` — FFmpeg compression
3. `[OnDevice] Extracting frames...` + HQ frames for the annotated video
4. `[OnDevice] Detecting poses...` — ML Kit Pose (33 landmarks, smoothed) frame by frame
5. `[OnDevice] Detecting ball + racket with YOLO11...` (`[YOLO-REPASSE]` — second pass), keys racket_x1/y1/x2/y2
6. `[OnDevice] Detecting moments (movementType=...)` — phase detection (`moment_detector.dart`, `moment_detector_ondevice.dart`, M0 result)
7. `metrics_calculator.dart` — angle/distance computation (pixels→cm, portrait scaling)
8. Scoring: each metric is compared against the pro-player reference range → green/red, overall_score /100
9. `[OnDevice] Generating annotated video...` → skeleton overlay → upload to Firebase Storage (`annotated_video_url`)
10. Results screen: phases M0–M6, metrics, corrections, pro comparison

## Phases (moments)

- Serve: M0 Ready → M1 Stance → M2 Toss & Backswing → M3 Loading/Trophy → M4 Extension → M5 Contact Point → M6 Recovery
- Forehand/Backhand: M0 Ready Position → M1 Directional Split Step → M2 Footwork/Unit Turn → M3 Drive/Trophy → M4 Contact Point → M5 Follow-Through → M6 Recovery
- Metric codes: M1_ARMS_POSITION, M1_FOOT_ANGLE, M2_KNEES_POSITION, M2_UNIT_TURN, M2_WEIGHT_TRANSFER, M3_TRIPLE_EXTENSION, M3_TROPHY_POSITION, M4_TRUNK_ROTATION_BODY_ALIGNMENT, M5_CONTACT_POINT_HEIGHT_DISTANCE, M5_RACKET_HEAD_POSITION, KNEE_FLEXION, DOM_KNEE, NDOM_KNEE, wristspeed, weighttransfer, torso_rotation, tripleextension, etc.

## Thresholds (examples from Dart strings)

- Ankle-Hip-Shoulder 110–170°; Heel-hip-shoulder 155–179°; Non-dom elbow 102–180°; Toss arm 152–180°; Toss arm speed 1.5–6.0 m/s; Wrist speed 10–25 m/s; Shoulder-Hip 10–30°; Shoulders horizontal <12°; Racket face parallel shoulders <25° + vertical; Trunk tilt 5–39° / 10–45°; Trunk rotation 3–20 cm; Toes 3–15 cm / Shoulder 14–72 cm; Feet/shoulders ratio 0.8–1.5x; Toss height 1.8–2.3x player height; Align 171–189° AND Angle 60–130°; front foot 30–45° relative to the baseline
- Pro comparison (hard-coded tables): "(M3) Alcaraz: 165 | Sinner: 164 | Djokovic: 168 | Federer: 151", "Alcaraz: -3.8cm | Sinner: 2.2cm | Djokovic: 12.1cm | Federer: 5.0cm", "Alcaraz: 1.0x | Djokovic: 1.1x | Sinner: 1.2x", etc.

## Correction mechanism

- Metric status: green = inside the range the best players sit in, red = outside it ("red means outside the range the best players sit in and green means inside it")
- Per metric: a description ("Knee bend during loading or contact. Deep bend stores elastic energy...") + 2 correction texts (too little/too much: "Your knees are too bent at X°" / "Your knees are too straight at X°" / "Good knee flexion...")
- Realtime "LIVE CORRECT": camera → frame-by-frame ML Kit pose → metrics → correction card only for "fixable live" metrics; recalibration on M2 (`[RT-RECALIB] Calibrated on M2`); "All metrics corrected! Excellent form!", "Errors found but none fixable live. Try drills."
- Axel coach memory: `axel_coaching_memory.dart`, `_recordCorrection` — records which advice was given and whether it worked ("First time you are coaching this metric. No history yet.")

## Training mechanism

- Drills: a library (Firestore), fields objective/setup/progression/reps/sets/difficulty, targetMetrics
- Selection: `drillAgent` (cloud) + `drill_recommendation_service.dart` — based on analysis weaknesses (topWeaknesses, recurringWeaknesses, perceivedWeakness — the player's self-report), "These drills target weaknesses detected in your last analysis"
- Training plans: `trainingPlanAgent` + `training_plan_service.dart`, "Based on your analysis - personalized"
- Gamification: XP (150 XP per drill), badges, levels (beginner/intermediate/advanced/pro), currentStreak
- Axel drill walkthrough: steps Objective, Setup, How to do it, Key points, Progression, What to look for (voice guidance, 'next' — advance_drill_walkthrough)
- Coach pairing: player↔coach QR code, coach dashboard, students, WebRTC live sessions, Axel coach-mode prompts

## Axel (AI coach) — tools (function calling)

`analyze_video`, `navigate`, `open_drill_detail`, `save_player_profile`, `complete_onboarding`, `set_session_intent`, `record_correction`, `advance_drill_walkthrough`, `speak`, `listen`, `get_training_plan`, fetch_analysis_history, fetch_recommended_drills (with analysisId), set_app_language (en, fr, es, it, de, zh, hi, pt, tr, ar), restart_voice, schedule_drill_reminder, show_last_analysis
Separate prompts for coach mode (talks to the coach with data, not as with a player).

## Quotas/monetization (RevenueCat)

Free: 9 video analyses lifetime + 20 real-time sessions. Pro: ~20 video analyses/month + unlimited real-time (variant: 100/month, 1000/year). Quota checks: `quota_service.dart`, the cloud blocks on overflow.

## Analysis limitations (unknowns)

- The server side (Cloud Run analyzer, functions) is outside the APK — the exact composition of the cloud pipeline is not visible
- The exact overall_score aggregation formula (weights) was not recovered from the strings — most likely in metrics_calculator (Dart code) and/or metric_definitions in Firestore
- Phase weights and the exact ranges of all metrics are stored partially in Firestore (`metric_definitions` is read at runtime)
- TFLite model contents were not examined (the YOLO11 architecture is known from the name)
- Live voice: Gemini Live (`GeminiLiveCoach`), the exact audio transport (websocket) did not make it into the strings
