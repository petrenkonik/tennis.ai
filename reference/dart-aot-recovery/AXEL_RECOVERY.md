# Axel — восстановление из Dart AOT (libapp.so, com.tennis.ai v2.0.19)

Метод: REA-экстракция строк Dart AOT-снапшота с сохранением файловых смещений
(`strings_ascii_ordered.txt`, `strings_utf16_ordered.txt`, `prompt_candidates.txt`,
`strong_prompts.txt` — всё в этом каталоге). Точность: тексты инструментов и правил
восстановлены дословно; системный промпт в оригинале собирается интерполяцией
фрагментов — здесь зафиксированы фрагменты с ролями. Смещения позволяют
сверить каждый фрагмент с снапшотом.

## 1. Инструменты (tool executor: `axel_tool_executor.dart`)

Каждое описание ниже — дословный текст из снапшота (сокращено «…» только там,
где текст служебно склеен с соседним). Исполнение: `SERVER` — наш API, `CLIENT` —
мини-апп (действие в UI), `HYBRID` — возврат данных клиенту для действия.

| Инструмент | Сторона | Описание из снапшота (offset) |
|---|---|---|
| `analyze_video` | CLIENT | «camera: film the stroke now (default). gallery: the player already HAS a clip… Choosing gallery opens the photo library directly; do not tell them to press a button, you are opening it.» (1654305) + «The stroke the player named, if they named one…» (466881) + недостающие данные онбординга: «In their language, explain in ONE honest sentence why the analysis needs it, then ask ONE question…» (1390116) |
| `navigate` | CLIENT | «Open an app screen. ONLY these exact destination values exist -- generated from the app's real route list (axel_capabilities.dart)… Do NOT use this for 'serve_setup', 'serve_analysis', 'forehand_analysis', or 'backhand_analysis'… Use analyze_video instead.» (1567483) |
| `open_drill_detail` | CLIENT | «Open the visual detail screen for ONE specific drill (video, sets, reps, tips). Use this whenever the player asks to SEE a drill screen… Different from start_drill_walkthrough, which reads it aloud step by step -- use both together…» (1042685) |
| `start_drill_walkthrough` | HYBRID | «Start a step-by-step voice walkthrough of a drill: Objective, Setup, How to do it, Key points, Progression, What to look for… Say ONLY the first step's content… call advance_drill_walkthrough» (799179) |
| `advance_drill_walkthrough` | HYBRID | «Move to the next step of the drill walkthrough… Returns the next step's content, or tells you the walkthrough is finished. Say ONLY that one step» (1105713) |
| `fetch_analysis_history` | SERVER | «Fetch the player's analysis session history (total sessions, average score, breakdown by stroke, and details of the most recent analysis including score, red metrics, and date)…» (1342156) |
| `fetch_recommended_drills` | SERVER | «Fetch recommended drills or the active training plan. Optionally pass an analysisId to get drills targeted at the weaknesses found in a specific analysis.» (343367) + fault: «The fault, exactly as it appears in the analysis (e.g. 'ARMS POSITION'…)» (900617) |
| `get_training_plan` | SERVER | «Fetch the player's active training plan. If no active plan exists, generate a new one.» (1070300) |
| `get_player_profile` | SERVER | «Fetch the current player profile (name, age, height, weight, handedness, level, goal, whether they have a coach). Use this to personalize advice or check what info you already have.» (1007112) |
| `save_player_profile` | SERVER | сохранение фактов профиля (уровень, рука, рост, цель…) — см. онбординг |
| `complete_onboarding` | SERVER | «complete_onboarding first if the account does not exist» (1390116, 1202639) |
| `set_session_intent` | SERVER | «Record what the player wants to do THIS session… 'I just want to do a drill today', 'I'm alone with my tripod', 'give me a full analysis'…» (1381516); значения: «full_analysis / drill_only / solo_tripod / quick_question» (1527718) + strokeType |
| `record_correction` | SERVER | память коуча: какой совет давался по метрике и сработал ли (`axel_coaching_memory.dart`, «First time you are coaching this metric. No history yet.») |
| `set_app_language` | CLIENT | «Change the language of the whole app… 'parle-moi en francais'… Only these are available: en, fr, es, it, de, zh, hi, pt, tr, ar… never call this with a code that is not on the list.» (1399922) |
| `restart_voice_input` | CLIENT | «Restart voice input when the player says it is not working… If they say it still fails after this, do NOT call it again… permission issue in iOS settings.» (1446572) |
| `set_voice_enabled` | CLIENT | «Turn your own voice on or off… 'stop talking', 'be quiet', 'I'll read instead'… you keep replying in writing.» (362742) |
| `set_speech_rate` | CLIENT | «Change how fast you speak… 'talk faster', 'you're going too fast', 'slow down a bit'. Apply it immediately… Do NOT offer this on your own» (1588324) |
| `send_message_to_coach` | SERVER | «Send a text message to the player's coach (if they have one).» (1615557) |
| `schedule_drill_reminder` | SERVER | «Schedule a daily local notification reminding the player to do a drill… Defaults to 6pm local time» (1138394) |
| `propose_drill` | SERVER | «Proposal saved for expert review. You may share the idea with the player now, saying plainly it is an extra idea from you and not yet part of the validated library. Do not promise it will be adopted.» (914615) |
| `show_last_analysis` | CLIENT | «[AxelExecutor] show_last_analysis: chargement analyse» (лог-строка) |

