# CLONE SPEC — Axel AI (com.tennis.ai v2.0.19) → tennis-ai-android

Полный UI/UX-анализ оригинала для точного клонирования. Источники: 8 скриншотов
оригинала (2026-10-17) + 11 фото `docs/OriginalScrean/` (3 из них уникальны —
Home в двух прокрутках и Axel в режиме переписки) + реверс
`reference/ANALYSIS_FINDINGS.md` и `reference/dart-aot-recovery/`.

Целевой проект: `tennis-ai-android/` (Kotlin + Compose, `com.rebuild.tennisai`).

---

## 1. Дизайн-система

### 1.1. Палитра

| Токен | Значение | Где встречается |
|---|---|---|
| `bg` | `#050505` | фон всех экранов |
| `bar` | `#0A0A0A` + нижняя/верхняя волосяная линия `#1F1F22` | топ-бары, bottom nav |
| `card` | `#141416`, граница `#26262A`, радиус 18–20 | все карточки |
| `cardAccent` | тёмно-зелёная заливка `#141F0C` + граница `#3F5A22` | Ambassador, реферальный баннер, активные элементы |
| `accent` | `#9ACD32` (лайм) | заголовки, иконки, тумблеры, активные чипы, пузырь пользователя |
| `accentDeep` | `#7CB342` | тумблеры, заливки кнопок |
| `textPrimary` | `#F2F2F2` | основные тексты |
| `textSecondary` | `#8E8E93` | подписи, подзаголовки |
| `textDim` | `#5A5A5E` | вторичные подписи карточек |
| Serve blue | `#4DA3FF` | модуль Serve Analysis / карточка Serve |
| Forehand lime | `#9ACD32` | модуль Forehand |
| Backhand amber | `#F5A623` | модуль Backhand, счётчик Analyzing |
| Drills red | `#E5544B` | модуль AI Drills / Drills |
| Coach orange | `#F0793D` | модуль Coach Mode |
| Stop red | `#EB4B3B` | кнопка stop в Axel |

Цветовые роли карточек Analyses/Videos = цвет границы (45% альфа), цвет иконки
и цвет «no_data» совпадают.

### 1.2. Типографика — три регистра

1. **Serif display с зелёным glow** — крупные заголовки: «AXEL AI» (Home),
   «SELECT DASHBOARD», «My Profile». Капс или как в оригинале; тень-свечение
   `Shadow(accent, blur≈18)`.
2. **Monospace с трекингом 2–4sp** — «технические» заголовки: топ-бары
   («AXEL AI», «COACH DASHBOARD», «MY ANALYSES», «MY VIDEOS»), секции
   («TRAINING MODULES», «Settings» — нет, Settings обычный), метки карточек
   («PLAYER DASHBOARD», «COACH DASHBOARD», «TRAINING PLANS», «MESSAGES»,
   «VIDEOS», «no_data», «BACK», «NEW», «COACH»), лейблы полей Edit Profile
   («SKILL LEVEL», «YEARS OF EXPERIENCE», «DOMINANT HAND», «MAIN GOAL»).
3. **Sans (Roboto)** — весь остальной текст.

### 1.3. Общие элементы

- **FAB Axel** — круг 56dp, фон `#0C0F08`, зелёная тонкая граница, иконка
  «sparkles» зелёная; справа снизу на всех таб-экранах (на Axel его нет). Открывает Axel.
- **Bottom nav** — 5 табов: Home (house), My Videos (videocam), My Analyses
  (activity/pulse), Dashboard (grid 2×2), Profile (person). Активный — зелёный
  с зелёной чертой 24×3dp у ВЕРХНЕГО края бара; неактивный `#6E6E73`.
- **Топ-бар** — высота ~56dp, по центру иконка+тайтл, фон `#0A0A0A`.
- **Секционный заголовок** — зелёная вертикальная черта 4×18dp + белый bold
  («Recent Activity», «Settings»).

---

## 2. Инвентаризация экранов (9 уникальных)

### 2.1. Home («AXEL AI») — таб 1

- Топ-бар: chevron-left (декоративный), по центру serif «AXEL AI» с glow.
- Кнопка **REAL-TIME ANALYSIS** — на всю ширину, граница accent 1dp, радиус 14,
  иконка `broadcast`/`((•))`, текст monospace accent. → Live-экран.
