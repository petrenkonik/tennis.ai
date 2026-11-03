package com.rebuild.tennisai.axel

import android.content.SharedPreferences
import com.rebuild.tennisai.core.model.AnalysisResult
import com.rebuild.tennisai.core.model.MetricStatus
import com.rebuild.tennisai.core.model.StrokeType
import org.json.JSONArray
import org.json.JSONObject

/** Action for the app UI (client-side tools recovered from the reverse engineering). */
sealed class AxelAction {
    data class Navigate(val route: String) : AxelAction()
    data class AnalyzeVideo(val stroke: StrokeType?) : AxelAction()
    data class SetLanguage(val code: String) : AxelAction()
    object StartLive : AxelAction()
}

data class AxelTurn(val reply: String, val actions: List<AxelAction>)

/**
 * Axel's "brain" on GLM: tools restored verbatim from the original's Dart AOT
 * (reference/dart-aot-recovery/AXEL_RECOVERY.md). Local data lives in SharedPreferences.
 */
class AxelBrain(
    private val prefs: SharedPreferences,
    private val lastAnalysis: () -> AnalysisResult?,
    private val onAction: (AxelAction) -> Unit,
) {

    private fun tool(
        name: String,
        description: String,
        properties: JSONObject = JSONObject(),
        required: JSONArray = JSONArray(),
    ): JSONObject {
        val fn = JSONObject().put("name", name).put("description", description)
            .put("parameters", JSONObject().put("type", "object").put("properties", properties))
        if (required.length() > 0) fn.put("required", required)
        return JSONObject().put("type", "function").put("function", fn)
    }

    fun tools(): JSONArray {
        val arr = JSONArray()
        arr.put(
            tool(
                "analyze_video",
                "Start an analysis for a stroke. mode: camera (film now, default) or gallery (import an existing clip). strokeType: the stroke the player named, if they named one -- fill it whenever they say it, even in passing.",
                JSONObject()
                    .put("mode", JSONObject().put("type", "string").put("enum", JSONArray(listOf("camera", "gallery"))))
                    .put("strokeType", JSONObject().put("type", "string").put("enum", JSONArray(listOf("serve", "forehand", "backhand")))),
                JSONArray(listOf("mode")),
            )
        )
        arr.put(
            tool(
                "start_live_analysis",
                "Open the LIVE corrections screen: the camera watches the technique in real time and shows correction cards for stance metrics.",
            )
        )
        arr.put(
            tool(
                "navigate",
                "Open an app screen. ONLY these destination values exist: home, analysis (last results), record (video analysis flow), live (real-time corrections). Do NOT use record when the player wants live corrections -- use start_live_analysis.",
                JSONObject().put(
                    "destination",
                    JSONObject().put("type", "string").put("enum", JSONArray(listOf("home", "analysis", "record", "live"))),
                ),
                JSONArray(listOf("destination")),
            )
        )
        arr.put(
            tool(
                "show_last_analysis",
                "Show the last analysis report on screen and summarize it: score /100, phases M0-M6, red metrics with corrections.",
            )
        )
        arr.put(
            tool(
                "fetch_analysis_history",
                "Fetch the player's analysis session history available on this device (total sessions, average score, breakdown by stroke, most recent analysis with red metrics).",
            )
        )
        arr.put(
            tool(
                "get_player_profile",
                "Fetch the current player profile (name, level, dominant hand, goal). Use this to personalize advice or check what info you already have.",
            )
        )
        arr.put(
            tool(
                "save_player_profile",
                "Store facts the player gives about themselves (first name, level, dominant hand, goal). One field per call is fine; call it as soon as each fact is clear.",
                JSONObject()
                    .put("firstName", JSONObject().put("type", "string"))
                    .put("level", JSONObject().put("type", "string").put("enum", JSONArray(listOf("beginner", "intermediate", "advanced", "pro"))))
                    .put("dominantHand", JSONObject().put("type", "string").put("enum", JSONArray(listOf("left", "right"))))
                    .put("goal", JSONObject().put("type", "string")),
            )
        )
        arr.put(
            tool(
                "record_correction",
                "Record which coaching correction was given for a metric and whether it worked (coaching memory). If this is the first time coaching this metric, there is no history yet -- record anyway.",
                JSONObject()
                    .put("metricCode", JSONObject().put("type", "string"))
                    .put("advice", JSONObject().put("type", "string"))
                    .put("worked", JSONObject().put("type", "boolean")),
                JSONArray(listOf("metricCode", "advice")),
            )
        )
        arr.put(
            tool(
                "set_session_intent",
                "Record what the player wants to do THIS session, as soon as they say it in their own words -- 'I just want to do a drill today', 'give me a full analysis'. Call this once, as soon as the intent is clear.",
                JSONObject().put(
                    "intent",
                    JSONObject().put("type", "string").put("enum", JSONArray(listOf("full_analysis", "drill_only", "solo_tripod", "quick_question"))),
                ),
                JSONArray(listOf("intent")),
            )
        )
        arr.put(
            tool(
                "set_app_language",
                "Change the app language when the player asks: 'parle-moi en francais', 'switch to Spanish'. Only these are available: en, ru. Say plainly if another language is not available yet.",
                JSONObject().put("code", JSONObject().put("type", "string").put("enum", JSONArray(listOf("en", "ru")))),
                JSONArray(listOf("code")),
            )
        )
        arr.put(
            tool(
                "fetch_recommended_drills",
                "Fetch drills targeted at the weaknesses of the last analysis. The drill library on this offline build is limited -- be honest if nothing matches.",
            )
        )
        return arr
    }

    fun systemPrompt(): String {
        val profile = profileJson()
        return """
You are Axel, an expert tennis coach inside the Tennis AI Android app. You are talking to the player themselves.
You coach them about stroke technique based on biomechanical analysis: phases M0-M6 (ready, stance/split step, toss/unit turn, loading, extension/drive, contact point, recovery) and per-phase metrics that are green when inside the pro range and red when outside.

Language: always reply in the language the player is using.

How to read results, once and briefly when results are first discussed: red is outside the range the best players sit in, green is inside it. This is the only time you will say it. A score is a score out of 100, never a percentage.

Rules:
- Use tools to fetch real data instead of guessing; this build runs on-device without a backend, so be honest when something is unavailable.
- When the player wants to record and analyze a stroke, call analyze_video. For live real-time corrections call start_live_analysis.
- During onboarding or missing-fact questions: ask ONE question per message, store each answer with save_player_profile.
- Never invent a fault or a correction. If no analysis exists, invite the player to record one (analyze_video) instead of guessing.
- Be concise, warm and concrete. Coach like a person, not like a report.

Current player profile: $profile
""".trim()
    }

    private fun profileJson(): JSONObject {
        val raw = prefs.getString(KEY_PROFILE, null)
        val obj = try {
            JSONObject(raw ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        if (!obj.has("level")) obj.put("level", "unknown")
        return obj
    }

    private fun memoryJson(): JSONObject = try {
        JSONObject(prefs.getString(KEY_MEMORY, null) ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    /** Full tool-calling dialogue loop. history holds the previous turns, without the system prompt. */
    suspend fun respond(config: GlmConfig, history: List<Pair<String, String>>, userMessage: String): AxelTurn {
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", systemPrompt()))
        for ((role, content) in history) {
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        val ctx = lastAnalysis()
        val contextLine = buildString {
            append("Player: ${profileJson().optString("firstName", "Player")}, level: ${profileJson().optString("level", "unknown")}")
            if (ctx != null) {
                append("\n\nLast analysis (JSON): ")
                append(
                    JSONObject()
                        .put("stroke", ctx.stroke.id)
                        .put("score", ctx.overallScore)
                        .put("reds", ctx.redMetrics.size)
                        .put(
                            "redMetrics",
                            JSONArray(ctx.redMetrics.take(8).map { JSONObject().put("phase", it.first.key).put("metric", it.second.id).put("value", it.second.value) }),
                        ),
                )
            } else {
                append("\n\nNo analysis yet on this device.")
            }
        }
        messages.put(JSONObject().put("role", "user").put("content", "$contextLine\n\n$userMessage"))

        val actions = ArrayList<AxelAction>()
        val tools = tools()
        for (round in 1..MAX_ROUNDS) {
            val reply = GlmClient.chat(config, messages, tools)
            if (reply.toolCalls.isEmpty()) return AxelTurn(reply.text.trim(), actions)

            val assistant = JSONObject().put("role", "assistant").put("content", reply.text)
            val rawCalls = JSONArray()
            for (call in reply.toolCalls) {
                rawCalls.put(JSONObject().put("id", call.id).put("type", "function").put("function", JSONObject().put("name", call.name).put("arguments", call.argsJson)))
            }
            assistant.put("tool_calls", rawCalls)
            messages.put(assistant)

            for (call in reply.toolCalls) {
                val args = try {
                    JSONObject(call.argsJson)
                } catch (_: Exception) {
                    JSONObject()
                }
                val result = execute(args, call.name, actions)
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", result),
                )
            }
        }
        val final = GlmClient.chat(config, messages, tools, disableTools = true)
        return AxelTurn(final.text.trim(), actions)
    }

    private fun execute(args: JSONObject, name: String, actions: MutableList<AxelAction>): JSONObject {
        if (name != "record_correction" && name != "get_player_profile") {
            // Coaching memory: context of past advice is available to the model via the profile.
        }
        return when (name) {
            "analyze_video" -> {
                val stroke = args.optString("strokeType", "").takeIf { it.isNotEmpty() }
                actions(AxelAction.AnalyzeVideo(stroke?.let { StrokeType.fromId(it) }))
                JSONObject().put("ok", true).put("note", "Recording screen opened. Guide the player: portrait, side view, full body, one stroke, up to 5 seconds.")
            }
            "start_live_analysis" -> {
                actions(AxelAction.StartLive)
                JSONObject().put("ok", true).put("note", "Live corrections screen opened.")
            }
            "navigate" -> {
                val dest = args.optString("destination", "home")
                actions(AxelAction.Navigate(dest))
                JSONObject().put("ok", true)
            }
            "set_app_language" -> {
                val code = args.optString("code", "en")
                actions(AxelAction.SetLanguage(code))
                JSONObject().put("ok", true)
            }
            "show_last_analysis" -> {
                val ctx = lastAnalysis()
                if (ctx == null) {
                    JSONObject().put("found", false).put("note", "No analysis yet. Invite the player to record one (analyze_video).")
                } else {
                    actions(AxelAction.Navigate("analysis"))
                    analysisJson(ctx).put("found", true).put("shownOnScreen", true)
                }
            }
            "fetch_analysis_history" -> {
                val ctx = lastAnalysis()
                JSONObject()
                    .put("totalSessions", if (ctx == null) 0 else 1)
                    .put("mostRecent", if (ctx == null) JSONObject.NULL else analysisJson(ctx))
                    .put("note", "Only the latest analysis is stored on this offline build.")
            }
            "get_player_profile" -> profileJson()
            "save_player_profile" -> {
                val p = profileJson()
                for (key in listOf("firstName", "level", "dominantHand", "goal")) {
                    if (args.has(key) && !args.isNull(key)) p.put(key, args.get(key))
                }
                prefs.edit().putString(KEY_PROFILE, p.toString()).apply()
                JSONObject().put("ok", true).put("profile", p)
            }
            "record_correction" -> {
                val m = memoryJson()
                m.put(args.optString("metricCode", "unknown"), JSONObject().put("advice", args.optString("advice")).put("worked", if (args.has("worked")) args.get("worked") else JSONObject.NULL))
                prefs.edit().putString(KEY_MEMORY, m.toString()).apply()
                JSONObject().put("ok", true)
            }
            "set_session_intent" -> {
                prefs.edit().putString(KEY_INTENT, args.optString("intent", "quick_question")).apply()
                JSONObject().put("ok", true)
            }
            "fetch_recommended_drills" -> {
                // No drill library in the offline build — answer honestly (as in the original: "No drills available").
                val ctx = lastAnalysis()
                JSONObject()
                    .put("drills", JSONArray())
                    .put("weaknesses", if (ctx == null) JSONArray() else JSONArray(ctx.redMetrics.map { it.second.id }))
                    .put("note", "Drill library is not part of this offline build yet. Suggest a simple exercise yourself and say the library is coming.")
            }
            else -> JSONObject().put("error", "unhandled tool: $name")
        }
    }

    private fun analysisJson(ctx: AnalysisResult): JSONObject {
        val reds = JSONArray(ctx.redMetrics.take(10).map {
            JSONObject().put("phase", it.first.key).put("metric", it.second.id).put("value", it.second.value).put("pro", "${it.second.proMin}-${it.second.proMax}")
        })
        return JSONObject()
            .put("stroke", ctx.stroke.id)
            .put("score", ctx.overallScore)
            .put("frames", ctx.frameCount)
            .put("durationMs", ctx.durationMs)
            .put("redMetrics", reds)
            .put("greenCount", ctx.moments.sumOf { m -> m.metrics.count { it.status == MetricStatus.GREEN } })
    }

    private fun actions(a: AxelAction) {
        onAction(a)
    }

    companion object {
        private const val MAX_ROUNDS = 6
        const val KEY_PROFILE = "axel_profile"
        const val KEY_MEMORY = "axel_memory"
        const val KEY_INTENT = "axel_intent"
        const val KEY_API_KEY = "glm_api_key"
        const val KEY_BASE_URL = "glm_base_url"
        const val KEY_MODEL = "glm_model"
        const val DEFAULT_BASE_URL = "https://api.z.ai/api/paas/v4"
        const val DEFAULT_MODEL = "glm-5.3-flash"
    }
}
