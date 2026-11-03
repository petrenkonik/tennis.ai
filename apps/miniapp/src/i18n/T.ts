/**
 * Строки интерфейса — порт `tennis-ai-android/core/i18n/T.kt`.
 * EN — дословно из оригинального `reference/extracted_assets/en.json` (реверс APK v2.0.19);
 * RU — перевод, язык по умолчанию.
 * НЕизвестный ключ возвращается как есть — так в оригинале выглядели невыровненные
 * i18n-ключи («track_progression», «no_data», «record_first_video», «manage_students_perf»).
 * Ключи с префиксом `live_`/`analyze_`/`results_`/`drills_`/`paywall_` — экраны анализа TMA
 * (в оригинале имели RU/EN, статусы пайплайна — наши, CLONE_SPEC §5).
 */

export type Lang = 'ru' | 'en';

export const DEFAULT_LANG: Lang = 'ru';

export function t(lang: Lang, key: string): string {
  return lang === 'ru' ? (RU[key] ?? EN[key] ?? key) : (EN[key] ?? key);
}

/** «N sessions» / «N сессий» — сборка метки модуля, как в оригинале. */
export function sessions(n: number, lang: Lang): string {
  return `${n} ${t(lang, 'sessions')}`;
}

export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'PROFESSIONAL';
export type Experience = 'UNDER_1' | 'Y1_3' | 'Y3_5' | 'Y5_10' | 'Y10_PLUS';
export type MainGoal = 'IMPROVE' | 'COMPETE' | 'FITNESS' | 'FUN' | 'PROFESSIONAL';

export const SKILL_LEVELS: SkillLevel[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'PROFESSIONAL'];
export const EXPERIENCES: Experience[] = ['UNDER_1', 'Y1_3', 'Y3_5', 'Y5_10', 'Y10_PLUS'];
export const MAIN_GOALS: MainGoal[] = ['IMPROVE', 'COMPETE', 'FITNESS', 'FUN', 'PROFESSIONAL'];

const SKILL_KEYS: Record<SkillLevel, string> = {
  BEGINNER: 'level_beginner',
  INTERMEDIATE: 'level_intermediate',
  ADVANCED: 'level_advanced',
  PROFESSIONAL: 'level_professional',
};

const EXP_KEYS: Record<Experience, string> = {
  UNDER_1: 'exp_under_1',
  Y1_3: 'exp_1_3',
  Y3_5: 'exp_3_5',
  Y5_10: 'exp_5_10',
  Y10_PLUS: 'exp_10_plus',
};

const GOAL_KEYS: Record<MainGoal, string> = {
  IMPROVE: 'goal_improve_skills',
  COMPETE: 'goal_compete',
  FITNESS: 'goal_fitness',
  FUN: 'goal_fun',
  PROFESSIONAL: 'goal_professional',
};

export function skillLabel(s: SkillLevel, lang: Lang): string {
  return t(lang, SKILL_KEYS[s]);
}

export function expLabel(e: Experience, lang: Lang): string {
  return t(lang, EXP_KEYS[e]);
}

export function expShort(e: Experience, lang: Lang): string {
  return e === 'UNDER_1' ? t(lang, 'new_player') : expLabel(e, lang);
}

export function goalChip(g: MainGoal, lang: Lang): string {
  return t(lang, GOAL_KEYS[g]);
}

export function handLabel(right: boolean, lang: Lang): string {
  return t(lang, right ? 'hand_right' : 'hand_left');
}