- Метка секции: «TRAINING MODULES» monospace gray + линия до края.
- **Модульные строки** (карточки, радиус 18): слева иконка-плитка 52dp
  (цветная заливка 14% + цветная иконка), заголовок белый 18sp semibold,
  подзаголовок gray 14sp; справа кольцо-прогресс (Canvas, 44dp, серый трек,
  цветная дуга = %, в центре «0%» цветом); внизу тонкая полоса-прогресс и
  справа под ней monospace-метка:
  | Модуль | Цвет | Иконка | Подзаголовок | Метка |
  |---|---|---|---|---|
  | Coach Mode | orange | clipboard | Real-time analysis for coaches | COACH |
  | Auto Detect | lime | scan/frame | Auto-detect your stroke and analyze it | NEW |
  | Serve Analysis | blue | videocam | Full Motion & Posture | 0 sessions |
  | Forehand | lime | play | Rotation & Extension | 0 sessions |
  | Backhand | amber | play | 1 or 2 Handed Technique | 0 sessions |
  | AI Drills | red | dumbbell | Personalized Exercises | 0 sessions |
- Навигация: Coach Mode/REAL-TIME → Live; Serve/Forehand/Backhand → анализ с
  предустановленным ударом; Auto Detect → анализ (ручной выбор); AI Drills → Axel.

### 2.2. Axel (чат-коуч, fullscreen)

- Шапка: зелёная иконка «sparkles» + «Axel» 22sp bold + **чип режима**
  (pill, граница оливковая, текст `#C6E36A`, значения Auto/Manual — тап
  переключает); справа: иконка speaker (TTS вкл/выкл), **красная кнопка stop**
  (скруглённый квадрат), chevron-down (свернуть = назад).
- **Idle-состояние**: hologram-блок (в оригинале — рендер неонового человека,
  бирюзово-зелёный на чёрном; в клоне — Canvas-рисунок: свечение, голова,
  плечи, воротник поло, частицы), затем «**Meet** **Axel AI**» (Meet — белый,
  Axel AI — accent, 34sp bold), подпись «Your intelligent tennis companion.»
  gray, сетка 2×2 карточек-подсказок (радиус 16): тайтл белый bold 17sp + body
  gray 14sp:
  1. «Tell Axel about your game» / «Tell me a bit about your tennis — how would you describe your game?» → отправляет текст в чат
  2. «Beat the nerves» / «Before an important match»
  3. «How's a top pro playing?» / «Current form and recent results» (в скриншоте обрезано — дополнено)
  4. «Analyse my first stroke» / «Send a video for full analysis» (обрезано — дополнено)
