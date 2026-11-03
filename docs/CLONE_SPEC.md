# CLONE SPEC — Axel AI (com.tennis.ai v2.0.19) → tennis-ai-android

Full UI/UX analysis of the original for exact cloning. Sources: 8 screenshots of
the original (2026-10-17) + 11 photos in `docs/OriginalScrean/` (3 of them unique —
Home in two scroll states and Axel in conversation mode) + reverse engineering of
`reference/ANALYSIS_FINDINGS.md` and `reference/dart-aot-recovery/`.

Target project: `tennis-ai-android/` (Kotlin + Compose, `com.rebuild.tennisai`).

---

## 1. Design system

### 1.1. Palette

| Token | Value | Where it appears |
|---|---|---|
| `bg` | `#050505` | background of all screens |
| `bar` | `#0A0A0A` + bottom/top hairline `#1F1F22` | top bars, bottom nav |
| `card` | `#141416`, border `#26262A`, radius 18–20 | all cards |
| `cardAccent` | dark-green fill `#141F0C` + border `#3F5A22` | Ambassador, referral banner, active elements |
| `accent` | `#9ACD32` (lime) | headings, icons, toggles, active chips, user bubble |
| `accentDeep` | `#7CB342` | toggles, button fills |
| `textPrimary` | `#F2F2F2` | primary texts |
| `textSecondary` | `#8E8E93` | captions, subtitles |
| `textDim` | `#5A5A5E` | secondary card captions |
| Serve blue | `#4DA3FF` | Serve Analysis module / Serve card |
| Forehand lime | `#9ACD32` | Forehand module |
| Backhand amber | `#F5A623` | Backhand module, Analyzing counter |
| Drills red | `#E5544B` | AI Drills / Drills module |
| Coach orange | `#F0793D` | Coach Mode module |
| Stop red | `#EB4B3B` | stop button in Axel |

Color roles of the Analyses/Videos cards = border color (45% alpha), icon color
and the "no_data" color are the same.

### 1.2. Typography — three registers

1. **Serif display with green glow** — large headings: «AXEL AI» (Home),
   «SELECT DASHBOARD», «My Profile». Caps or as in the original; glow shadow
   `Shadow(accent, blur≈18)`.
2. **Monospace with 2–4sp tracking** — "technical" headings: top bars
   («AXEL AI», «COACH DASHBOARD», «MY ANALYSES», «MY VIDEOS»), sections
   («TRAINING MODULES», «Settings» — no, Settings is normal), card labels
   («PLAYER DASHBOARD», «COACH DASHBOARD», «TRAINING PLANS», «MESSAGES»,
   «VIDEOS», «no_data», «BACK», «NEW», «COACH»), Edit Profile field labels
   («SKILL LEVEL», «YEARS OF EXPERIENCE», «DOMINANT HAND», «MAIN GOAL»).
3. **Sans (Roboto)** — all the remaining text.

### 1.3. Common elements

- **Axel FAB** — 56dp circle, `#0C0F08` background, thin green border, green
  "sparkles" icon; bottom-right on all tab screens (absent on Axel). Opens Axel.
- **Bottom nav** — 5 tabs: Home (house), My Videos (videocam), My Analyses
  (activity/pulse), Dashboard (2×2 grid), Profile (person). Active one is green
  with a green 24×3dp bar at the TOP edge of the bar; inactive `#6E6E73`.
- **Top bar** — height ~56dp, icon+title centered, background `#0A0A0A`.
- **Section heading** — green vertical bar 4×18dp + white bold
  («Recent Activity», «Settings»).

---

## 2. Screen inventory (9 unique)

### 2.1. Home («AXEL AI») — tab 1

- Top bar: chevron-left (decorative), centered serif «AXEL AI» with glow.
- **REAL-TIME ANALYSIS** button — full width, 1dp accent border, radius 14,
  `broadcast`/`((•))` icon, monospace accent text. → Live screen.
- Section label: «TRAINING MODULES» monospace gray + line to the edge.
- **Module rows** (cards, radius 18): on the left a 52dp icon tile
  (colored fill at 14% + colored icon), 18sp semibold white title,
  14sp gray subtitle; on the right a progress ring (Canvas, 44dp, gray track,
  colored arc = %, «0%» in the center in color); at the bottom a thin progress
  bar and to the right beneath it a monospace label:
  | Module | Color | Icon | Subtitle | Label |
  |---|---|---|---|---|
  | Coach Mode | orange | clipboard | Real-time analysis for coaches | COACH |
  | Auto Detect | lime | scan/frame | Auto-detect your stroke and analyze it | NEW |
  | Serve Analysis | blue | videocam | Full Motion & Posture | 0 sessions |
  | Forehand | lime | play | Rotation & Extension | 0 sessions |
  | Backhand | amber | play | 1 or 2 Handed Technique | 0 sessions |
  | AI Drills | red | dumbbell | Personalized Exercises | 0 sessions |
- Navigation: Coach Mode/REAL-TIME → Live; Serve/Forehand/Backhand → analysis with
  a preset stroke; Auto Detect → analysis (manual selection); AI Drills → Axel.

