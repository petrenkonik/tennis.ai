package com.rebuild.tennisai.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
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
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.ui.common.AppCard
import com.rebuild.tennisai.ui.common.AxelFab
import com.rebuild.tennisai.ui.common.MonoCaption
import com.rebuild.tennisai.ui.common.PillBadge
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.BackhandAmber
import com.rebuild.tennisai.ui.theme.Bar
import com.rebuild.tennisai.ui.theme.DrillsRed
import com.rebuild.tennisai.ui.theme.Hairline
import com.rebuild.tennisai.ui.theme.MonoLabel
import com.rebuild.tennisai.ui.theme.ServeBlue
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary

private data class Bucket(val titleKey: String, val color: Color, val stroke: StrokeType?) {
    fun title(lang: Lang) = T.t(lang, titleKey)
}

private val BUCKETS = listOf(
    Bucket("serve", ServeBlue, StrokeType.SERVE),
    Bucket("forehand", Accent, StrokeType.FOREHAND),
    Bucket("backhand", BackhandAmber, StrokeType.BACKHAND),
    Bucket("drills", DrillsRed, null),
)

/** Общий каркас Analyses/Videos (CLONE_SPEC §2.8/§2.9). */
@Composable
private fun LibraryScaffold(
    topIcon: ImageVector,
    topTitle: String,
    onAxel: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050505)).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 90.dp)) {
            Box(Modifier.fillMaxWidth().height(54.dp).background(Bar)) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(topIcon, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(topTitle, style = MonoLabel.copy(fontSize = 16.sp), color = Accent)
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline),
                )
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
        AxelFab(onClick = onAxel, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp))
    }
}

@Composable
private fun StatsRow(lang: Lang, total: Int, completed: Int, analyzing: Int) {
    AppCard(Modifier.fillMaxWidth(), radius = 18.dp) {
        Row(Modifier.padding(vertical = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatColumn("$total", T.t(lang, "total"), TextPrimary, Modifier.weight(1f), mono = true)
            StatColumn("$completed", T.t(lang, "completed"), Accent, Modifier.weight(1f))
            StatColumn("$analyzing", T.t(lang, "analyzing"), BackhandAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color, modifier: Modifier = Modifier, mono: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (mono) MonoCaption(label, TextSecondary, fontSize = 13) else Text(label, color = TextSecondary, fontSize = 14.sp)
    }
}

/** MY ANALYSES (CLONE_SPEC §2.8). */
@Composable
fun AnalysesScreen(state: AppState, onAxel: () -> Unit, onOpen: (StrokeType) -> Unit = {}) {
    val l = state.lang
    LibraryScaffold(topIcon = Icons.Outlined.MonitorHeart, topTitle = T.t(l, "my_analyses"), onAxel = onAxel) {
        StatsRow(l, state.sessions, state.sessions, 0)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BucketCard(BUCKETS[0], l, Modifier.weight(1f), analyses = true)
            BucketCard(BUCKETS[1], l, Modifier.weight(1f), analyses = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BucketCard(BUCKETS[2], l, Modifier.weight(1f), analyses = true)
            BucketCard(BUCKETS[3], l, Modifier.weight(1f), analyses = true)
        }
    }
}

/** MY VIDEOS (CLONE_SPEC §2.9). */
@Composable
fun VideosScreen(state: AppState, onAxel: () -> Unit, onOpen: (StrokeType) -> Unit = {}) {
    val l = state.lang
    LibraryScaffold(topIcon = Icons.Outlined.Videocam, topTitle = T.t(l, "my_videos"), onAxel = onAxel) {
        StatsRow(l, 0, 0, 0)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BucketCard(BUCKETS[0], l, Modifier.weight(1f), analyses = false)
            BucketCard(BUCKETS[1], l, Modifier.weight(1f), analyses = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BucketCard(BUCKETS[2], l, Modifier.weight(1f), analyses = false)
            BucketCard(BUCKETS[3], l, Modifier.weight(1f), analyses = false)
        }
    }
}

@Composable
private fun BucketCard(bucket: Bucket, lang: Lang, modifier: Modifier = Modifier, analyses: Boolean) {
    AppCard(modifier.fillMaxWidth(), borderColor = bucket.color.copy(alpha = 0.45f), radius = 18.dp) {
        Column(Modifier.padding(12.dp)) {
            // Внутренняя плитка: цветная рамка, иконка + no_data / VIDEOS
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bucket.color.copy(alpha = 0.10f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (analyses) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.MonitorHeart, null, tint = bucket.color, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(8.dp))
                        MonoCaption(T.t(lang, "no_data"), bucket.color.copy(alpha = 0.75f), fontSize = 12)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Videocam, null, tint = bucket.color, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("0", color = bucket.color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        MonoCaption(T.t(lang, "videos_upper"), bucket.color.copy(alpha = 0.75f), fontSize = 11)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(bucket.title(lang), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (analyses) T.sessions(0, lang) else T.t(lang, "no_videos_yet"),
                color = TextSecondary,
                fontSize = 13.sp,
            )
            if (analyses) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillBadge(Icons.Outlined.Check, "0", Accent)
                    PillBadge(Icons.Outlined.Schedule, "0", BackhandAmber)
                }
            }
        }
    }
}