const EN: Record<string, string> = {
  // Home
  realtime_analysis: 'REAL-TIME ANALYSIS',
  training_modules: 'TRAINING MODULES',
  coach_mode: 'Coach Mode',
  realtime_for_coaches: 'Real-time analysis for coaches',
  auto_detect: 'Auto Detect',
  auto_detect_desc: 'Auto-detect your stroke and analyze it',
  serve_analysis: 'Serve Analysis',
  full_motion_posture: 'Full Motion & Posture',
  forehand: 'Forehand',
  rotation_extension: 'Rotation & Extension',
  backhand: 'Backhand',
  handed_technique: '1 or 2 Handed Technique',
  ai_drills: 'AI Drills',
  personalized_exercises: 'Personalized Exercises',
  coach: 'COACH',
  new: 'NEW',
  sessions: 'sessions',
  // Select dashboard
  select_dashboard: 'SELECT DASHBOARD',
  player_dashboard: 'PLAYER DASHBOARD',
  track_personal_progress: 'Track your personal progress and sessions',
  coach_dashboard: 'COACH DASHBOARD',
  back: 'BACK',
  // Player dashboard
  my_dashboard: 'My Dashboard',
  my_sessions: 'My Sessions',
  skill_analysis: 'Skill Analysis',
  training_plans: 'TRAINING PLANS',
  messages: 'MESSAGES',
  get_1_year_free: 'Get 1 year free',
  invite_10_friends: 'Invite friends → unlock free months',
  recent_activity: 'Recent Activity',
  no_sessions_yet: 'No sessions yet',
  // Coach dashboard
  welcome_coach: 'Welcome, Coach!',
  student_overview: 'Student overview',
  my_students: 'My Students',
  analysis: 'Analysis',
  add_students_free: 'Add 10 students → unlock free months',
  no_student_activity: 'No student activity yet',
  add_students_to_see: 'Add students to see their activity',
  // Profile
  my_profile: 'My Profile',
  ambassador_program: 'Ambassador Program',
  ambassador_subtitle: 'Share and get rewarded',
  level: 'Level',
  new_player: 'New player',
  goal: 'Goal',
  progress: 'Progress',
  free_plan: 'Free Plan',
  free: 'FREE',
  free_desc: 'Upgrade to unlock unlimited analyses',
  settings: 'Settings',
  push_notifications: 'Push Notifications',
  ai_coaching_tips: 'AI Coaching Tips',
  email_notifications: 'Email Notifications',
  app_version: 'App Version 2.0.1',
  language: 'Language',
  // Edit profile
  edit_profile: 'Edit Profile',
  save: 'Save',
  general: 'General',
  additional: 'Additional',
  name: 'Name',
  height_cm: 'Height (cm)',
  age: 'Age',
  weight_placeholder: '75 kg',
  skill_level: 'SKILL LEVEL',
  years_experience: 'YEARS OF EXPERIENCE',
  dominant_hand: 'DOMINANT HAND',
  main_goal: 'MAIN GOAL',
  level_beginner: 'Beginner',
  level_intermediate: 'Intermediate',
  level_advanced: 'Advanced',
  level_professional: 'Professional',
  exp_under_1: '< 1 year',
  exp_1_3: '1-3 years',
  exp_3_5: '3-5 years',
  exp_5_10: '5-10 years',
  exp_10_plus: '10+ years',
  hand_right: 'Right',
  hand_left: 'Left',
  goal_improve_skills: 'Improve skills',
  goal_compete: 'Compete',
  goal_fitness: 'Fitness',
  goal_fun: 'Fun',
  goal_professional: 'Professional',
  // Analyses / videos
  my_analyses: 'MY ANALYSES',
  my_videos: 'MY VIDEOS',
  total: 'TOTAL',
  completed: 'Completed',
  analyzing: 'Analyzing',
  serve: 'Serve',
  drills: 'Drills',
  videos_upper: 'VIDEOS',
  no_videos_yet: 'No videos yet',
  // My Videos actions (en.json дословно)
  avg_score_upper: 'AVG SCORE',
  video_saved_to: 'Video saved to',
  download_failed: 'Download failed',
  delete_video: 'Delete Video',
  delete_video_confirm: 'This will permanently delete the video and its analysis.',
  video_deleted: 'Video deleted',
  delete_failed: 'Delete failed',
  delete_session: 'Delete Session',
  delete_session_confirm: 'This will permanently delete this session and its analysis.',
  re_analyze: 'RE-ANALYZE',
  no_url: 'No URL',
  share: 'Share',
  download: 'Download',
  cancel_upper: 'CANCEL',
  delete_upper: 'DELETE',
  preparing: 'Preparing…',
  // Axel
  meet: 'Meet',
  companion: 'Your intelligent tennis companion.',
  s1_title: 'Tell Axel about your game',
  s1_body: 'Tell me a bit about your tennis — how would you describe your game?',
  s2_title: 'Beat the nerves',
  s2_body: 'Before an important match',
  s3_title: "How's a top pro playing?",
  s3_body: 'Current form and recent results',
  s4_title: 'Analyse my first stroke',
  s4_body: 'Send a video for full analysis',
  reply_hint: 'Reply to Axel...',
  record: 'Record',
  upload: 'Upload',
  speak: 'Speak',
  send: 'Send',
  manual: 'Manual',
  auto: 'Auto',
  thinking: 'Axel is thinking…',
  toast_voice_in: 'Voice input is not available in this build',
  toast_voice_out: 'Voice playback is not available in this build',
  // Экраны анализа TMA (RU/EN были в оригинале; статусы пайплайна — наши)
  live_title: 'LIVE CORRECT',
  live_desc: 'Real-time stroke correction with instant voice feedback.',
  live_unavailable: 'Real-time mode needs camera access inside Telegram — coming soon in the Mini App. Use Auto Detect for now.',
  // Live-флоу (en.json/libapp дословно)
  rec: 'REC',
  hd_30fps: 'HD @ 30FPS',
  ai_tracking_active: 'AI TRACKING: ACTIVE',
  subject_in_frame: 'SUBJECT IN FRAME',
  tap_to_stop: 'TAP TO STOP',
  tap_rec_when_ready: 'Tap REC when ready',
  get_in_position: 'Get in position. 15 seconds.',
  go: 'Go!',
  live_camera_feed: 'LIVE CAMERA FEED',
  live_correction: 'LIVE CORRECTION',
  start_live_analysis: 'START LIVE ANALYSIS',
  camera_denied: 'Camera permission required',
  stop: 'Stop',
  try_again: 'Try again',
  all_metrics_corrected: 'All metrics corrected! Excellent form!',
  errors_not_fixable: 'Errors found but none fixable live. Try drills.',
  analyze_title: 'ANALYZE',
  stroke_type: 'STROKE TYPE',
  auto_detect_frame: 'Auto-detect from a frame',
  detecting: 'Detecting stroke type…',
  detected: 'Detected',
  video_label: 'Video (portrait, ≤ 5 s, one stroke, full body)',
  pick_video: 'Pick a video',
  analyze_run: 'Analyze video',
  working: 'Working…',
  stage_pose: 'Detecting pose…',
  stage_engine: 'Computing metrics and phases…',
  stage_upload: 'Uploading video…',
  failed: 'Failed',
  // VIDEO PREVIEW (экран ревью после записи; EN — дословно из en.json/libapp оригинала)
  video_preview: 'VIDEO PREVIEW',
  checking_video: 'CHECKING VIDEO...',
  video_too_low_quality: 'VIDEO TOO LOW QUALITY',
  retake_video: 'RETAKE VIDEO',
  analyze_upper: 'ANALYZE',
  trim_your_stroke: 'TRIM YOUR STROKE',
  good_angle_lighting_tip: 'Good angle! Make sure you have good lighting.',
  landscape_clip:
    'The clip is in landscape: the PHONE was held horizontally while filming. The analysis cannot run on it, and rotating the file afterwards does not fix it.',
  square_crop:
    'The clip is a square crop. It distorts the pixel-to-cm calibration, so the analysis cannot run on it.',
  clip_too_long: 'The clip is longer than 5 seconds. Trim it to the stroke itself before analyzing.',
  clip_duration_unknown: 'Clip duration is unknown — trim is disabled.',
  review_guide:
    'Welcome to the review screen. You can analyse your swing, trim it down to the stroke itself, or take another video. The trim handles are draggable.',
  results_title: 'ANALYSIS',
  analysis_complete: 'ANALYSIS COMPLETE',
  red_explainer: 'Red means outside the range the best players sit in, green means inside it.',
  stroke_phases: 'STROKE PHASES',
  no_metrics_phase: 'No metrics available for this phase.',
  weaknesses: 'WEAKNESSES → DRILLS',
  recommended_drills: 'Recommended drills',
  no_analysis_result: 'No analysis result',
  drills_title: 'AI DRILLS',
  only_weaknesses: 'Only for my weaknesses',
  targets: 'targets',
  loading: 'Loading…',
  empty_list: 'Nothing here yet',
  delete: 'Delete',
  open: 'Open',
  report: 'Report',
  // Paywall
  paywall_title: 'AXEL AI PRO',
  paywall_desc:
    'Free: 9 video analyses lifetime. Pro: 20 video analyses / month + unlimited real-time correction sessions. Payment with Telegram Stars; renewal is manual — the bot will remind you before the period ends.',
  pay: 'Pay',
  // Boot
  starting: 'Starting…',
  retry_login: 'Retry login',
};

