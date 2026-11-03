# Карта портирования: Dart (оригинал) → монорепа

Источник имён: `reference/ANALYSIS_FINDINGS.md` + строки `reference/extracted_assets/libapp_strings.txt`.
Статусы: ✅ сделано · 🔶 каркас готов, логика TODO · ⬜ не начато.

| Dart-сервис оригинала | Куда портировано | Статус |
|---|---|---|
| `pose_service.dart` (ML Kit 33 landmark) | `apps/miniapp/src/services/poseService.ts` + `videoPipeline.ts` (`@mediapipe/tasks-vision@1.1.1`, VIDEO-режим = встроенный трекинг, GPU→CPU fallback, модель lite float16) | ✅ v0; смоук на живом видео ⬜ |
| `yolo_detector_service.dart` / `ball_detection_service.dart` | там же → onnxruntime-web (int8) | 🔶 интерфейс готов |
| `calibration_service.dart` (px → см) | `Calibration.cmPerPixel` в `@tennis-ai/shared` | 🔶 поле есть, калибровка ⬜ |
| `moment_detector*.dart` (M0–M6) | `packages/engine/src/phases.ts` | 🔶 heuristic v0 |
| `metrics_calculator.dart` | `packages/engine/src/measurements.ts` + `metrics.ts` | 🔶 17 калькуляторов из 24; пороги из реверса |
| Пороги Firestore `metric_definitions` | `packages/shared/src/metrics/metrics.json` | ✅ восстановленное; остальное — unknown-range |
| Скоринг green/red + overall | `packages/engine/src/scoring.ts` | ✅ правило; веса = v0-аппроксимация |
| `video_annotation_service.dart` (FFmpeg) | клиентский canvas + MediaRecorder → fallback серверный | ⬜ |
| Компрессия видео (FFmpeg Kit) | сервер (fluent-ffmpeg, 720p/crf 28) | ⬜ воркер в API |
| `stroke_detection_service.dart` | `POST /api/analysis/detect-stroke` (Gemini vision) | ✅ |
| `quota_service.dart` (9/20) | `apps/api/src/modules/users/users.service.ts` | ✅ |
| `drill_recommendation_service.dart` | `apps/api/src/modules/drills` | ✅ v0 (intersect weaknesses×targetMetrics) |
| `training_plan_service.dart` | `apps/api/src/modules/plans` | ✅ v0 |
| `axel/*` (Gemini function calling, память) | `apps/api/src/modules/coach` | 🔶 прокси + инструменты; память коуча ⬜ |
| LIVE CORRECT (realtime) | ожидает спайка `getUserMedia` в Telegram WebView (DECISIONS §6.3) | ⬜ |
| Firebase Auth/Storage/Firestore | Nest + Mongo + S3 (осознанная замена, не порт) | ✅ каркас |
| RevenueCat + Play Billing | Telegram Stars (осознанная замена) | ✅ каркас |
| pairip license check | не нужен (TMA = initData-подпись) | — |
| UI-клоны экранов оригинала (CLONE_SPEC) | Android: 1:1 (SECTION_AUDIT); TMA: рескин 2026-10-18 | ✅ v0 TMA; сверка скриншотами ⬜ |

## VIDEO PREVIEW + нативный TTS (2026-10-18)

Порт `video_preview_screen.dart` в TMA (evidence: `reference/extracted_assets/`
libapp_strings, en.json, long_strings):

- Флоу: выбор видео в `/record` → `/preview` — зацикленный `<video>` (порт
  `setLooping`), гейт качества («CHECKING VIDEO...», ландшафт с дословным текстом
  «rotating the file afterwards does not fix it», квадратный кроп, <360px →
  «VIDEO TOO LOW QUALITY» + RETAKE), «TRIM YOUR STROKE» — RangeSlider с двумя
  ручками (порт `_previewTrim`/`trimStartSec`), три действия ANALYZE/RETAKE.
- Окно трима режет кадровую выборку позы (startSec/endSec в `videoPipeline`),
  сам оригинал грузится целиком; `trimStartSec/trimEndSec` сохраняются в сессии
  (`shared/api.ts` + analyses schema/DTO/service).
- Дефект UX оригинала воспроизведён: подписи «ручки тащатся» на экране нет —
  её произносит голос (см. TTS).
- **Нативный TTS** (`services/tts.ts`, Web Speech API): одноразовый голосовой
  гид экрана ревью (порт `axel_recording_guide_shown_`), озвучка ответов Axel
  по тумблеру speaker (ttsOn в store), красный stop глушит речь. В Android-клоне
  TTS пока флаг без движка (CLONE_SPEC §6.4) — зеркало на Kotlin TextToSpeech TODO.