### 2.2. Axel (chat coach, fullscreen)

- Header: green "sparkles" icon + «Axel» 22sp bold + **mode chip**
  (pill, olive border, `#C6E36A` text, Auto/Manual values — tap switches);
  on the right: speaker icon (TTS on/off), **red stop button**
  (rounded square), chevron-down (collapse = back).
- **Idle state**: hologram block (in the original — a render of a neon human,
  turquoise-green on black; in the clone — Canvas drawing: glow, head,
  shoulders, polo collar, particles), then «**Meet** **Axel AI**» (Meet — white,
  Axel AI — accent, 34sp bold), caption «Your intelligent tennis companion.»
  gray, a 2×2 grid of hint cards (radius 16): title white bold 17sp + body
  gray 14sp:
  1. «Tell Axel about your game» / «Tell me a bit about your tennis — how would you describe your game?» → sends the text into the chat
  2. «Beat the nerves» / «Before an important match»
  3. «How's a top pro playing?» / «Current form and recent results» (cropped in the screenshot — filled in)
  4. «Analyse my first stroke» / «Send a video for full analysis» (cropped — filled in)
- **Bottom input panel**: on the left two round buttons labeled «Record»
  (videocam → analysis/recording screen) and «Upload» (upload → gallery); center —
  pill input «Reply to Axel...» (#1B1B1D, radius 28); on the right «Speak» (mic) and
  «Send» — GREEN circle with a dark send icon.
- **Conversation** (photo 11-55-47): user — accent bubble on the right, dark
  text; Axel — `#1B1B1D` bubble on the left, white text. Question texts — from
  the hint cards.
- GLM configuration (key/baseURL/model) moved into the «App Version» row on
  Profile (not visible in the original; the clone needs access — documented
  deviation).

### 2.3. Select Dashboard — tab 4 (root)

Centered, no top bar: large green 2×2 grid icon (56dp), serif
«SELECT DASHBOARD» with glow, two cards (radius 20):
- 56dp icon tile (dark-green fill, green person/people icon),
  «PLAYER DASHBOARD» monospace accent + «Track your personal progress and
  sessions» gray, chevron right;
- «COACH DASHBOARD» + «manage_students_perf» (in the original — an UNTRANSLATED
  i18n key, cloned verbatim), chevron.
Below, centered «BACK» monospace gray.

### 2.4. My Dashboard (player) — after selecting Player

- Top bar: globe icon (accent) + «AXEL AI» monospace WHITE + QR icon (accent) on the right.
- «My Dashboard» accent 28sp bold centered, beneath it «track_progression» gray
  (untranslated i18n key in the original — verbatim).
- 2×2 grid of stat cards: green icon on top, 32sp white number, gray label:
  «My Sessions» (target, 0), «Skill Analysis» (bar-chart, «—»),
  «TRAINING PLANS» (monospace, doc, 0), «MESSAGES» (monospace, chat, 0).
- Referral banner (cardAccent): gift icon, «Get 1 year free» white bold,
  «Invite friends → unlock free months» gray, chevron.
- «Recent Activity» section (bar+title), empty-state card:
  videocam icon gray, «No sessions yet» white, «record_first_video» gray
  (i18n key, verbatim).
- FAB.

### 2.5. Coach Dashboard

Top bar: person icon + «COACH DASHBOARD» monospace white. Heading
«Welcome, Coach!» accent, «Student overview» gray. Stat cards: My Students
(0), Analysis («—»), TRAINING PLANS (0), MESSAGES («—»). Banner: «Add 10
students → unlock free months». Recent Activity: people icon, «No student
activity yet», «Add students to see their activity». FAB.

### 2.6. My Profile — tab 5

- Top bar: chevron-back, person icon + serif «My Profile» accent.
- **Ambassador Program** (cardAccent, radius 20): green 52dp tile with people,
  «Ambassador Program» white bold 20sp, «Share and get rewarded» gray, on the right
  «€0» accent 26sp.
- 2×2 grid of mini-cards (accent icon + white 15sp text):
  «Level: Beginner» (bar-chart), «New player» (clock), «Goal: Improve» (target),
  «Progress: 78%» (trending-up).
- **Free Plan**: card with 🎾 (emoji), «Free Plan» bold, on the right pill badge
  «FREE» (gray), below «Upgrade to unlock unlimited analyses» gray.
- «Settings» section (bar+title), row-cards:
  bell «Push Notifications» + toggle ON; chip «AI Coaching Tips» + ON;
  mail «Email Notifications» + ON; info «App Version 2.0.1» + chevron
  (→ GLM config, documented deviation). Toggles accent.
- FAB. Level/Experience/Goal/Progress values are taken from the profile.

### 2.7. Edit Profile

- Header: chevron-back, «Edit Profile» white bold centered, «Save» accent on the right.
- **General | Additional segments**: pill container #141416, active segment —
  accent with dark text.
- **Additional** (per the screenshot): «75 kg» field (scale icon, gray — placeholder),
  monospace label sections + chips (radius 24, inactive: #141416+border, active:
  accent + black text):
  - SKILL LEVEL: Beginner, Intermediate, Advanced, Professional
  - YEARS OF EXPERIENCE: < 1 year, 1-3 years, 3-5 years, 5-10 years, 10+ years
  - DOMINANT HAND: ✋ Right / ✋ Left (pan_tool icon)
  - MAIN GOAL: 📈 Improve skills, 🏆 Compete, ❤️ Fitness, 😊 Fun, ⭐ Professional
- **General** (not shown in the screenshots — reconstructed from logic, marked
  inferred): Name (text), Height cm (number — needed for the cm scale, GAP P1),
  Age (number).
- «Save» writes to AppState (SharedPreferences) and recalculates progress.

### 2.8. My Analyses — tab 3

- Top bar: activity icon + «MY ANALYSES» monospace accent.
- Full-width stat card, 3 columns: TOTAL 0 (white), Completed 0 (accent),
  Analyzing 0 (amber); labels gray, TOTAL — monospace.
- 2×2 grid of cards with a COLORED border (45%): inner tile (radius 14,
  fill color 12%, border color 40%): colored activity icon + «no_data»
  monospace in color (i18n key, verbatim); below: white 18sp name
  (Serve/Forehand/Backhand/Drills), «0 sessions» gray; two pill badges:
  «✓ 0» (accent border) and «🕐 0» (amber border).
- FAB.

### 2.9. My Videos — tab 2

Same as 2.8: top bar videocam + «MY VIDEOS»; in the tile a videocam icon +
number 0 + «VIDEOS» monospace in color; caption «No videos yet»; no badges.

---

## 3. Navigation map

```
BottomNav: home | videos | analyses | dashboard | profile
home ─→ live (REAL-TIME, Coach Mode) ─→ (back)
home ─→ analyze?stroke=… (Serve/Forehand/Backhand/AutoDetect) ─→ results ─→ home
home ─→ axel (FAB, AI Drills)
axel ─→ analyze (Record/Upload, tool analyze_video/start_live)
dashboard ─→ select ─→ player | coach (BACK)
profile ─→ edit (Save → profile)
analyze/live/results — fullscreen, no bottom nav
```

## 4. Data (AppState, SharedPreferences)

- Profile: name, heightCm, age, weightKg=75.0, skill=Beginner, experience=<1year
  («New player»), hand=Right, goal=Improve, progress (default 78 — as in the
  screenshot; after a manual Save it is recalculated as field completeness).
- Settings: pushOn=true, aiTipsOn=true, emailOn=true; voiceMode=Manual
  (Axel chip), ttsOn=true.
- Counters: sessions (incremented after analysis), analysesCompleted/Analyzing
  (0), plans=0, messages=0, students=0, ambassadorEur=0, plan=Free.

## 5. Strings and languages (updated 2026-10-17)

The i18n layer `core/i18n/T.kt`: **Russian by default**, EN — verbatim from the
original's `reference/extracted_assets/en.json` (APK v2.0.19 reverse).
Language selection: Settings → «Language» (RU/EN), plus the Axel tool
`set_app_language` (en, ru) — as in the reverse; the selection is stored in SharedPreferences.

`track_progression`, `record_first_video`, `no_data`, `manage_students_perf`
are missing from the original's en.json — hence they displayed as keys in the original.
The `T.t()` fallback returns an unknown key as text → the original's defect
is reproduced 1:1 in both languages. The Axel hint texts cropped in the
screenshots were filled in (marked in 2.2). The analysis screens (Analyze/Live/Results,
phases M0–M6) had RU/EN from the start and follow the same `state.lang`.

The original had 10 languages (en, fr, es, it, de, zh, hi, pt, tr, ar); the clone
has RU+EN translated, the rest can be added by writing into the T.kt tables.

## 6. Deviations from the original (deliberate)

1. Axel hologram — Canvas approximation instead of a video render (the original's
   asset is unavailable from APK strings; extract to a resource when the source is available).
2. GLM config is opened from «App Version» on Profile (the original — server-side
   Firebase; the clone needs key input).
3. The Edit Profile General tab was reconstructed from logic (Name/Height/Age), it is
   not shown in the screenshots.
4. TTS/speaker and the red stop: stop cancels the current GLM request; speaker —
   a flag (no real TTS in the clone).
5. Cropped hint texts were filled in plausibly.

---

## 7. Build and run (verified 2026-10-17)

```bash
cd tennis-ai-android
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rebuild.tennisai/.MainActivity
```

**Important about ADB on this machine:** port 5037 falls into the reserved
Hyper-V/WinNAT range 5027–5126 («cannot bind … forbidden 10013»), so the
ADB server is on port **5237**: `adb -P 5237 …`; start the emulator with
`ANDROID_ADB_SERVER_PORT=5237`. Permanent fix (as administrator):
`net stop winnat && adb start-server && net start winnat`.

Clone-vs-original comparison screenshots: `tennis-ai-android/shots/01…09*.png`
(Home, Videos, Analyses, Select, Player, Coach, Profile, Edit, Axel).
