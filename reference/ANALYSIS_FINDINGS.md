# Tennis AI 2.0 (com.tennis.ai) v2.0.19 — находки реверс-инжиниринга

Дата: 2026-10-16. Инструмент: REA CLI 6.5.0 + jadx-headless-mcp 0.7.1 (Java 21, Android Studio JBR).

## Идентификация цели

- Файл: `com.tennis.ai_v2.0.19.apks.apk` (243 МБ, App Bundle ZIP)
- REA Evidence: `ev_7f8b10ab273ceac2442806b5363b4ca749f9d4b8539ad360c34b927ccf5dd516`
- base.apk SHA-256: `bb423559aaf67256638694641d81a279c3e386b6bf805b0cd6de2338bc1478e3`
- Пакет: `com.tennis.ai`, versionName 2.0.19, versionCode 102, minSdk 24, targetSdk 36
- 18 860 Java/Kotlin классов; Android-сторона — тонкая обвязка Flutter (`com.tennis.ai.MainActivity` = стандартный Flutter-активити, только `onCreate`)
- Вся логика — Dart AOT (`lib/arm64-v8a/libapp.so`, 13 МБ, package `tennis_ai_2`)

## Стек

- Flutter (Dart AOT), Riverpod, GoRouter
- ML: google_mlkit_pose_detection (MediaPipe-рантайм, переименован в `libxeno_native.so`: PoseLandmarkWithSmoothingSubgraph, BlazeFace blaze-2024.07.30), TensorFlow Lite + GPU delegate
- Кастомные TFLite-модели в assets: `yolo11_detection.tflite` (38 МБ, COCO-классы), `tennis_ball.tflite`, `tennis_racket.tflite`
- FFmpeg Kit (обработка видео: компрессия, кадры `-vf "fps=30,scale=640:-2"`), WebRTC (flutter_webrtc), Firebase (Auth, Storage, Firestore, FCM, Remote Config), RevenueCat + Play Billing, pairip license check

## Бэкенд

- Cloud Run: `https://tennis-ai-analysis-v2-w4eburiouq-uc.a.run.app/v1/analysis/analyze`
- Firebase Functions: `axelModel`, `axelKnowledgeSearch`, `axelSpeak`, `coachAgent`, `drillAgent`, `knowledge`, `strokeDetect`, `trainingPlanAgent`, `translateDrillContent`, `translateUI`, `webSearch`
- Axel (AI-коуч) = Gemini function calling (`gemini_llm_client.dart`), TTS `gemini-3.1-flash-tts-previews`, знания в Firestore (домены: biomechanics, tactics, fitness, mental, drills, coaching)
- Метрики/определения читаются из Firestore: `metric_definitions`, `cohort_stats`, `amine_consignes`

## Пайплайн видеоанализа (on-device)

Порядок из лог-строк libapp.so:
1. Протокол съёмки: портрет, 1080p 30fps, боковой ракурс с угла корта, всё тело в кадре, 1 удар, макс 5 сек (`recording_screen`, `serve_setup` — калибровочный чек-лист "SUBJECT IN FRAME")
2. `[VIDEO] Compressed` — FFmpeg-компрессия
3. `[OnDevice] Extracting frames...` + HQ-кадры для аннотированного видео
4. `[OnDevice] Detecting poses...` — ML Kit Pose (33 landmark, со сглаживанием) покадрово
5. `[OnDevice] Detecting ball + racket with YOLO11...` (`[YOLO-REPASSE]` — повторный проход), ключи racket_x1/y1/x2/y2
6. `[OnDevice] Detecting moments (movementType=...)` — детекция фаз (`moment_detector.dart`, `moment_detector_ondevice.dart`, M0-результат)
7. `metrics_calculator.dart` — расчёт углов/дистанций (пиксели→см, портретный масштаб)
8. Скоринг: каждый метрик сравнивается с эталонным диапазоном профи-игроков → green/red, overall_score /100
9. `[OnDevice] Generating annotated video...` → скелет-оверлей → загрузка в Firebase Storage (`annotated_video_url`)
10. Экран результатов: фазы M0–M6, метрики, поправки, сравнение с профи

## Фазы (moments)

- Serve: M0 Ready → M1 Stance → M2 Toss & Backswing → M3 Loading/Trophy → M4 Extension → M5 Contact Point → M6 Recovery
- Forehand/Backhand: M0 Ready Position → M1 Directional Split Step → M2 Footwork/Unit Turn → M3 Drive/Trophy → M4 Contact Point → M5 Follow-Through → M6 Recovery
- Коды метрик: M1_ARMS_POSITION, M1_FOOT_ANGLE, M2_KNEES_POSITION, M2_UNIT_TURN, M2_WEIGHT_TRANSFER, M3_TRIPLE_EXTENSION, M3_TROPHY_POSITION, M4_TRUNK_ROTATION_BODY_ALIGNMENT, M5_CONTACT_POINT_HEIGHT_DISTANCE, M5_RACKET_HEAD_POSITION, KNEE_FLEXION, DOM_KNEE, NDOM_KNEE, wristspeed, weighttransfer, torso_rotation, tripleextension и др.