- Из разобранного реверса осталось на потом: скачивание/share/RE-ANALYZE в My Videos,
  live-элементы.

## Results-плеер: перемотка по фазам (2026-10-18)

Порт видеоплеера `results_screen.dart` в `AnalysisView.vue` (evidence:
`_seekToMoment`, `_applyPlaybackSpeed`, `PlaySpeedReset`, `isLooping`,
`annotated_video_url` в строках libapp.so):

- Плеер над карточкой балла: зацикленный `<video>` (порт `isLooping`), muted+playsinline.
- Чипы фаз M0–M6 теперь и выбирают фазу, и перематывают клип на таймкод фазы —
  порт `_seekToMoment`: `currentTime = trimStartSec + startFrame / fps`
  (кадры сэмплированы от начала окна трима, поэтому в отчёте `frame/fps`
  отсчитывается от `trimStartSec`).
- Скорости 0.25x/0.5x/1x под плеером — порт `_applyPlaybackSpeed`
  (`playbackRate`). Точный набор скоростей оригинала из строк не восстановлен —
  ближайшая реконструкция по `PlaySpeedReset`.
- URL плеера: новый `GET /api/analyses/:id/video` — presigned GET (S3Service
  `presignGet`), играет `videos.annotated`, пока его нет — `videos.original`
  (порт `annotated_video_url`; аннотированное видео — порт ⬜, шаг «серверный
  ffmpeg-воркер»). Контракт `AnalysisVideoResponse` в `shared/api.ts`,
  клиент — `analysis.fetchVideoUrl(id)`.

## My Videos: действия над сессией (2026-10-18)

Порт действий `video_library_screen.dart` в `VideosView.vue` (evidence: ключи
en.json `re_analyze`, `delete_video`+`delete_video_confirm`, `video_deleted`,
`download_failed`, `video_saved_to`, `share_plus` в манифесте):

- Карточка сессии в бакете: Open + строка действий RE-ANALYZE / Share /
  Download / Delete Video (красная). Busy-состояние «Preparing…» на время
  скачивания blob.
- **RE-ANALYZE**: оригинал сессии (presigned GET → File, стор
  `analysis.fetchVideoFile`) кладётся в `pendingPreview` и открывается `/preview`
  — тот же флоу ревью/трима/анализа, что и у нового видео; квота списывается при
  создании сессии, как в оригинале.
- **Share** (порт share_plus): `navigator.share({ files })` при доступности
  (canShare), иначе скачивание как фолбэк; отмена системного листа — не ошибка.
- **Download**: anchor+blob, тост `video_saved_to` / при ошибке
  `download_failed`.
- **Delete Video**: подтверждение оригинала (`delete_video_confirm`, кнопки
  CANCEL/DELETE), тосты `video_deleted`/`delete_failed`; каскад — существующий
  `DELETE /analyses/:id`.
- Тот же паттерн подтверждения добавлен в My Analyses (ключи оригинала
  `delete_session` + `delete_session_confirm` — раньше удаляло без диалога).

## LIVE CORRECT: спайк getUserMedia + live-цикл (2026-10-18)

`LiveView.vue` переписан из заглушки в рабочий live-режим (порты: клон
`LiveAnalysisScreen.kt`, строки оригинала `en.json`/libapp):

- **Спайк getUserMedia**: в Chromium (headless, `--use-fake-device-for-media-stream`)
  полный флоу `/live` проходит — старт → «Get in position. 15 seconds.» →
  live-фид с чипами → стоп → итог; MediaPipe Pose грузится по GPU-делегату,
  `detectForVideo` крутится без ошибок. Открытый вопрос — тот же флоу внутри
  Telegram WebView на iOS/Android (проверить на реальном устройстве).
- Флоу: idle (выбор удара чипами + «START LIVE ANALYSIS») → prepare 15 c
  («Get in position. 15 seconds.» → «Go!», дословно) → live → summary.
- Live-цикл: `getUserMedia(facingMode: environment)` с фолбэком на фронталку
  (зеркалится), rAF + `detectForVideo` (~15 fps), скелет BlazePose на canvas
  поверх фида (FILL_CENTER, порт SkeletonOverlay клона), чипы REC /
  HD @ 30FPS / AI TRACKING: ACTIVE / SUBJECT IN FRAME ✓.
- Метрики live: `METRIC_DEFINITIONS` выбранного удара с кадровыми
  калькуляторами и восстановленным proRange; первая метрика вне диапазона —
  красная карточка «LIVE CORRECTION» с 15-секундным окном на исправление
  (порт countdown оригинала), возврат в диапазон засчитывается как corrected.
  Скорости/мяч (YAML null-range и не-кадровые калькуляторы) в live не участвуют.
