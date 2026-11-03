package com.rebuild.tennisai.core.i18n

import com.rebuild.tennisai.Experience
import com.rebuild.tennisai.MainGoal
import com.rebuild.tennisai.SkillLevel
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lang

/**
 * Строки интерфейса клона. EN — дословно из оригинального `reference/extracted_assets/en.json`
 * (реверс APK v2.0.19); RU — перевод (язык по умолчанию).
 * НЕизвестный ключ возвращается как есть — так в оригинале выглядели невыровненные
 * i18n-ключи («track_progression», «no_data», «record_first_video», «manage_students_perf»).
 */
object T {
    fun t(lang: Lang, key: String): String = when (lang) {
        Lang.RU -> RU[key] ?: EN[key] ?: key
        Lang.EN -> EN[key] ?: key
    }

    fun skillLabel(s: SkillLevel, lang: Lang): String = t(lang, when (s) {
        SkillLevel.BEGINNER -> "level_beginner"
        SkillLevel.INTERMEDIATE -> "level_intermediate"
        SkillLevel.ADVANCED -> "level_advanced"
        SkillLevel.PROFESSIONAL -> "level_professional"
    })

    fun expLabel(e: Experience, lang: Lang): String = t(lang, when (e) {
        Experience.UNDER_1 -> "exp_under_1"
        Experience.Y1_3 -> "exp_1_3"
        Experience.Y3_5 -> "exp_3_5"
        Experience.Y5_10 -> "exp_5_10"
        Experience.Y10_PLUS -> "exp_10_plus"
    })

    fun expShort(e: Experience, lang: Lang): String =
        if (e == Experience.UNDER_1) t(lang, "new_player") else expLabel(e, lang)

    fun goalChip(g: MainGoal, lang: Lang): String = t(lang, when (g) {
        MainGoal.IMPROVE -> "goal_improve_skills"
        MainGoal.COMPETE -> "goal_compete"
        MainGoal.FITNESS -> "goal_fitness"
        MainGoal.FUN -> "goal_fun"
        MainGoal.PROFESSIONAL -> "goal_professional"
    })

    fun handLabel(h: Handedness, lang: Lang): String = t(lang, if (h == Handedness.RIGHT) "hand_right" else "hand_left")

    fun sessions(n: Int, lang: Lang): String = "$n ${t(lang, "sessions")}"

