package com.rebuild.tennisai.ui.analyze

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.core.engine.AnalysisEngine
import com.rebuild.tennisai.core.metrics.MetricsCalculator
import com.rebuild.tennisai.core.metrics.playerHeightCm
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.data.FrameExtractor
import kotlinx.coroutines.launch

/**
 * Video analysis screen: clip pick → quality checks (as in the original) →
 * the "DETECTING KEYPOINTS → CALCULATING JOINT ANGLES → MAPPING TORSO
 * ROTATION → ANALYZING KNEE FLEXION → FINALIZING REPORT" pipeline.
 */
@Composable
fun AnalyzeScreen(state: AppState, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ru = state.lang == Lang.RU
    val prefs = remember { context.getSharedPreferences("profile", android.content.Context.MODE_PRIVATE) }

    var stage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showHeightDialog by remember { mutableStateOf(false) }
    var heightText by remember { mutableStateOf((playerHeightCm.toInt()).toString()) }

    fun startAnalysis(uri: Uri) {
        stage = if (ru) "ДЕТЕКЦИЯ КЛЮЧЕВЫХ ТОЧЕК..." else "DETECTING KEYPOINTS..."
        progress = 0.05f
        error = null
        scope.launch {
            try {
                runAnalysis(state, uri) { s, p -> stage = s; progress = p }
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "error"
                stage = null
            }
        }
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri == null) return
        // --- Clip checks (texts are verbatim from the original) ---
        val check = checkClip(context, uri)
        if (check != null) {
            error = check
            stage = null
            return
        }
        // Player height is required: the basis for converting normalized coordinates to centimeters.
        val saved = prefs.getInt("height_cm", 0)
        if (saved in 120..230) {
            playerHeightCm = saved.toFloat()
            startAnalysis(uri)
        } else {
            pendingUri = uri
            showHeightDialog = true
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onVideoPicked(uri)
    }

    if (showHeightDialog) {
        AlertDialog(
            onDismissRequest = { showHeightDialog = false },
            title = { Text(if (ru) "Введите рост в см" else "Enter your height in cm") },
            text = {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("HEIGHT (CM)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = heightText.toIntOrNull()
                    if (h != null && h in 120..230) {
                        prefs.edit().putInt("height_cm", h).apply()
                        playerHeightCm = h.toFloat()
                        showHeightDialog = false
                        pendingUri?.let { startAnalysis(it) }
                        pendingUri = null
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showHeightDialog = false; pendingUri = null }) { Text(if (ru) "Отмена" else "Cancel") } },
        )
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            if (ru) "Анализ: ${strokeTitle(state)}" else "Analyze: ${strokeTitle(state)}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            if (ru) "Один удар, до 5 секунд, боковой ракурс, всё тело в кадре."
            else "One stroke, up to 5 seconds, side view, full body visible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        if (stage == null) {
            Button(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }) {
                Text(if (ru) "Выбрать видео" else "Pick a video")
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            Text(stage ?: "", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun strokeTitle(state: AppState): String {
    val ru = state.lang == Lang.RU
    return when (state.stroke) {
        com.rebuild.tennisai.core.model.StrokeType.SERVE -> if (ru) "Подача" else "Serve"
        com.rebuild.tennisai.core.model.StrokeType.FOREHAND -> if (ru) "Форхенд" else "Forehand"
        com.rebuild.tennisai.core.model.StrokeType.BACKHAND -> if (ru) "Бэкхенд" else "Backhand"
    }
}

/** Clip checks with the original's texts. null means everything is fine. */
private fun checkClip(context: android.content.Context, uri: Uri): String? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val (pw, ph) = if (rot == 90 || rot == 270) h to w else w to h
        when {
            // "The clip is in landscape: the PHONE was held horizontally while filming..."
            pw > ph -> "Клип в альбомной ориентации: телефон держали горизонтально. Анализ невозможен — снимите в портретном режиме. / The clip is in landscape: the PHONE was held horizontally while filming. Film again in portrait."
            // "VIDEO TOO LOW QUALITY"
            ph < 480 -> "ВИДЕО СЛИШКОМ НИЗКОГО КАЧЕСТВА / VIDEO TOO LOW QUALITY"
            // "The analysis needs a portrait frame; a square crop distorts the scale used to convert pixels to centimetres."
            ph < pw * 4 / 3 -> "Нужен вертикальный кадр: квадратный кроп искажает масштаб перевода пикселей в сантиметры. / The analysis needs a portrait frame; a square crop distorts the scale."
            // ≤5 seconds in the original; we simply reject overly long clips without a separate warning
            durationMs > 12_000 -> "Слишком длинный клип: один удар, до 5 секунд, можно обрезать на следующем экране. / Make sure the video is max 5 seconds. You can trim it on the next screen."
            else -> null
        }
    } catch (e: Exception) {
        "Не удалось прочитать видео / Could not read the video: ${e.message?.take(80)}"
    } finally {
        try { r.release() } catch (_: Exception) {}
    }
}

private suspend fun runAnalysis(
    state: AppState,
    uri: Uri,
    onStage: (String, Float) -> Unit,
) {
    val context = state.getApplication<android.app.Application>()
    val ru = state.lang == Lang.RU
    val extractor = FrameExtractor(context)
    val frames = extractor.extractPoses(uri, maxFrames = 150) { p ->
        val frac = if (p.total == 0) 0f else p.done.toFloat() / p.total
        onStage(
            if (ru) "ДЕТЕКЦИЯ КЛЮЧЕВЫХ ТОЧЕК... (${p.done}/${p.total})" else "DETECTING KEYPOINTS... (${p.done}/${p.total})",
            0.05f + 0.6f * frac,
        )
    }
    if (frames.size < 8) {
        // "SUBJECT IN FRAME" — the player was not recognized
        throw IllegalStateException(
            if (ru) "ИГРОК НЕ В КАДРЕ. Снимите так, чтобы всё тело было видно с бокового ракурса."
            else "SUBJECT IN FRAME. Film from the side with the full body visible."
        )
    }
    onStage(if (ru) "РАСЧЁТ СУСТАВНЫХ УГЛОВ..." else "CALCULATING JOINT ANGLES...", 0.72f)
    onStage(if (ru) "ПОСТРОЕНИЕ РОТАЦИИ ТОРСА..." else "MAPPING TORSO ROTATION...", 0.82f)
    onStage(if (ru) "АНАЛИЗ СГИБАНИЯ КОЛЕНЕЙ..." else "ANALYZING KNEE FLEXION...", 0.9f)
    val result = AnalysisEngine.analyze(frames, state.stroke, state.handedness, state.lang)
    onStage(if (ru) "ФИНАЛИЗАЦИЯ ОТЧЁТА..." else "FINALIZING REPORT...", 1f)
    state.result = result
}