- Итог (дословные строки libapp): «All metrics corrected! Excellent form!»
  либо «Errors found but none fixable live. Try drills.» + счётчики
  corrected/errors, кнопки Try again / Drills. TTS озвучивает поправки и итог
  при включённом спикере (`app.ttsOn`).
- Отказ камеры/отсутствие API → прежняя заглушка + «Camera permission required».
- **Dev-offline** (`VITE_DEV_OFFLINE=1`, только в DEV-сборке): auth-стор выдаёт
  мок-пользователя без Telegram initData — локальный запуск `pnpm dev:miniapp`
  вне Telegram для всех экранов. Спайк гонялся именно в этом режиме.

## Рескин TMA по CLONE_SPEC (2026-10-18)

`apps/miniapp` приведён к дизайн-системе и экранам клона (источники —
`tennis-ai-android` после аудита 2026-10-17 + `docs/CLONE_SPEC.md`):

- Дизайн-система в `src/styles.css` (палитра §1.1, три регистра типографики §1.2),
  примитивы — `src/components/ui.ts` + `Icon.vue` (порт `ui/common/Ui.kt`).
- Иконки — официальные Material Symbols outlined (weight 400), те же глифы, что
  `Icons.Outlined.*` клона: реестр `src/components/materialIcons.ts` генерируется
  `apps/miniapp/scripts/icon-fetch.mjs` из `@material-symbols/svg-400@0.48.0`
  (Apache-2.0). Для «MY ANALYSES» взят `show_chart` (пульс-линия) — по аудиту §2
  SECTION_AUDIT так в оригинале.
- Навигация §3: bottom-nav home|videos|analyses|dashboard|profile + FAB Axel;
  select/edit/axel/record/live/analysis/drills/paywall — fullscreen без навигации.
- Экраны: Home (6 модулей с кольцами и метками COACH/NEW/N sessions), My Videos,
  My Analyses (бакеты serve blue/forehand lime/backhand amber/drills red,
  `no_data` воспроизводится дословно), Select/Player/Coach Dashboard
  (невыровненные ключи `track_progression`/`manage_students_perf`/`record_first_video`
  — дословно), Profile + Edit Profile (General|Additional, чипы, пересчёт Progress),
  Axel (голограф, подсказки 2×2, пузыри, чип Auto/Manual, красный stop = AbortController).
- i18n: `src/i18n/T.ts` — порт `core/i18n/T.kt`; RU по умолчанию, EN дословно из
  `reference/extracted_assets/en.json`; выбор языка в Settings.
- Состояние: `src/stores/app.ts` — порт `AppState.kt` (профиль/настройки/lang,
  localStorage ≈ SharedPreferences).

Осознанные отклонения TMA от клона (продуктовая замена, не расхождение с оригиналом):

1. Axel говорит через `POST /api/coach/chat` (ключ на сервере), а не GLM-конфиг
   из «App Version»; `set_app_language` переключает язык приложения.
2. Карточка «Free Plan» ведёт на Stars-пейволл `/paywall` (в клоне — Edit Profile).
3. Счётчики дашбордов/бакетов — реальные данные API (список анализов), а не нули;
   тап по бакету раскрывает список сессий (Открыть/Удалить).
4. Live — стилизованная заглушка до спайка `getUserMedia` (см. шаг 2 ниже).

## Ближайшие шаги (по приоритету)

1. ~~Подключить `@mediapipe/tasks-vision`~~ ✅ (2026-10-16): `poseService.ts` + потоковый
   `analyzeVideoPoses` (seek → canvas → `detectForVideo`), кадры не буферизуются. WASM/модель —
   CDN по умолчанию, самохостинг через `VITE_MEDIAPIPE_WASM_URL` / `VITE_POSE_MODEL_URL`.
   Осталось: смоук на реальном видео в Telegram WebView.
2. ~~Спайк `getUserMedia`~~ ✅ в браузере (2026-10-18, LiveView live-цикл);
   осталось проверить тот же флоу внутри Telegram WebView (iOS/Android/desktop).
3. Портировать точные веса `overall_score`, когда формула будет восстановлена
   (или зафиксировать свою калибровку на живых данных).
4. Серверный ffmpeg-воркер компрессии + аннотированное видео.
5. Заменить heuristic-фазы `phases.ts` на порт логики `moment_detector_ondevice`
   (последовательности порогов движения по кадрам из строк `libapp_strings.txt`).
