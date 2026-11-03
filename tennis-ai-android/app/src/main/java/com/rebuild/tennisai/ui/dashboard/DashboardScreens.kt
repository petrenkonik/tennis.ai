package com.rebuild.tennisai.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.TrackChanges
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.core.i18n.T
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.ui.common.AppCard
import com.rebuild.tennisai.ui.common.AxelFab
import com.rebuild.tennisai.ui.common.GlowText
import com.rebuild.tennisai.ui.common.MonoCaption
import com.rebuild.tennisai.ui.common.SectionBar
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.Bar
import com.rebuild.tennisai.ui.theme.CardAccentBg
import com.rebuild.tennisai.ui.theme.CardAccentBorder
import com.rebuild.tennisai.ui.theme.Hairline
import com.rebuild.tennisai.ui.theme.MonoLabel
import com.rebuild.tennisai.ui.theme.SerifDisplay
import com.rebuild.tennisai.ui.theme.TextDim
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary

/** SELECT DASHBOARD (CLONE_SPEC §2.3) — без bottom nav, серифный заголовок со свечением. */
@Composable
fun SelectDashboardScreen(lang: Lang, onPlayer: () -> Unit, onCoach: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))
        Icon(Icons.Outlined.GridView, null, tint = Accent, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(22.dp))
        GlowText(T.t(lang, "select_dashboard"), style = SerifDisplay.copy(fontSize = 27.sp))
        Spacer(Modifier.height(44.dp))
        DashboardChoice(
            icon = Icons.Outlined.Person,
            title = T.t(lang, "player_dashboard"),
            subtitle = T.t(lang, "track_personal_progress"),
            onClick = onPlayer,
        )
        Spacer(Modifier.height(18.dp))
        DashboardChoice(
            icon = Icons.Outlined.Person,
            title = T.t(lang, "coach_dashboard"),
            subtitle = "manage_students_perf", // невыровненный ключ оригинала — дословно
            onClick = onCoach,
        )
        Spacer(Modifier.weight(1f))
        Text(
            T.t(lang, "back"),
            style = MonoLabel.copy(fontSize = 14.sp),
            color = TextSecondary,
            modifier = Modifier.clickable { onBack() }.padding(16.dp),
        )
        Spacer(Modifier.weight(0.7f))
    }
}

@Composable
private fun DashboardChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), radius = 20.dp, onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF17220D)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MonoLabel.copy(fontSize = 15.sp), color = Accent)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 14.sp, lineHeight = 18.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}

/** Общий каркас дашбордов (CLONE_SPEC §2.4/§2.5). */
@Composable
private fun DashboardScaffold(
    topIcon: ImageVector,
    topTitle: String,
    title: String,
    subtitle: String,
    fab: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050505)).statusBarsPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 90.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(54.dp).background(Bar)) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(topIcon, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(topTitle, style = MonoLabel.copy(fontSize = 16.sp), color = TextPrimary)
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
            Text(title, color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(22.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
        fab()
    }
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    AppCard(modifier.fillMaxWidth().height(150.dp)) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (mono) MonoCaption(label, TextSecondary, fontSize = 12) else Text(label, color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReferralBanner(lang: Lang, text: String, onClick: () -> Unit) {
    AppCard(
        Modifier.fillMaxWidth(),
        background = CardAccentBg,
        borderColor = CardAccentBorder,
        radius = 18.dp,
        onClick = onClick,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Redeem, null, tint = Accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(T.t(lang, "get_1_year_free"), color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyActivity(icon: ImageVector, title: String, subtitle: String) {
    AppCard(Modifier.fillMaxWidth().height(140.dp)) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, tint = TextDim, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, color = TextSecondary, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = TextDim, fontSize = 13.sp)
        }
    }
}

/** My Dashboard — player (CLONE_SPEC §2.4). */
@Composable
fun PlayerDashboardScreen(state: AppState, onAxel: () -> Unit) {
    val l = state.lang
    DashboardScaffold(
        topIcon = Icons.Outlined.Public,
        topTitle = "AXEL AI",
        title = T.t(l, "my_dashboard"),
        subtitle = "track_progression", // невыровненный ключ оригинала — дословно
        fab = { AxelFab(onClick = onAxel, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCell(Icons.Outlined.TrackChanges, "${state.sessions}", T.t(l, "my_sessions"), modifier = Modifier.weight(1f))
            StatCell(
                Icons.Outlined.BarChart,
                state.result?.let { "${it.overallScore}" } ?: "—",
                T.t(l, "skill_analysis"),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCell(Icons.Outlined.Description, "${state.plans}", T.t(l, "training_plans"), mono = true, modifier = Modifier.weight(1f))
            StatCell(Icons.Outlined.ChatBubbleOutline, "${state.messages}", T.t(l, "messages"), mono = true, modifier = Modifier.weight(1f))
        }
        ReferralBanner(l, T.t(l, "invite_10_friends")) {}
        Spacer(Modifier.height(8.dp))
        SectionBar(T.t(l, "recent_activity"))
        EmptyActivity(Icons.Outlined.Videocam, T.t(l, "no_sessions_yet"), "record_first_video")
    }
}

/** Coach Dashboard (CLONE_SPEC §2.5). */
@Composable
fun CoachDashboardScreen(state: AppState, onAxel: () -> Unit) {
    val l = state.lang
    DashboardScaffold(
        topIcon = Icons.Outlined.Person,
        topTitle = T.t(l, "coach_dashboard"),
        title = T.t(l, "welcome_coach"),
        subtitle = T.t(l, "student_overview"),
        fab = { AxelFab(onClick = onAxel, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCell(Icons.Outlined.Person, "${state.students}", T.t(l, "my_students"), modifier = Modifier.weight(1f))
            StatCell(Icons.Outlined.BarChart, "—", T.t(l, "analysis"), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCell(Icons.Outlined.Description, "${state.plans}", T.t(l, "training_plans"), mono = true, modifier = Modifier.weight(1f))
            StatCell(Icons.Outlined.ChatBubbleOutline, "—", T.t(l, "messages"), mono = true, modifier = Modifier.weight(1f))
        }
        ReferralBanner(l, T.t(l, "add_students_free")) {}
        Spacer(Modifier.height(8.dp))
        SectionBar(T.t(l, "recent_activity"))
        EmptyActivity(Icons.Outlined.Person, T.t(l, "no_student_activity"), T.t(l, "add_students_to_see"))
    }
}
