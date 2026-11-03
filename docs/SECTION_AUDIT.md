# SECTION AUDIT — поэкранная сверка клона с оригиналом через REA

Журнал поэкранной доводки `tennis-ai-android/` до 1:1 с оригиналом
`com.tennis.ai` v2.0.19. Источник истины — REA-артефакты реверса:
`reference/extracted_assets/` (en.json, libapp_strings, all_identifiers),
`reference/dart-aot-recovery/`, фото `docs/OriginalScrean/`, `docs/CLONE_SPEC.md`.

## Протокол сверки (одинаковый для каждого раздела)

1. **Досье из REA** — строки раздела из `en.json` (дословно, включая
   невыровненные i18n-ключи — дефекты оригинала воспроизводим 1:1), имена
   виджетов/сервисов из `all_identifiers.txt`, литералы из
   `libapp_strings.txt` / `long_strings.txt`.
2. **Код-сверка** — экран клона против досье и CLONE_SPEC: структура, цвета
   (`ui/theme/Theme.kt`), типографика (3 регистра), строки, навигация.
3. **Runtime-сверка** — `gradlew :app:assembleDebug` → установка на
   Pixel_7 (ADB-порт 5237, см. CLONE_SPEC §7) → скриншоты в `shots/` →
   сравнение с фото оригинала (оба состояния прокрутки, RU и EN).
4. **Правки + гейт** — расхождения фиксируются, пересборка, повторный
   скриншот; сюда же — строка в этом журнале.

Статусы: ✅ сверено, расхождений нет · 🔸 сверено, мелкие осознанные
отличия (перечислены) · ⬜ не сверялось.

## 1. Home («AXEL AI», таб 1) — ✅ (2026-10-17)

Файлы: `ui/home/HomeScreen.kt`, `ui/common/Ui.kt`, `core/i18n/T.kt`.
Скриншоты: `shots/01_home.png`, `shots/01_home_scrolled.png`.

REA-находки:
- Ключей `tag_coach`/`tag_new` в en.json нет — теги в оригинале это ключи
  `coach` ('COACH') и `new` ('NEW'). Ключи клона переименованы к оригиналу.
- Значения «Coach Mode», «Real-time analysis for coaches», «Auto Detect»,
  «Auto-detect your stroke and analyze it» в en.json отсутствуют, но есть
  литералами в libapp (ключи `coach_mode`/`auto_detect` подтверждены
  `all_identifiers.txt`) — значения клона совпадают дословно.
- `sessions` = 'sessions' → метка «0 sessions» собирается как «N sessions» ✓.

Исправлено (код-сверка + runtime):
1. Низ карточки модуля: было — линия и метка в одной строке; стало — линия
   84% ширины, метка ПОД ней справа (как в оригинале).
2. Топ-бар Home: добавлены фон `Bar` (#0A0A0A) и волосяная линия снизу.
3. Кнопка REAL-TIME ANALYSIS: убрана зелёная заливка `0xFF0C1206` — в
   оригинале только граница accent.
4. Иконка таба Analyses в bottom-nav: `MonitorHeart` (рамка с пульсом) →
   `AutoMirrored.ShowChart` (чистая пульс-линия, как в оригинале).

Осталось (не блокирует, сверить при случае):
- Вертикальные отступы карточек оригинала на ~2–4dp больше — в пределах
  допуска, фото-динамика не позволяет измерить точнее.

## 2. My Videos + My Analyses — ⬜

Известное заранее (найдено при аудите 1): `LibraryScreens.kt` использует
`MonitorHeart` в топ-баре «MY ANALYSES» и в плитках no_data — в оригинале
везде пульс-линия (`ShowChart`). Топ-бары должны быть Bar+hairline.
Фото оригинала: `photo_2026-10-17_11-55-25.jpg` (Videos),
`photo_2026-10-17_11-55-29.jpg` (Analyses).

## 3. Dashboard (Select / Player / Coach) — ⬜

Фото: `11-55-16` (Select), `11-55-07` (Player), `11-55-21` (Coach).
Проверить: невыровненные ключи `track_progression`, `manage_students_perf`,
`record_first_video` (дословно!), REFERENCE-баннер cardAccent, сетку 2×2.

## 4. Profile + Edit Profile — ⬜

Фото: `11-54-53` (Profile), `11-55-11` (Edit/Additional).
Проверить: «App Version 2.0.1» (в клоне версия? оригинал показывает 2.0.1
при пакете 2.0.19 — воспроизвести дословно), Ambassador €0, тумблеры,
сегмент General|Additional, чипы с эмодзи.

## 5. Axel — ⬜

Фото: `11-55-02` (idle), `11-55-47` (переписка).
Проверить: чип Auto/Manual, speaker, красный stop, подсказки 2×2 (тексты
обрезаны на фото — дополнены, см. CLONE_SPEC §2.2/§6.5), Record/Upload,
пузыри чата, GLM-конфиг через «App Version».

## 6. Analyze → Results — ⬜ (ядро; приоритеты в GAP_ANALYSIS P1–P3)

Сверять по `reference/dart-aot-recovery/GAP_ANALYSIS.md`: D/ND-пары
диапазонов, коды метрик 1:1 (`M1_ARMS_POSITION`…), про-сравнение (87 строк),
стадии прогресса (5 из 5), тексты фаз. Это самый крупный разрыв (~40% данных).

## 7. Live / Real-time — ⬜

GAP_ANALYSIS §C: рекалибровка на M2, ball window, SUBJECT IN FRAME,
все метрики live (клон держит только knee flexion + stance width).

## Инфраструктурные заметки

- ADB на этой машине — порт **5237** (`ANDROID_ADB_SERVER_PORT=5237`),
  координаты `input tap` — в физических пикселях 1080×2400 (Read может
  показывать скриншоты отмасштабированными — брать bounds из
  `uiautomator dump`).
- Первый старт свежего AVD пару минут занимается dexopt/Play-обновлениями —
  возможны ANR-диалоги System UI, к приложению отношения не имеют
  (гасить тапом «Wait», не «Close app»).