- **Нижняя панель ввода**: слева две круглые кнопки с подписями «Record»
  (videocam → экран анализа/записи) и «Upload» (upload → галерея); центр —
  pill-инпут «Reply to Axel...» (#1B1B1D, радиус 28); справа «Speak» (mic) и
  «Send» — ЗЕЛЁНЫЙ круг с тёмной иконкой send.
- **Переписка** (фото 11-55-47): пользователь — пузырь accent справа, тёмный
  текст; Axel — пузырь `#1B1B1D` слева, белый текст. Тексты вопросов — из
  карточек-подсказок.
- GLM-конфигурация (ключ/baseURL/model) перенесена в строку «App Version» на
  Profile (в оригинале не видна; в клоне нужен доступ — задокументированное
  отклонение).

### 2.3. Select Dashboard — таб 4 (корень)

По центру, без топ-бара: большая зелёная иконка grid 2×2 (56dp), serif
«SELECT DASHBOARD» с glow, две карточки (радиус 20):
- плитка-иконка 56dp (тёмно-зелёная заливка, зелёная иконка person/people),
  «PLAYER DASHBOARD» monospace accent + «Track your personal progress and
  sessions» gray, chevron right;
- «COACH DASHBOARD» + «manage_students_perf» (в оригинале — НЕВЫРАВНЕННЫЙ
  i18n-ключ, клонируем дословно), chevron.
Ниже по центру «BACK» monospace gray.

### 2.4. My Dashboard (player) — после выбора Player

- Топ-бар: иконка globe (accent) + «AXEL AI» monospace БЕЛЫЙ + справа иконка QR (accent).
- «My Dashboard» accent 28sp bold по центру, под ним «track_progression» gray
  (в оригинале невыровненный i18n-ключ — дословно).
- Сетка 2×2 стат-карточек: зелёная иконка сверху, число 32sp white, метка gray:
  «My Sessions» (target, 0), «Skill Analysis» (bar-chart, «—»),
  «TRAINING PLANS» (monospace, doc, 0), «MESSAGES» (monospace, chat, 0).
- Реферальный баннер (cardAccent): gift-иконка, «Get 1 year free» белый bold,
  «Invite friends → unlock free months» gray, chevron.
- Секция «Recent Activity» (черта+тайтл), карточка-пустое состояние:
  иконка videocam gray, «No sessions yet» white, «record_first_video» gray
  (i18n-ключ, дословно).
- FAB.

### 2.5. Coach Dashboard

Топ-бар: person-иконка + «COACH DASHBOARD» monospace white. Заголовок
«Welcome, Coach!» accent, «Student overview» gray. Стат-карточки: My Students
(0), Analysis («—»), TRAINING PLANS (0), MESSAGES («—»). Баннер: «Add 10
students → unlock free months». Recent Activity: people-иконка, «No student
activity yet», «Add students to see their activity». FAB.

### 2.6. My Profile — таб 5

- Топ-бар: chevron-back, person-иконка + serif «My Profile» accent.
- **Ambassador Program** (cardAccent, радиус 20): зелёная плитка 52dp с people,
  «Ambassador Program» белый bold 20sp, «Share and get rewarded» gray, справа
  «€0» accent 26sp.
- Сетка 2×2 мини-карточек (иконка accent + текст white 15sp):
  «Level: Beginner» (bar-chart), «New player» (clock), «Goal: Improve» (target),
  «Progress: 78%» (trending-up).
- **Free Plan**: карточка с 🎾 (эмодзи), «Free Plan» bold, справа pill-бейдж
  «FREE» (серый), ниже «Upgrade to unlock unlimited analyses» gray.
- Секция «Settings» (черта+тайтл), строки-карточки:
  bell «Push Notifications» + тумблер ON; chip «AI Coaching Tips» + ON;
  mail «Email Notifications» + ON; info «App Version 2.0.1» + chevron
  (→ GLM-конфиг, отклонение documented). Тумблеры accent.
- FAB. Значения Level/Experience/Goal/Progress берутся из профиля.

### 2.7. Edit Profile

- Шапка: chevron-back, «Edit Profile» белый bold по центру, «Save» accent справа.
- **Сегмент General | Additional**: контейнер-пилюля #141416, активный сегмент —
  accent с тёмным текстом.
- **Additional** (по скриншоту): поле «75 kg» (иконка scale, gray — placeholder),
  секции monospace-лейблов + чипы (радиус 24, неактив: #141416+граница, актив:
  accent + чёрный текст):
  - SKILL LEVEL: Beginner, Intermediate, Advanced, Professional
  - YEARS OF EXPERIENCE: < 1 year, 1-3 years, 3-5 years, 5-10 years, 10+ years
  - DOMINANT HAND: ✋ Right / ✋ Left (иконка pan_tool)
  - MAIN GOAL: 📈 Improve skills, 🏆 Compete, ❤️ Fitness, 😊 Fun, ⭐ Professional
- **General** (не показан в скриншотах — восстановлен по логике, помечено
  inferred): Name (текст), Height cm (число — нужно для cm-масштаба, GAP P1),
  Age (число).
- «Save» пишет в AppState (SharedPreferences) и пересчитывает прогресс.

### 2.8. My Analyses — таб 3

- Топ-бар: activity-иконка + «MY ANALYSES» monospace accent.
- Стат-карточка на всю ширину, 3 колонки: TOTAL 0 (white), Completed 0 (accent),
  Analyzing 0 (amber); метки gray, TOTAL — monospace.
- Сетка 2×2 карточек с ЦВЕТНОЙ границей (45%): внутренняя плитка (радиус 14,
  заливка цвет 12%, граница цвет 40%): иконка activity цветная + «no_data»
  monospace цветным (i18n-ключ, дословно); ниже: название белое 18sp
  (Serve/Forehand/Backhand/Drills), «0 sessions» gray; два pill-бейджа:
  «✓ 0» (accent-граница) и «🕐 0» (amber-граница).
- FAB.

### 2.9. My Videos — таб 2

Аналогично 2.8: топ-бар videocam + «MY VIDEOS»; в плитке иконка videocam +
число 0 + «VIDEOS» monospace цветом; подпись «No videos yet»; бейджей нет.

---

## 3. Карта навигации

```
BottomNav: home | videos | analyses | dashboard | profile
home ─→ live (REAL-TIME, Coach Mode) ─→ (back)
home ─→ analyze?stroke=… (Serve/Forehand/Backhand/AutoDetect) ─→ results ─→ home
home ─→ axel (FAB, AI Drills)
axel ─→ analyze (Record/Upload, tool analyze_video/start_live)
dashboard ─→ select ─→ player | coach (BACK)
profile ─→ edit (Save → profile)
analyze/live/results — fullscreen, без bottom nav
```

## 4. Данные (AppState, SharedPreferences)

- Профиль: name, heightCm, age, weightKg=75.0, skill=Beginner, experience=<1year
  («New player»), hand=Right, goal=Improve, progress (default 78 — как в
  скриншоте; после ручного Save пересчитывается как заполненность полей).
- Настройки: pushOn=true, aiTipsOn=true, emailOn=true; voiceMode=Manual
  (Axel-чип), ttsOn=true.
- Счётчики: sessions (инкремент после анализа), analysesCompleted/Analyzing
  (0), plans=0, messages=0, students=0, ambassadorEur=0, план=Free.

## 5. Строки и языки (обновлено 2026-10-17)

i18n-слой `core/i18n/T.kt`: **русский по умолчанию**, EN — дословно из
оригинального `reference/extracted_assets/en.json` (реверс APK v2.0.19).
Выбор языка: Settings → «Язык/Language» (RU/EN), плюс инструмент Axel
`set_app_language` (en, ru) — как в реверсе; выбор сохраняется в SharedPreferences.

`track_progression`, `record_first_video`, `no_data`, `manage_students_perf`
отсутствуют в en.json оригинала — потому в оригинале отображались как ключи.
Фолбэк `T.t()` возвращает неизвестный ключ как текст → дефект оригинала
воспроизводится 1:1 в обоих языках. Обрезанные на скриншотах тексты подсказок
Axel дополнены (помечено в 2.2). Экраны анализа (Analyze/Live/Results, фазы
M0–M6) имели RU/EN с самого начала и следуют тому же `state.lang`.

В оригинале было 10 языков (en, fr, es, it, de, zh, hi, pt, tr, ar); в клоне
переведены RU+EN, остальные добавляются записью в таблицы T.kt.

## 6. Отклонения от оригинала (осознанные)

1. Hologram Axel — Canvas-аппроксимация вместо видеорендера (ассет оригинала
   недоступен из APK-строк; вынести в ресурс при наличии исходника).
2. GLM-конфиг открывается из «App Version» на Profile (оригинал — серверный
   Firebase; клону нужен ввод ключа).
3. General-таб Edit Profile восстановлен по логике (Name/Height/Age), в
   скриншотах не показан.
4. TTS/speaker и красный stop: stop отменяет текущий запрос GLM; speaker —
   флаг (реального TTS в клоне нет).
5. Обрезанные тексты подсказок дополнены правдоподобно.

---

## 7. Сборка и запуск (проверено 2026-10-17)

```bash
cd tennis-ai-android
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rebuild.tennisai/.MainActivity
```

**Важно про ADB на этой машине:** порт 5037 попадает в зарезервированный
Hyper-V/WinNAT диапазон 5027–5126 («cannot bind … forbidden 10013»), поэтому
ADB-сервер поднят на порту **5237**: `adb -P 5237 …`; эмулятор запускать с
`ANDROID_ADB_SERVER_PORT=5237`. Постоянный фикс (от администратора):
`net stop winnat && adb start-server && net start winnat`.

Скриншоты сверки клона с оригиналом: `tennis-ai-android/shots/01…09*.png`
(Home, Videos, Analyses, Select, Player, Coach, Profile, Edit, Axel).