Перехват ошибок вызова: «[SYSTEM: you just WROTE a tool call as text instead of
emitting it. Nothing ran, and nothing opened. Emit the real function call now.
Do not describe it, do not tell the player it is done.]» (416088).

## 2. Контексты экранов (состояние → инструкция Axel)

- Новый игрок, голограмм-приветствие + карточки (441905); «Meet Axel AI» + 2 кнопки record/upload (1066623)
- Review-экран: клип загружен, можно тримить ручками (352817, 1416302, 316636 — «Abraham, 04/08»)
- Запись: «Filming now. The player needs the clip to show one stroke, filmed from the side, whole body in frame, around five seconds.» (1393290); позиционирование: «Position the camera at the sideline and baseline intersection…» (374044); повторная инструкция: «You already gave the positioning/filming instructions earlier…» (1403938)
- Анализ идёт: «This takes one to two minutes…» (1004962); результат только что пришёл (415027); результаты открыты, но не объяснены (800428)
- Trophy-экран: «this is a moment to acknowledge, not analyse» (790945)
- Дашборды игрока/тренера (930204, 1578564); coach-only экран для игрока: «…is a coach-only screen and this account is a player. Do not offer or attempt this.» (1516105)
- «You cannot send anyone to the analysis screen: it needs a video already submitted. Use analyze_video to start one.» (907369)

## 3. Поведенческие правила

- Красный/зелёный объясняется один раз: «red is outside the range the best players sit in, green is inside it. This is the only time you will say it.» (1531988)
- Балл — «a score out of 100, never a percentage» (1593656)
- Нет замечаний: «Nothing flagged… one more analysis would confirm the level holds. End on a real question and stop.» (1593656)
- Метрики не пришли: «Do NOT invent a fault or a correction» (1338698)
- Спина к камере: «Left and right are swapped that way, so every angle would be measured on the wrong side.» (1501875); ландшафт: «rotating the file afterwards does not fix it» (421912); портрет обязателен: «a square crop distorts the scale used to convert pixels to centimetres» (1664626)
- Режим тренера: «You are talking to a COACH, not the player… Do not address them as if they hit the strokes.» (440618)
- Прогресс-вопросы детектор (regex): `(your|the) (score|overall score) | \d+ (/|out of) 100 | … from (red|green) | last (time|session|analysis)` (1371035)
- Диалог «записать или загрузить»: один вопрос, ничего не открывать самому (999369, 1640273)
- Пропозиция drills после 1-го анализа: «based on ONE analysis… a couple more…» (1330913); после 2-го: «the picture is getting clearer…» (1146979) + «an invitation to sharpen the picture, never a condition» (1184623)

## 4. Онбординг и профиль

- Первое сообщение: «Welcome to Axel AI! I'm Axel, your AI coach. I'll ask you a few quick questions to personalize your experience. First, what's your first name?» (328259) + FR-версия (1075974)
- Профиль: имя, возраст, рост, вес, рука, уровень, цель, наличие тренера; стиль игры «baseliner, all-court, serve-and-volley…» (1201309); сильный удар (1282469); матч/рекреация (776931); что даёт проблемы в матче (926945); самооценка слабости (1088046)

## 5. Память и знания (Firestore → у нас Mongo)

- `metric_definitions` — пороги и тексты метрик (читаются в рантайме, «[AxelTools] metric_definitions read failed»)
- `amine_consignes` — инструкции/правила коуча
- `cohort_stats` — когортная статистика (перцентили)
- `axel_coaching_memory` — какой совет по какой метрике давался и сработал ли
- `knowledge` — RAG-база, домены: biomechanics, tactics, fitness, mental, drills, coaching («Filter by domain…»)

## 6. Ограничения восстановления

Системный промпт в снапшоте не хранится одной строкой — он собирается из
констант; выше приведены все значимые фрагменты. Часть инструкций может
приезжать из Firestore (`amine_consignes`) и отсутствовать в APK. Кириллические
строки лежат в `strings_utf16_ordered.txt` (2951 строка).
