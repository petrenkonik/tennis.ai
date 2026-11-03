# Аудит переноса: элементы анализа оригинала vs Android-порт (tennis-ai-android)

Дата: 2026-10-17. Источник истины: реверс v2.0.19 (`reference/extracted_assets/`,
`reference/dart-aot-recovery/`). Статусы: ✅ перенесено точно · ⚠️ перенесено
приближённо · ❌ отсутствует.

## Вердикт

Ядро механики перенесено верно (фазы M0–M6, правило red/green против про-диапазонов,
скоринг /100, пайплайн «кадры → поза → фазы → метрики → отчёт»), но **набор метрик
и данные покрыты на ~40%**: в оригинале ~40+ определений метрик с попарными
доминантными/недоминантными диапазонами, метрики мяча и ракетки (YOLO),
классификация стойки, cm-масштаб через рост игрока и таблицы сравнения с профи —
этого в порте нет или есть в упрощённом виде.

## A. Пайплайн анализа

| Элемент | Оригинал | Порт | Статус |
|---|---|---|---|
| Протокол съёмки | портрет 1080p/30, боковой ракурс, всё тело, ≤5 c, один удар | те же тексты на AnalyzeScreen | ✅ |
| Калибровочный чек-лист | SUBJECT IN FRAME, «phone at waist height, 3m away», «sideline & baseline intersection» (`calibration_service`) | текст-подсказка, чек-листа нет | ⚠️ |
| Проверки клипа | TOO LOW QUALITY; ландшафт («rotating the file afterwards does not fix it»); спина к камере; квадратный кроп (искажает px→cm); «seeing your back, not your side» | нет | ❌ |
| Трим клипа | draggable handles, `_trim.mp4` | нет | ❌ |
| Авто-детекция удара | 1 кадр → CF `strokeDetect` (serve/forehand/backhand), `AutoDetectFlowScreen` | нет (ручной выбор чипами) | ❌ |
| Сжатие видео | FFmpeg 720p/libx264/crf28 → `compressed.mp4` | нет (только downscale кадров при извлечении) | ❌ |
| Извлечение кадров | fps=30, scale=640:-2, `frame_%04d.jpg` + HQ-кадры для оверлея | fps guess + ≤150 кадров, 640px | ⚠️ |
| Детекция позы | ML Kit Pose (MediaPipe, 33 landmark) со сглаживанием (`PoseLandmarkWithSmoothing`) | MediaPipe Pose Landmarker full, GPU→CPU, без явного smoothing | ⚠️ |
| YOLO мяч+ракетка | `yolo11_detection.tflite` + `tennis_ball.tflite` + `tennis_racket.tflite`; keypoints ракетки x1y1x2y2; второй проход «YOLO-REPASSE» | нет | ❌ |
| Детекция фаз | M0–M6 (`moment_detector[_ondevice]`), RT-рекалибровка на M2 | та же структура фаз; эвристики свои (toss apex / peak wrist speed) | ⚠️ |
| Скоринг | green = в про-диапазоне, red = вне; overall /100, веса из `metric_definitions` (Firestore) | правило 1:1; веса — собственные (оригинальные не восстановлены) | ⚠️ |
| Аннотированное видео | скелет на HQ-кадрах → mp4 (`annotated_video_url`) | нет | ❌ |
| Стадии прогресса | DETECTING KEYPOINTS → CALCULATING JOINT ANGLES → MAPPING TORSO ROTATION / ANALYZING KNEE FLEXION → FINALIZING REPORT | 3 из 5 стадий | ⚠️ |
| Геймификация | «You earned 150 XP and unlocked a new badge» (`celebration_screen`) | нет | ❌ |
| Квоты | free 9 анализов / pro 20 в месяц / 1000 в год (`quota_service`) | нет (нет бэкенда) | ❌ |
| История сессий | sessionId, MY VIDEOS, каскадное удаление | только текущий результат в памяти | ⚠️ |

## B. Метрики (главный разрыв)

Перенесено 15 определений (`core/metrics/MetricDefs.kt`). В оригинале:

**1. Фазовые коды (1:1 именование):**
`M1_ARMS_POSITION`, `M1_FOOT_ANGLE`, `M2_KNEES_POSITION`, `M2_UNIT_TURN`,
`M2_WEIGHT_TRANSFER`, `M3_TRIPLE_EXTENSION`, `M3_TROPHY_POSITION`,
`M4_TRUNK_ROTATION_BODY_ALIGNMENT`, `M5_CONTACT_POINT_HEIGHT_DISTANCE`,
`M5_RACKET_HEAD_POSITION` + коды `KNEE_FLEXION`, `DOM_KNEE`, `NDOM_KNEE`,
`ALIGNMENT`, `CONTACT_POINT`, `FOLLOW_THROUGH`, `RACQUET_PATH`.
→ В порте имена свои; переименовать к 1:1.

**2. D/ND-пары диапазонов по фазам (18 пар):** `(D10-175/ND40-180)`,
`(D30-110/ND30-95)`, `(D40-120/ND20-135)`, `(D65-160/ND50-145)`,
`(D85-180/ND95-180)`, `(D90-180/ND40-180)`, `(D75-175/ND50-160)`,
`(D60-175/ND20-170)`, `(D65-175/ND65-170)`, `(D65-130/ND65-145)`,
`(D80-165/ND90-175)`, `(D95-180/ND85-170)`, `(D70-165/ND30-180)`,
`(D0-50/ND0-50)`, `(D0-75/ND0-60)`, `(D3-85/ND1-80)`, `(D30-125/ND2-125)`,
`(D5-75/ND5-75)` — доминантный и недоминантный сустав считаются раздельно.
→ В порте большинство углов считаются только по доминантной стороне.

