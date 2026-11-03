package com.rebuild.tennisai.axel

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Z.ai GLM client (OpenAI-compatible chat completions + tools).
 * No external dependencies: HttpURLConnection + org.json.
 */
data class GlmConfig(val baseUrl: String, val apiKey: String, val model: String)

data class ToolCall(val id: String, val name: String, val argsJson: String)

data class GlmReply(val text: String, val toolCalls: List<ToolCall>)

object GlmClient {
    private const val TAG = "GlmClient"

    suspend fun chat(
        config: GlmConfig,
        messages: JSONArray,
        tools: JSONArray,
        disableTools: Boolean = false,
    ): GlmReply = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", 0.6)
            .put("max_tokens", 1024)
        if (!disableTools) {
            body.put("tools", tools).put("tool_choice", "auto")
        }

        val conn = URL("${config.baseUrl.trimEnd('/')}/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 120_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText() ?: ""
            if (code !in 200..299) {
                Log.e(TAG, "GLM $code: ${text.take(400)}")
                throw IOException("GLM $code: ${text.take(200)}")
            }
            val root = JSONObject(text)
            val msg = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val content = msg.optString("content", "") ?: ""
            val calls = ArrayList<ToolCall>()
            val tc = msg.optJSONArray("tool_calls")
            if (tc != null) {
                for (i in 0 until tc.length()) {
                    val c = tc.getJSONObject(i)
                    val f = c.getJSONObject("function")
                    calls.add(ToolCall(id = c.getString("id"), name = f.getString("name"), argsJson = f.optString("arguments", "{}")))
                }
            }
            GlmReply(text = content, toolCalls = calls)
        } finally {
            conn.disconnect()
        }
    }
}
