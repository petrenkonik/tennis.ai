package com.rebuild.tennisai.ui.axel

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.axel.AxelAction
import com.rebuild.tennisai.axel.AxelBrain
import com.rebuild.tennisai.axel.GlmConfig
import com.rebuild.tennisai.core.i18n.T
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.ui.common.AppCard
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.AccentSoft
import com.rebuild.tennisai.ui.theme.HoloTeal
import com.rebuild.tennisai.ui.theme.StopRed
import com.rebuild.tennisai.ui.theme.TextDim
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class ChatLine(val role: String, val text: String)

private data class Suggestion(val titleKey: String, val bodyKey: String)

// CLONE_SPEC §2.2 — the original's suggestion cards (texts from REA; ones cropped in the screenshots were completed)
private val SUGGESTIONS = listOf(
    Suggestion("s1_title", "s1_body"),
    Suggestion("s2_title", "s2_body"),
    Suggestion("s3_title", "s3_body"),
    Suggestion("s4_title", "s4_body"),
)

@Composable
fun AxelScreen(state: AppState, onBack: () -> Unit, onRecord: () -> Unit, onUpload: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("axel", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val lang = state.lang

    val lines = remember { mutableStateListOf<ChatLine>() }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(lines.size, busy) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    fun config(): GlmConfig? {
        val apiKey = prefs.getString(AxelBrain.KEY_API_KEY, "") ?: ""
        if (apiKey.isBlank()) return null
        return GlmConfig(
            baseUrl = prefs.getString(AxelBrain.KEY_BASE_URL, AxelBrain.DEFAULT_BASE_URL) ?: AxelBrain.DEFAULT_BASE_URL,
            apiKey = apiKey,
            model = prefs.getString(AxelBrain.KEY_MODEL, AxelBrain.DEFAULT_MODEL) ?: AxelBrain.DEFAULT_MODEL,
        )
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || busy) return
        val cfg = config()
        if (cfg == null) {
            showKeyDialog = true
            return
        }
        draft = ""
        lines.add(ChatLine("user", clean))
        busy = true
        error = null
        job = scope.launch {
            try {
                val brain = AxelBrain(prefs, lastAnalysis = { state.result }) { action ->
                    when (action) {
                        is AxelAction.AnalyzeVideo -> {
                            action.stroke?.let { state.stroke = it }
                            onRecord()
                        }
                        is AxelAction.StartLive -> onRecord()
                        is AxelAction.Navigate -> if (action.route == "record") onRecord()
                        is AxelAction.SetLanguage -> {
                            state.lang = if (action.code == "ru") com.rebuild.tennisai.core.model.Lang.RU else com.rebuild.tennisai.core.model.Lang.EN
                            state.saveSettings()
                        }
                    }
                }
                val turn = brain.respond(cfg, history.toList(), clean)
                history.add("user" to clean)
                history.add("assistant" to turn.reply)
                lines.add(ChatLine("coach", turn.reply.ifBlank { "…" }))
            } catch (e: Exception) {
                error = "GLM: ${e.message?.take(160)}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Header (CLONE_SPEC §2.2)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Axel", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF66792F), RoundedCornerShape(14.dp))
                    .clickable {
                        state.voiceModeAuto = !state.voiceModeAuto
                        state.saveSettings()
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    if (state.voiceModeAuto) T.t(lang, "auto") else T.t(lang, "manual"),
                    color = AccentSoft,
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.VolumeUp,
                null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp).clickable {
                    Toast.makeText(context, T.t(lang, "toast_voice_out"), Toast.LENGTH_SHORT).show()
                },
            )
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (busy) StopRed else StopRed.copy(alpha = 0.85f))
                    .clickable {
                        job?.cancel()
                        busy = false
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
            }
            Spacer(Modifier.width(14.dp))
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp).clickable { onBack() },
            )
        }

        if (lines.isEmpty()) {
            // Idle: hologram + title + suggestion cards (weight keeps the input panel on screen)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Hologram(Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 20.dp))
                Spacer(Modifier.height(18.dp))
                Row {
                    Text("${T.t(lang, "meet")} ", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("Axel AI", color = Accent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    T.t(lang, "companion"),
                    color = TextSecondary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SUGGESTIONS.chunked(2).forEach { rowCards ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowCards.forEach { s ->
                                AppCard(Modifier.weight(1f).height(150.dp), radius = 16.dp, onClick = { send(T.t(lang, s.titleKey)) }) {
                                    Column(
                                        Modifier.padding(14.dp).fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            T.t(lang, s.titleKey),
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            T.t(lang, s.bodyKey),
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                            if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Chat thread
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(lines) { line ->
                    val isUser = line.role == "user"
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.86f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isUser) Accent else Color(0xFF1B1B1D))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                line.text,
                                color = if (isUser) Color(0xFF141400) else TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
                if (busy) {
                    item {
                        Text(T.t(lang, "thinking"), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        }

        error?.let {
            Text(
                it,
                color = StopRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 2,
            )
        }

        // Bottom bar: Record/Upload · input · Speak/Send
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            LabeledCircle(T.t(lang, "record"), Icons.Outlined.Videocam, Color(0xFF1B1B1D), TextPrimary) { onRecord() }
            Spacer(Modifier.width(8.dp))
            LabeledCircle(T.t(lang, "upload"), Icons.Outlined.Upload, Color(0xFF1B1B1D), TextPrimary) { onUpload() }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF1B1B1D))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                    cursorBrush = Brush.verticalGradient(listOf(Accent, Accent)),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text(T.t(lang, "reply_hint"), color = TextDim, fontSize = 15.sp)
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            LabeledCircle(T.t(lang, "speak"), Icons.Outlined.Mic, Color(0xFF1B1B1D), TextPrimary) {
                Toast.makeText(context, T.t(lang, "toast_voice_in"), Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.width(8.dp))
            LabeledCircle(T.t(lang, "send"), Icons.AutoMirrored.Outlined.Send, Accent, Color(0xFF141400), filled = true) { send(draft) }
        }
    }

    if (showKeyDialog) GlmConfigDialog(onDismiss = { showKeyDialog = false })
}

@Composable
private fun LabeledCircle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    tint: Color,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(bg)
                .then(if (!filled) Modifier.border(1.dp, Color(0xFF2C2C30), CircleShape) else Modifier)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Approximation of the Axel hologram (the original uses a neon render; CLONE_SPEC §6.1):
 * glow, head, polo collar, torso, particles — drawn on a Canvas with an alpha "breathing" animation.
 */
@Composable
private fun Hologram(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "holo")
    val breath by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "breath",
    )
    val particles = remember { List(26) { Offset(Random(7).nextFloat(), Random(7 + it).nextFloat()) } }

    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF070707))) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val glowR = w * 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(HoloTeal.copy(alpha = 0.16f * breath), Color.Transparent),
                center = Offset(cx, h * 0.52f),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, h * 0.52f),
        )
        val line = Stroke(width = 2.2f, cap = StrokeCap.Round)
        val teal = HoloTeal.copy(alpha = breath)
        val soft = HoloTeal.copy(alpha = 0.5f * breath)
        val headR = w * 0.085f
        val headC = Offset(cx, h * 0.30f)
        // head + hair
        drawCircle(teal, radius = headR, center = headC, style = line)
        drawArc(
            color = soft,
            topLeft = Offset(headC.x - headR, headC.y - headR),
            size = androidx.compose.ui.geometry.Size(headR * 2, headR * 2),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            style = Stroke(width = 5f, cap = StrokeCap.Round),
        )
        // neck
        drawLine(teal, Offset(cx - headR * 0.28f, headC.y + headR), Offset(cx - headR * 0.28f, headC.y + headR * 1.5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        drawLine(teal, Offset(cx + headR * 0.28f, headC.y + headR), Offset(cx + headR * 0.28f, headC.y + headR * 1.5f), strokeWidth = 2.2f, cap = StrokeCap.Round)
        // shoulders and torso (polo)
        val shoulderY = headC.y + headR * 1.7f
        val shoulderW = w * 0.21f
        val torsoBottom = h * 0.88f
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - headR * 0.5f, shoulderY)
                lineTo(cx + headR * 0.5f, shoulderY)
                lineTo(cx + headR * 0.9f, shoulderY + headR * 0.5f)
                quadraticBezierTo(cx + shoulderW, shoulderY + headR * 0.9f, cx + shoulderW * 0.96f, torsoBottom)
                lineTo(cx - shoulderW * 0.96f, torsoBottom)
                quadraticBezierTo(cx - shoulderW, shoulderY + headR * 0.9f, cx - headR * 0.9f, shoulderY + headR * 0.5f)
                close()
            },
            color = soft,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round),
        )
        // polo collar
        drawLine(teal, Offset(cx, shoulderY + headR * 0.1f), Offset(cx - headR * 0.42f, shoulderY + headR * 0.75f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawLine(teal, Offset(cx, shoulderY + headR * 0.1f), Offset(cx + headR * 0.42f, shoulderY + headR * 0.75f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        // particles on the torso
        particles.forEach { p ->
            drawCircle(
                HoloTeal.copy(alpha = 0.25f + 0.45f * breath),
                radius = 1.6f + p.x * 2.2f,
                center = Offset(cx + (p.x - 0.5f) * shoulderW * 1.5f, shoulderY + p.y * (torsoBottom - shoulderY)),
            )
        }
    }
}