**3. Пороги, которых нет в порте:** `(100-180)`, `(109-180)`, `(130-162)`,
`(140-175)`, `(20-170)`, `(75-120)`, `(80-180)`, `(85-160/166/170/175)`,
`(90-165)`, `(R 155-178)`, `(R:100-179)`, `(K110-180/H100-180)`,
`(K120-180/H100-150)`, `Arm cocked 40-120`, `Front chain 2-33`,
`Depth 15-90cm`, `7-30cm`.

**4. Мяч (нет в порте — нужен YOLO):**
- высота выхода мяча из руки: «above eye level (player height −10cm, −5..+25cm)»
- вершина подброса: «1.8–2.3x player height»
- контакт: «H:50–165cm D:10–100cm, 0–100 open» / «D:10–80cm, 0–80 open»
- глубина/высота контакта: `Depth 15–90cm`, `7–30cm`
- высота прыжка на подаче (`jumpheight`)

**5. Ракетка (нет в порте):** face angle в замахе («controls spin type»);
face ∥ плечам <25° на trophy; head over wrist; lag («wrist leads racket head at
contact»); head drop below ball; траектория (`racquetpathtrajectory`); ускорение
(`racketacceleration`).

**6. Классификация стойки (нет в порте):** open / semi-open / closed / neutral на
контакте, с про-примерами: «Djokovic: Closed | Sinner: Semi-open | Wawrinka:
Closed» (бэкхенд), «Djokovic: Semi-open | Alcaraz: Open | Sinner: Semi-open».

**7. Готовность (M0):** «arms hanging too low… racket at chest height, elbows
slightly away» (форхенд), «both hands on the racket at chest height, non-dominant
hand on the throat» (бэкхенд) — в порте M0 только колени/стойка.

**8. Приземление/ноги:** «Back leg angle during landing. Stable landing on front
foot», `landingontheexternalfoot`, `landingoutsidefoot`, `rearlegalignment`,
`backfootalignment`, `pushoftheinternalfoot`.

**9. Прочее:** `split_step` как измеряемый элемент; `kinetic_chain(_principle)`;
«Body center offset from the baseline. Should be centered and balanced»;
«Horizontal alignment of both shoulders… level during stance»; рост игрока
(`height_cm`, `player_height_cm`) — база cm-масштаба; в порте рост не спрашивается,
см-метрики отсутствуют.

**10. Сравнение с профи:** **87 строк** значений по метрикам×фазам (Sinner,
Djokovic, Federer, Alcaraz, Wawrinka): «(M3) Alcaraz: 165 | Sinner: 164 |
Djokovic: 168 | Federer: 151», «Alcaraz: −3.8cm | Sinner: 2.2cm | Djokovic:
12.1cm | Federer: 5.0cm», «Alcaraz: 1.0x | Djokovic: 1.1x | Sinner: 1.2x»,
«Alcaraz: 28cm (ratio 0.8x) | Sinner: 66cm (ratio 1.9x)».
→ В порте экран Results показывает только пояснение, без значений.

**11. Точность перенесённых порогов:** совпадают 1:1 — knee 110–170,
heel-hip-shoulder 155–179, ankle-hip-shoulder 110–170, toss arm 152–180,
non-dom elbow 102–180, trunk tilt 5–39/10–45, wrist speed 10–25 m/s, toss 1.5–6.0
m/s, shoulders <12°, stance 0.8–1.5x. Приближение (восстановлено косвенно):
trophy coil, unit turn, contact in front, weight transfer, triple extension.

## C. Real-time

| Элемент | Оригинал | Порт | Статус |
|---|---|---|---|
| Все метрики live | «Auto-detect stroke & coach all metrics» | только knee flexion + stance width | ⚠️ |
| Рекалибровка | `[RT-RECALIB] Calibrated on M2` | нет | ❌ |
| Ball window | `[RT-BALL-WINDOW]` | нет (нет YOLO) | ❌ |
| SUBJECT IN FRAME live | да | нет | ❌ |
| Карточки поправок | fixable-live метрики, «All metrics corrected!» | есть (2 метрики) | ⚠️ |

## D. Отчёт (Results)

| Элемент | Оригинал | Порт | Статус |
|---|---|---|---|
| Фазы-табы + разбор | M0–M6 tap-through | есть | ✅ |
| Тексты «WHAT THIS MOMENT IS FOR» | свои на каждую фазу | свои (не дословные) | ⚠️ |
| Поправки | пофазные, по метрике, направление ошибки | есть (общие на метрику) | ⚠️ |
| Pro comparison со значениями | 87 строк значений | нет (только пояснение) | ❌ |
| Ход анализа для Axel | moments + red metrics JSON в чат | есть (Android Axel) | ✅ |

## Приоритеты доведения до 1:1

- **P1 (данные и точность):** ввод роста игрока + cm-масштаб; D/ND-пары в
  MetricDefs; переименование метрик к кодам оригинала; полная таблица порогов
  (45 значений выше); классификация стойки; про-значения (87 строк) в Results.
- **P2 (пайплайн):** проверки качества клипа (ландшафт/спина/качество/кроп);
  авто-детекция удара — реализуема на GLM vision (клиент уже есть в AxelScreen);
  live: все метрики, рекалибровка на M2; сглаживание позы.
- **P3 (оснастка):** YOLO мяч/ракетка (обучить на открытых датасетах), метрики
  мяча/ракетки, аннотированное видео, XP/celebration, трим, история сессий.