## Пороги (примеры из строк Dart)

- Ankle-Hip-Shoulder 110–170°; Heel-hip-shoulder 155–179°; Non-dom elbow 102–180°; Toss arm 152–180°; Toss arm speed 1.5–6.0 m/s; Wrist speed 10–25 m/s; Shoulder-Hip 10–30°; Shoulders horizontal <12°; Racket face parallel shoulders <25° + vertical; Trunk tilt 5–39° / 10–45°; Trunk rotation 3–20 cm; Toes 3–15 cm / Shoulder 14–72 cm; Feet/shoulders ratio 0.8–1.5x; Toss height 1.8–2.3x player height; Align 171–189° AND Angle 60–130°; front foot 30–45° к задней линии
- Pro-сравнение (вшитые таблицы): "(M3) Alcaraz: 165 | Sinner: 164 | Djokovic: 168 | Federer: 151", "Alcaraz: -3.8cm | Sinner: 2.2cm | Djokovic: 12.1cm | Federer: 5.0cm", "Alcaraz: 1.0x | Djokovic: 1.1x | Sinner: 1.2x" и т.д.

## Механизм поправки (correction)

- Статус метрики: green = внутри диапазона лучших игроков, red = вне ("red means outside the range the best players sit in and green means inside it")
- На каждую метрику: описание ("Knee bend during loading or contact. Deep bend stores elastic energy...") + 2 коррекционных текста (слишком мало/слишком много: "Your knees are too bent at X°" / "Your knees are too straight at X°" / "Good knee flexion...")
- Realtime "LIVE CORRECT": камера → покадровый ML Kit pose → метрики → карточка поправки только для "fixable live" метрик; рекалибровка на M2 (`[RT-RECALIB] Calibrated on M2`); "All metrics corrected! Excellent form!", "Errors found but none fixable live. Try drills."
- Axel-памятьcoach: `axel_coaching_memory.dart`, `_recordCorrection` — записывает какой совет дал и сработал ли ("First time you are coaching this metric. No history yet.")

## Механизм обучения (training)

- Drills: библиотека (Firestore), поля objective/setup/progression/reps/sets/difficulty, targetMetrics
- Подбор: `drillAgent` (облако) + `drill_recommendation_service.dart` — по weaknesses анализа (topWeaknesses, recurringWeaknesses, perceivedWeakness — самоотчёт игрока), "These drills target weaknesses detected in your last analysis"
- Тренировочные планы: `trainingPlanAgent` + `training_plan_service.dart`, "Based on your analysis - personalized"
- Геймификация: XP (150 XP за drill), бейджи, уровни (beginner/intermediate/advanced/pro), currentStreak
- Axel walkthrough drill: шаги Objective, Setup, How to do it, Key points, Progression, What to look for (голосовое ведение, 'next' — advance_drill_walkthrough)
- Coach-связка: QR-код игрок↔тренер, coach dashboard, студенты, WebRTC live-сессии, coach-режим промпты Axel

## Axel (AI-коуч) — инструменты (function calling)

`analyze_video`, `navigate`, `open_drill_detail`, `save_player_profile`, `complete_onboarding`, `set_session_intent`, `record_correction`, `advance_drill_walkthrough`, `speak`, `listen`, `get_training_plan`, fetch_analysis_history, fetch_recommended_drills (с analysisId), set_app_language (en, fr, es, it, de, zh, hi, pt, tr, ar), restart_voice, schedule_drill_reminder, show_last_analysis
Отдельные промпты для coach-режима (говорит с тренером данными, не как с игроком).

## Квоты/монетизация (RevenueCat)

Free: 9 видеоанализов навсегда + 20 real-time сессий. Pro: ~20 видеоанализов/мес + unlimited real-time (вариант: 100/мес, 1000/год). Проверка квот: `quota_service.dart`, облако блокирует при переполнении.

## Ограничения анализа (unknowns)

- Серверная часть (Cloud Run анализатор, функции) вне APK — точный состав обл. пайплайна не виден
- Точная формула агрегации overall_score (веса) не восстановлена из строк — вероятно в metrics_calculator (Dart-код) и/или metric_definitions в Firestore
- Весовые коэффициенты фаз, точные диапазоны всех метрик хранятся частично в Firestore (`metric_definitions` читается в рантайме)
- Содержимое TFLite-моделей не разбиралось (архитектура YOLO11 известна из имени)
- Live voice: Gemini Live (`GeminiLiveCoach`), точный транспорт аудио (websocket) в строки не попал
