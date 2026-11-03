package com.rebuild.tennisai.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.core.i18n.T
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.ui.common.AppCard
import com.rebuild.tennisai.ui.common.AxelFab
import com.rebuild.tennisai.ui.common.GlowText
import com.rebuild.tennisai.ui.common.IconTile
import com.rebuild.tennisai.ui.common.MonoCaption
import com.rebuild.tennisai.ui.common.RingPercent
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.BackhandAmber
import com.rebuild.tennisai.ui.theme.Bar
import com.rebuild.tennisai.ui.theme.CoachOrange
import com.rebuild.tennisai.ui.theme.DrillsRed
import com.rebuild.tennisai.ui.theme.Hairline
import com.rebuild.tennisai.ui.theme.MonoLabel
import com.rebuild.tennisai.ui.theme.ServeBlue
import com.rebuild.tennisai.ui.theme.SerifDisplay
import com.rebuild.tennisai.ui.theme.TextDim
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary

private data class Module(
    val titleKey: String,
    val subtitleKey: String,
    val icon: ImageVector,
    val color: Color,
    val tagKey: String?,
    val stroke: StrokeType?,
)

@Composable
fun HomeScreen(
    state: AppState,
    onLive: () -> Unit,
    onAnalyze: (StrokeType?) -> Unit,
    onAxel: () -> Unit,
) {
    val lang = state.lang
    val modules = listOf(
        Module("coach_mode", "realtime_for_coaches", Icons.Outlined.AssignmentInd, CoachOrange, "coach", null),
        Module("auto_detect", "auto_detect_desc", Icons.Outlined.CenterFocusWeak, Accent, "new", null),
        Module("serve_analysis", "full_motion_posture", Icons.Outlined.Videocam, ServeBlue, null, StrokeType.SERVE),
        Module("forehand", "rotation_extension", Icons.Outlined.PlayArrow, Accent, null, StrokeType.FOREHAND),
        Module("backhand", "handed_technique", Icons.Outlined.PlayArrow, BackhandAmber, null, StrokeType.BACKHAND),
        Module("ai_drills", "personalized_exercises", Icons.Outlined.FitnessCenter, DrillsRed, null, null),
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Топ-бар: chevron (декоративный) + serif AXEL AI, фон Bar + волосяная линия (как в оригинале)
            Column(Modifier.fillMaxWidth().background(Bar)) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier.align(Alignment.CenterStart).size(26.dp),
                    )
                    GlowText(
                        "AXEL AI",
                        modifier = Modifier.align(Alignment.Center),
                        style = SerifDisplay.copy(fontSize = 24.sp),
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }

            // REAL-TIME ANALYSIS
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Accent.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .clickable { onLive() },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Sensors, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(T.t(lang, "realtime_analysis"), style = MonoLabel.copy(fontSize = 15.sp), color = Accent)
                }
            }

            // TRAINING MODULES
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoCaption(T.t(lang, "training_modules"), TextSecondary, fontSize = 13)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(1.dp).background(Hairline))
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                modules.forEach { m ->
                    ModuleRow(m, lang) {
                        when {
                            m.titleKey == "coach_mode" -> onLive()
                            m.titleKey == "ai_drills" -> onAxel()
                            else -> onAnalyze(m.stroke)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        AxelFab(
            onClick = onAxel,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp),
        )
    }
}

@Composable
private fun ModuleRow(m: Module, lang: com.rebuild.tennisai.core.model.Lang, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(m.icon, m.color)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(T.t(lang, m.titleKey), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(T.t(lang, m.subtitleKey), color = TextSecondary, fontSize = 13.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.width(10.dp))
                RingPercent(0, m.color, Modifier.size(44.dp))
            }
            Spacer(Modifier.height(12.dp))
            // В оригинале линия-прогресс ~84% ширины, метка — ПОД ней, справа
            Box(
                Modifier
                    .fillMaxWidth(0.84f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2A2E)),
            )
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    if (m.tagKey != null) T.t(lang, m.tagKey) else T.sessions(0, lang),
                    style = MonoLabel.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
                    color = TextDim,
                )
            }
        }
    }
}