    private val EN = mapOf(
        // Home
        "realtime_analysis" to "REAL-TIME ANALYSIS",
        "training_modules" to "TRAINING MODULES",
        "coach_mode" to "Coach Mode",
        "realtime_for_coaches" to "Real-time analysis for coaches",
        "auto_detect" to "Auto Detect",
        "auto_detect_desc" to "Auto-detect your stroke and analyze it",
        "serve_analysis" to "Serve Analysis",
        "full_motion_posture" to "Full Motion & Posture",
        "forehand" to "Forehand",
        "rotation_extension" to "Rotation & Extension",
        "backhand" to "Backhand",
        "handed_technique" to "1 or 2 Handed Technique",
        "ai_drills" to "AI Drills",
        "personalized_exercises" to "Personalized Exercises",
        "coach" to "COACH",
        "new" to "NEW",
        "sessions" to "sessions",
        // Select dashboard
        "select_dashboard" to "SELECT DASHBOARD",
        "player_dashboard" to "PLAYER DASHBOARD",
        "track_personal_progress" to "Track your personal progress and sessions",
        "coach_dashboard" to "COACH DASHBOARD",
        "back" to "BACK",
        // Player dashboard
        "my_dashboard" to "My Dashboard",
        "my_sessions" to "My Sessions",
        "skill_analysis" to "Skill Analysis",
        "training_plans" to "TRAINING PLANS",
        "messages" to "MESSAGES",
        "get_1_year_free" to "Get 1 year free",
        "invite_10_friends" to "Invite friends → unlock free months",
        "recent_activity" to "Recent Activity",
        "no_sessions_yet" to "No sessions yet",
        // Coach dashboard
        "welcome_coach" to "Welcome, Coach!",
        "student_overview" to "Student overview",
        "my_students" to "My Students",
        "analysis" to "Analysis",
        "add_students_free" to "Add 10 students → unlock free months",
        "no_student_activity" to "No student activity yet",
        "add_students_to_see" to "Add students to see their activity",
        // Profile
        "my_profile" to "My Profile",
        "ambassador_program" to "Ambassador Program",
        "ambassador_subtitle" to "Share and get rewarded",
        "level" to "Level",
        "new_player" to "New player",
        "goal" to "Goal",
        "progress" to "Progress",
        "free_plan" to "Free Plan",
        "free" to "FREE",
        "free_desc" to "Upgrade to unlock unlimited analyses",
        "settings" to "Settings",
        "push_notifications" to "Push Notifications",
        "ai_coaching_tips" to "AI Coaching Tips",
        "email_notifications" to "Email Notifications",
        "app_version" to "App Version 2.0.1",
        "language" to "Language",
        // Edit profile
        "edit_profile" to "Edit Profile",
        "save" to "Save",
        "general" to "General",
        "additional" to "Additional",
        "name" to "Name",
        "height_cm" to "Height (cm)",
        "age" to "Age",
        "weight_placeholder" to "75 kg",
        "skill_level" to "SKILL LEVEL",
        "years_experience" to "YEARS OF EXPERIENCE",
        "dominant_hand" to "DOMINANT HAND",
        "main_goal" to "MAIN GOAL",
        "level_beginner" to "Beginner",
        "level_intermediate" to "Intermediate",
        "level_advanced" to "Advanced",
        "level_professional" to "Professional",
        "exp_under_1" to "< 1 year",
        "exp_1_3" to "1-3 years",
        "exp_3_5" to "3-5 years",
        "exp_5_10" to "5-10 years",
        "exp_10_plus" to "10+ years",
        "hand_right" to "Right",
        "hand_left" to "Left",
        "goal_improve_skills" to "Improve skills",
        "goal_compete" to "Compete",
        "goal_fitness" to "Fitness",
        "goal_fun" to "Fun",
        "goal_professional" to "Professional",
        // Analyses / videos
        "my_analyses" to "MY ANALYSES",
        "my_videos" to "MY VIDEOS",
        "total" to "TOTAL",
        "completed" to "Completed",
        "analyzing" to "Analyzing",
        "serve" to "Serve",
        "drills" to "Drills",
        "videos_upper" to "VIDEOS",
        "no_videos_yet" to "No videos yet",
        // Axel
        "meet" to "Meet",
        "companion" to "Your intelligent tennis companion.",
        "s1_title" to "Tell Axel about your game",
        "s1_body" to "Tell me a bit about your tennis — how would you describe your game?",
        "s2_title" to "Beat the nerves",
        "s2_body" to "Before an important match",
        "s3_title" to "How's a top pro playing?",
        "s3_body" to "Current form and recent results",
        "s4_title" to "Analyse my first stroke",
        "s4_body" to "Send a video for full analysis",
        "reply_hint" to "Reply to Axel...",
        "record" to "Record",
        "upload" to "Upload",
        "speak" to "Speak",
        "send" to "Send",
        "manual" to "Manual",
        "auto" to "Auto",
        "thinking" to "Axel is thinking…",
        "toast_voice_in" to "Voice input is not available in this build",
        "toast_voice_out" to "Voice playback is not available in this build",
    )