const RU: Record<string, string> = {
  // Home
  realtime_analysis: 'АНАЛИЗ В РЕАЛЬНОМ ВРЕМЕНИ',
  training_modules: 'ТРЕНИРОВОЧНЫЕ МОДУЛИ',
  coach_mode: 'Режим тренера',
  realtime_for_coaches: 'Анализ в реальном времени для тренеров',
  auto_detect: 'Авто-определение',
  auto_detect_desc: 'Определи удар автоматически и получи разбор',
  serve_analysis: 'Анализ подачи',
  full_motion_posture: 'Полное движение и поза',
  forehand: 'Форхенд',
  rotation_extension: 'Вращение и вытяжение',
  backhand: 'Бэкхенд',
  handed_technique: 'Техника в одну или две руки',
  ai_drills: 'AI-упражнения',
  personalized_exercises: 'Персональные упражнения',
  coach: 'ТРЕНЕР',
  new: 'НОВОЕ',
  sessions: 'сессий',
  // Select dashboard
  select_dashboard: 'ВЫБОР ДАШБОРДА',
  player_dashboard: 'ДАШБОРД ИГРОКА',
  track_personal_progress: 'Твой личный прогресс и сессии',
  coach_dashboard: 'ДАШБОРД ТРЕНЕРА',
  back: 'НАЗАД',
  // Player dashboard
  my_dashboard: 'Мой дашборд',
  my_sessions: 'Мои сессии',
  skill_analysis: 'Анализ техники',
  training_plans: 'ПЛАНЫ ТРЕНИРОВОК',
  messages: 'СООБЩЕНИЯ',
  get_1_year_free: 'Получи год бесплатно',
  invite_10_friends: 'Пригласи друзей → открой бесплатные месяцы',
  recent_activity: 'Недавняя активность',
  no_sessions_yet: 'Пока нет сессий',
  // Coach dashboard
  welcome_coach: 'Добро пожаловать, Тренер!',
  student_overview: 'Обзор учеников',
  my_students: 'Мои ученики',
  analysis: 'Анализы',
  add_students_free: 'Добавь 10 учеников → открой бесплатные месяцы',
  no_student_activity: 'У учеников пока нет активности',
  add_students_to_see: 'Добавь учеников, чтобы видеть их активность',
  // Profile
  my_profile: 'Мой профиль',
  ambassador_program: 'Амбассадорская программа',
  ambassador_subtitle: 'Делись и получай награды',
  level: 'Уровень',
  new_player: 'Новый игрок',
  goal: 'Цель',
  progress: 'Прогресс',
  free_plan: 'Бесплатный план',
  free: 'БЕСПЛАТНО',
  free_desc: 'Обнови план для безлимитных анализов',
  settings: 'Настройки',
  push_notifications: 'Push-уведомления',
  ai_coaching_tips: 'AI-подсказки коуча',
  email_notifications: 'Email-уведомления',
  app_version: 'Версия приложения 2.0.1',
  language: 'Язык',
  // Edit profile
  edit_profile: 'Редактирование профиля',
  save: 'Сохранить',
  general: 'Основное',
  additional: 'Дополнительно',
  name: 'Имя',
  height_cm: 'Рост (см)',
  age: 'Возраст',
  weight_placeholder: '75 кг',
  skill_level: 'УРОВЕНЬ ИГРОКА',
  years_experience: 'ОПЫТ ИГРЫ',
  dominant_hand: 'ВЕДУЩАЯ РУКА',
  main_goal: 'ГЛАВНАЯ ЦЕЛЬ',
  level_beginner: 'Новичок',
  level_intermediate: 'Средний',
  level_advanced: 'Продвинутый',
  level_professional: 'Профи',
  exp_under_1: '< 1 года',
  exp_1_3: '1-3 года',
  exp_3_5: '3-5 лет',
  exp_5_10: '5-10 лет',
  exp_10_plus: '10+ лет',
  hand_right: 'Правша',
  hand_left: 'Левша',
  goal_improve_skills: 'Улучшение навыков',
  goal_compete: 'Соревнования',
  goal_fitness: 'Фитнес',
  goal_fun: 'Удовольствие',
  goal_professional: 'Профи',
  // Analyses / videos
  my_analyses: 'МОИ АНАЛИЗЫ',
  my_videos: 'МОИ ВИДЕО',
  total: 'ВСЕГО',
  completed: 'Завершено',
  analyzing: 'Анализируется',
  serve: 'Подача',
  drills: 'Упражнения',
  videos_upper: 'ВИДЕО',
  no_videos_yet: 'Пока нет видео',
  // My Videos actions
  avg_score_upper: 'СРЕДНИЙ БАЛЛ',
  video_saved_to: 'Видео сохранено:',
  download_failed: 'Не удалось скачать видео',
  delete_video: 'Удалить видео',
  delete_video_confirm: 'Видео и его анализ будут удалены безвозвратно.',
  video_deleted: 'Видео удалено',
  delete_failed: 'Не удалось удалить',
  delete_session: 'Удалить сессию',
  delete_session_confirm: 'Сессия и её анализ будут удалены безвозвратно.',
  re_analyze: 'ПЕРЕРАЗОБРАТЬ',
  no_url: 'Нет ссылки',
  share: 'Поделиться',
  download: 'Скачать',
  cancel_upper: 'ОТМЕНА',
  delete_upper: 'УДАЛИТЬ',
  preparing: 'Готовим…',
  // Axel
  meet: 'Знакомьтесь,',
  companion: 'Твой умный теннисный компаньон.',
  s1_title: 'Расскажи Axel о своей игре',
  s1_body: 'Расскажи немного о своём теннисе — как бы ты описал свою игру?',
  s2_title: 'Победить волнение',
  s2_body: 'Перед важным матчем',
  s3_title: 'Как играет топ-профи?',
  s3_body: 'Текущая форма и свежие результаты',
  s4_title: 'Разбери мой первый удар',
  s4_body: 'Пришли видео для полного анализа',
  reply_hint: 'Ответь Axel...',
  record: 'Запись',
  upload: 'Загрузить',
  speak: 'Голос',
  send: 'Отправить',
  manual: 'Ручной',
  auto: 'Авто',
  thinking: 'Axel думает…',
  toast_voice_in: 'Голосовой ввод недоступен в этой сборке',
  toast_voice_out: 'Голосовое воспроизведение недоступно в этой сборке',
  // Экраны анализа TMA
  live_title: 'КОРРЕКЦИЯ В РЕАЛЬНОМ ВРЕМЕНИ',
  live_desc: 'Коррекция удара в реальном времени с голосовыми подсказками.',
  live_unavailable: 'Реальному режиму нужен доступ к камере внутри Telegram — скоро в Mini App. Пока используй авто-определение.',
  // Live-флоу
  rec: 'REC',
  hd_30fps: 'HD @ 30FPS',
  ai_tracking_active: 'AI-ТРЕКИНГ: АКТИВЕН',
  subject_in_frame: 'ИГРОК В КАДРЕ',
  tap_to_stop: 'НАЖМИ, ЧТОБЫ ОСТАНОВИТЬ',
  tap_rec_when_ready: 'Нажми REC, когда будешь готов',
  get_in_position: 'Встань в позицию. 15 секунд.',
  go: 'Вперёд!',
  live_camera_feed: 'ЖИВАЯ КАМЕРА',
  live_correction: 'LIVE-ПОПРАВКА',
  start_live_analysis: 'НАЧАТЬ LIVE-АНАЛИЗ',
  camera_denied: 'Нужно разрешение на камеру',
  stop: 'Стоп',
  try_again: 'Попробовать ещё раз',
  all_metrics_corrected: 'Все метрики исправлены! Отличная форма!',
  errors_not_fixable: 'Есть ошибки, но live их не исправит. Попробуй упражнения.',
  analyze_title: 'АНАЛИЗ',
  stroke_type: 'ТИП УДАРА',
  auto_detect_frame: 'Авто-определение по кадру',
  detecting: 'Определяем тип удара…',
  detected: 'Определено',
  video_label: 'Видео (вертикальное, ≤ 5 сек, один удар, всё тело в кадре)',
  pick_video: 'Выбрать видео',
  analyze_run: 'Анализировать видео',
  working: 'Работаю…',
  stage_pose: 'Ищем позу…',
  stage_engine: 'Считаем метрики и фазы…',
  stage_upload: 'Загружаем видео…',
  failed: 'Ошибка',
  // VIDEO PREVIEW
  video_preview: 'ПРОВЕРКА ВИДЕО',
  checking_video: 'ПРОВЕРЯЕМ ВИДЕО...',
  video_too_low_quality: 'ВИДЕО СЛИШКОМ НИЗКОГО КАЧЕСТВА',
  retake_video: 'ПЕРЕСНЯТЬ',
  analyze_upper: 'АНАЛИЗИРОВАТЬ',
  trim_your_stroke: 'ОБРЕЗАТЬ УДАР',
  good_angle_lighting_tip: 'Хороший ракурс! Убедись, что освещение достаточное.',
  landscape_clip:
    'Клип в горизонтальной ориентации: телефон держали боком при съёмке. Анализ на нём невозможен, и поворот файла потом не исправит это.',
  square_crop:
    'Клип обрезан до квадрата. Это искажает калибровку пиксели→сантиметры, поэтому анализ на нём невозможен.',
  clip_too_long: 'Клип длиннее 5 секунд. Обрежь его до самого удара перед анализом.',
  clip_duration_unknown: 'Длительность клипа неизвестна — обрезка недоступна.',
  review_guide:
    'Экран проверки видео. Можно разобрать удар, обрезать видео двумя ручками до самого удара или переснять. Ручки обрезки перетаскиваются.',
  results_title: 'АНАЛИЗ',
  analysis_complete: 'АНАЛИЗ ЗАВЕРШЁН',
  red_explainer: 'Красный — вне диапазона лучших игроков, зелёный — внутри.',
  stroke_phases: 'ФАЗЫ УДАРА',
  no_metrics_phase: 'Для этой фазы метрик нет.',
  weaknesses: 'СЛАБЫЕ МЕСТА → УПРАЖНЕНИЯ',
  recommended_drills: 'Рекомендованные упражнения',
  no_analysis_result: 'Нет результата анализа',
  drills_title: 'AI-УПРАЖНЕНИЯ',
  only_weaknesses: 'Только под мои слабые места',
  targets: 'метрики',
  loading: 'Загрузка…',
  empty_list: 'Здесь пока пусто',
  delete: 'Удалить',
  open: 'Открыть',
  report: 'Отчёт',
  // Paywall
  paywall_title: 'AXEL AI PRO',
  paywall_desc:
    'Бесплатно: 9 видеоанализов навсегда. Pro: 20 видеоанализов в месяц + безлимитные сессии коррекции в реальном времени. Оплата Telegram Stars; продление ручное — бот напомнит до конца периода.',
  pay: 'Оплатить',
  // Boot
  starting: 'Запуск…',
  retry_login: 'Повторить вход',
};
