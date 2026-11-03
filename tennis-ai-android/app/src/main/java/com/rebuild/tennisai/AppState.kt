package com.rebuild.tennisai

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.rebuild.tennisai.core.model.AnalysisResult
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.core.model.StrokeType
import org.json.JSONObject

/** Уровень игрока (чипы SKILL LEVEL в Edit Profile). */
enum class SkillLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced"), PROFESSIONAL("Professional");
}

/** Опыт игры (YEARS OF EXPERIENCE). */
enum class Experience(val label: String, val short: String) {
    UNDER_1("< 1 year", "New player"),
    Y1_3("1-3 years", "1-3 years"),
    Y3_5("3-5 years", "3-5 years"),
    Y5_10("5-10 years", "5-10 years"),
    Y10_PLUS("10+ years", "10+ years");
}

/** Основная цель (MAIN GOAL). */
enum class MainGoal(val label: String) {
    IMPROVE("Improve"), COMPETE("Compete"), FITNESS("Fitness"), FUN("Fun"), PROFESSIONAL("Professional");
}

/**
 * Общее состояние приложения: выбранный удар, рука, язык, последний результат анализа,
 * профиль игрока и настройки — всё, что видно на экранах оригинала (CLONE_SPEC §4).
 */
class AppState(app: Application) : AndroidViewModel(app) {

    var lang by mutableStateOf(Lang.RU)
    var stroke by mutableStateOf(StrokeType.SERVE)
    var handedness by mutableStateOf(Handedness.RIGHT)

    var result by mutableStateOf<AnalysisResult?>(null)

    var videoUri by mutableStateOf<android.net.Uri?>(null)

    // --- Профиль (Edit Profile / My Profile) ---
    var name by mutableStateOf("")
    var heightCm by mutableStateOf("")
    var age by mutableStateOf("")
    var weightKg by mutableStateOf("")
    var skill by mutableStateOf(SkillLevel.BEGINNER)
    var experience by mutableStateOf(Experience.UNDER_1)
    var hand by mutableStateOf(Handedness.RIGHT)
    var goal by mutableStateOf(MainGoal.IMPROVE)
    var progress by mutableStateOf(78)

    // --- Настройки (My Profile / Settings) ---
    var pushOn by mutableStateOf(true)
    var aiTipsOn by mutableStateOf(true)
    var emailOn by mutableStateOf(true)
    var voiceModeAuto by mutableStateOf(false) // чип Auto/Manual в Axel

    // --- Счётчики дашборда ---
    var sessions by mutableStateOf(0)
    var plans by mutableStateOf(0)
    var messages by mutableStateOf(0)
    var students by mutableStateOf(0)

    private val prefs = app.getSharedPreferences("app_state", Context.MODE_PRIVATE)

    init {
        val p = prefs
        lang = if (p.getString("lang", "RU") == "EN") Lang.EN else Lang.RU // русский — по умолчанию
        name = p.getString("name", "") ?: ""
        heightCm = p.getString("heightCm", "") ?: ""
        age = p.getString("age", "") ?: ""
        weightKg = p.getString("weightKg", "") ?: ""
        skill = SkillLevel.entries.firstOrNull { it.name == p.getString("skill", null) } ?: SkillLevel.BEGINNER
        experience = Experience.entries.firstOrNull { it.name == p.getString("experience", null) } ?: Experience.UNDER_1
        hand = if (p.getString("hand", "RIGHT") == "LEFT") Handedness.LEFT else Handedness.RIGHT
        goal = MainGoal.entries.firstOrNull { it.name == p.getString("goal", null) } ?: MainGoal.IMPROVE
        progress = p.getInt("progress", 78)
        pushOn = p.getBoolean("pushOn", true)
        aiTipsOn = p.getBoolean("aiTipsOn", true)
        emailOn = p.getBoolean("emailOn", true)
        voiceModeAuto = p.getBoolean("voiceModeAuto", false)
        sessions = p.getInt("sessions", 0)
        plans = p.getInt("plans", 0)
        messages = p.getInt("messages", 0)
        students = p.getInt("students", 0)
    }

    /** Пересчёт «Progress: N%» — заполненность 9 полей профиля (CLONE_SPEC §2.6). */
    fun recomputeProgress() {
        val fields = listOf(
            name.isNotBlank(), heightCm.isNotBlank(), age.isNotBlank(), weightKg.isNotBlank(),
            true, true, true, true, true, // skill/experience/hand/goal + предустановка
        )
        progress = (fields.count { it } * 100 / 9).coerceIn(0, 100)
    }

    fun saveProfile() {
        recomputeProgress()
        prefs.edit()
            .putString("name", name)
            .putString("heightCm", heightCm)
            .putString("age", age)
            .putString("weightKg", weightKg)
            .putString("skill", skill.name)
            .putString("experience", experience.name)
            .putString("hand", hand.name)
            .putString("goal", goal.name)
            .putInt("progress", progress)
            .apply()
    }

    fun saveSettings() {
        prefs.edit()
            .putString("lang", lang.name)
            .putBoolean("pushOn", pushOn)
            .putBoolean("aiTipsOn", aiTipsOn)
            .putBoolean("emailOn", emailOn)
            .putBoolean("voiceModeAuto", voiceModeAuto)
            .apply()
    }

    fun onAnalysisCompleted() {
        sessions += 1
        prefs.edit().putInt("sessions", sessions).apply()
    }

    /** Профиль для системного промпта Axel (axel_profile в SharedPreferences AxelBrain). */
    fun toAxelProfileJson(): JSONObject = JSONObject()
        .put("firstName", name)
        .put("level", skill.name.lowercase())
        .put("dominantHand", if (hand == Handedness.RIGHT) "right" else "left")
        .put("goal", goal.label.lowercase())

    companion object {
        const val ROUTE_HOME = "home"
        const val ROUTE_VIDEOS = "videos"
        const val ROUTE_ANALYSES = "analyses"
        const val ROUTE_DASHBOARD = "dashboard"
        const val ROUTE_PROFILE = "profile"

        const val ROUTE_SELECT_DASHBOARD = "select_dashboard"
        const val ROUTE_DASHBOARD_PLAYER = "dashboard_player"
        const val ROUTE_DASHBOARD_COACH = "dashboard_coach"
        const val ROUTE_PROFILE_EDIT = "profile_edit"

        const val ROUTE_LIVE = "live"
        const val ROUTE_ANALYZE = "analyze"
        const val ROUTE_RESULTS = "results"
        const val ROUTE_AXEL = "axel"
    }
}