    private val RU = mapOf(
        // Home
        "realtime_analysis" to "АНАЛИЗ В РЕАЛЬНОМ ВРЕМЕНИ",
        "training_modules" to "ТРЕНИРОВОЧНЫЕ МОДУЛИ",
        "coach_mode" to "Режим тренера",
        "realtime_for_coaches" to "Анализ в реальном времени для тренеров",
        "auto_detect" to "Авто-определение",
        "auto_detect_desc" to "Определи удар автоматически и получи разбор",
        "serve_analysis" to "Анализ подачи",
        "full_motion_posture" to "Полное движение и поза",
        "forehand" to "Форхенд",
        "rotation_extension" to "Вращение и вытяжение",
        "backhand" to "Бэкхенд",
        "handed_technique" to "Техника в одну или две руки",
        "ai_drills" to "AI-упражнения",
        "personalized_exercises" to "Персональные упражнения",
        "coach" to "ТРЕНЕР",
        "new" to "НОВОЕ",
        "sessions" to "сессий",
        // Select dashboard
        "select_dashboard" to "ВЫБОР ДАШБОРДА",
        "player_dashboard" to "ДАШБОРД ИГРОКА",
        "track_personal_progress" to "Твой личный прогресс и сессии",
        "coach_dashboard" to "ДАШБОРД ТРЕНЕРА",
        "back" to "НАЗАД",
        // Player dashboard
        "my_dashboard" to "Мой дашборд",
        "my_sessions" to "Мои сессии",
        "skill_analysis" to "Анализ техники",
        "training_plans" to "ПЛАНЫ ТРЕНИРОВОК",
        "messages" to "СООБЩЕНИЯ",
        "get_1_year_free" to "Получи год бесплатно",
        "invite_10_friends" to "Пригласи друзей → открой бесплатные месяцы",
        "recent_activity" to "Недавняя активность",
        "no_sessions_yet" to "Пока нет сессий",
        // Coach dashboard
        "welcome_coach" to "Добро пожаловать, Тренер!",
        "student_overview" to "Обзор учеников",
        "my_students" to "Мои ученики",
        "analysis" to "Анализы",
        "add_students_free" to "Добавь 10 учеников → открой бесплатные месяцы",
        "no_student_activity" to "У учеников пока нет активности",
        "add_students_to_see" to "Добавь учеников, чтобы видеть их активность",
        // Profile
        "my_profile" to "Мой профиль",
        "ambassador_program" to "Амбассадорская программа",
        "ambassador_subtitle" to "Делись и получай награды",
        "level" to "Уровень",
        "new_player" to "Новый игрок",
        "goal" to "Цель",
        "progress" to "Прогресс",
        "free_plan" to "Бесплатный план",
        "free" to "БЕСПЛАТНО",
        "free_desc" to "Обнови план для безлимитных анализов",
        "settings" to "Настройки",
        "push_notifications" to "Push-уведомления",
        "ai_coaching_tips" to "AI-подсказки коуча",
        "email_notifications" to "Email-уведомления",
        "app_version" to "Версия приложения 2.0.1",
        "language" to "Язык",
        // Edit profile
        "edit_profile" to "Редактирование профиля",
        "save" to "Сохранить",
        "general" to "Основное",
        "additional" to "Дополнительно",
        "name" to "Имя",
        "height_cm" to "Рост (см)",
        "age" to "Возраст",
        "weight_placeholder" to "75 кг",
        "skill_level" to "УРОВЕНЬ ИГРОКА",
        "years_experience" to "ОПЫТ ИГРЫ",
        "dominant_hand" to "ВЕДУЩАЯ РУКА",
        "main_goal" to "ГЛАВНАЯ ЦЕЛЬ",
        "level_beginner" to "Новичок",
        "level_intermediate" to "Средний",
        "level_advanced" to "Продвинутый",
        "level_professional" to "Профи",
        "exp_under_1" to "< 1 года",
        "exp_1_3" to "1-3 года",
        "exp_3_5" to "3-5 лет",
        "exp_5_10" to "5-10 лет",
        "exp_10_plus" to "10+ лет",
        "hand_right" to "Правша",
        "hand_left" to "Левша",
        "goal_improve_skills" to "Улучшение навыков",
        "goal_compete" to "Соревнования",
        "goal_fitness" to "Фитнес",
        "goal_fun" to "Удовольствие",
        "goal_professional" to "Профи",
        // Analyses / videos
        "my_analyses" to "МОИ АНАЛИЗЫ",
        "my_videos" to "МОИ ВИДЕО",
        "total" to "ВСЕГО",
        "completed" to "Завершено",
        "analyzing" to "Анализируется",
        "serve" to "Подача",
        "drills" to "Упражнения",
        "videos_upper" to "ВИДЕО",
        "no_videos_yet" to "Пока нет видео",
        // Axel
        "meet" to "Знакомьтесь,",
        "companion" to "Твой умный теннисный компаньон.",
        "s1_title" to "Расскажи Axel о своей игре",
        "s1_body" to "Расскажи немного о своём теннисе — как бы ты описал свою игру?",
        "s2_title" to "Победить волнение",
        "s2_body" to "Перед важным матчем",
        "s3_title" to "Как играет топ-профи?",
        "s3_body" to "Текущая форма и свежие результаты",
        "s4_title" to "Разбери мой первый удар",
        "s4_body" to "Пришли видео для полного анализа",
        "reply_hint" to "Ответь Axel...",
        "record" to "Запись",
        "upload" to "Загрузить",
        "speak" to "Голос",
        "send" to "Отправить",
        "manual" to "Ручной",
        "auto" to "Авто",
        "thinking" to "Axel думает…",
        "toast_voice_in" to "Голосовой ввод недоступен в этой сборке",
        "toast_voice_out" to "Голосовое воспроизведение недоступно в этой сборке",
    )
}
