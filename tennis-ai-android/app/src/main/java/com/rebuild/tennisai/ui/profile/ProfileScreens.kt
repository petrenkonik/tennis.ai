package com.rebuild.tennisai.ui.profile

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.Experience
import com.rebuild.tennisai.MainGoal
import com.rebuild.tennisai.SkillLevel
import com.rebuild.tennisai.core.i18n.T
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.ui.axel.GlmConfigDialog
import com.rebuild.tennisai.ui.common.AppCard
import com.rebuild.tennisai.ui.common.AxelFab
import com.rebuild.tennisai.ui.common.GlowText
import com.rebuild.tennisai.ui.common.SectionBar
import com.rebuild.tennisai.ui.common.ToggleRow
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.Bar
import com.rebuild.tennisai.ui.theme.CardAccentBg
import com.rebuild.tennisai.ui.theme.CardAccentBorder
import com.rebuild.tennisai.ui.theme.CardBg
import com.rebuild.tennisai.ui.theme.CardBorder
import com.rebuild.tennisai.ui.theme.Hairline
import com.rebuild.tennisai.ui.theme.MonoLabel
import com.rebuild.tennisai.ui.theme.SerifDisplay
import com.rebuild.tennisai.ui.theme.TextDim
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary

private val OnAccent = Color(0xFF141400)

/** My Profile (CLONE_SPEC §2.6) — tab 5. */
@Composable
fun ProfileScreen(state: AppState, onEdit: () -> Unit, onAxel: () -> Unit) {
    val l = state.lang
    var showGlm by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050505)).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 90.dp)) {
            // Top bar
            Box(Modifier.fillMaxWidth().height(54.dp).background(Bar)) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(26.dp),
                )
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null, tint = Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    GlowText(T.t(l, "my_profile"), style = SerifDisplay.copy(fontSize = 22.sp, letterSpacing = 0.5.sp))
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline),
                )
            }

            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Ambassador Program
                AppCard(
                    Modifier.fillMaxWidth(),
                    background = CardAccentBg,
                    borderColor = CardAccentBorder,
                    radius = 20.dp,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Person, null, tint = Accent, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(T.t(l, "ambassador_program"), color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(T.t(l, "ambassador_subtitle"), color = TextSecondary, fontSize = 14.sp)
                        }
                        Text("€0", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2×2 info chips (values from the profile — as in the original)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoChip(Icons.Outlined.BarChart, "${T.t(l, "level")}: ${T.skillLabel(state.skill, l)}", Modifier.weight(1f))
                    InfoChip(Icons.Outlined.Schedule, T.expShort(state.experience, l), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoChip(Icons.Outlined.TrackChanges, "${T.t(l, "goal")}: ${T.goalChip(state.goal, l)}", Modifier.weight(1f))
                    InfoChip(Icons.AutoMirrored.Outlined.TrendingUp, "${T.t(l, "progress")}: ${state.progress}%", Modifier.weight(1f))
                }

                // Free Plan
                AppCard(Modifier.fillMaxWidth(), radius = 20.dp, onClick = onEdit) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎾", fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                T.t(l, "free_plan"),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF2E2E32))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(T.t(l, "free"), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(T.t(l, "free_desc"), color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionBar(T.t(l, "settings"), Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleRow(Icons.Outlined.Notifications, T.t(l, "push_notifications"), state.pushOn) {
                    state.pushOn = it; state.saveSettings()
                }
                ToggleRow(Icons.Outlined.Memory, T.t(l, "ai_coaching_tips"), state.aiTipsOn) {
                    state.aiTipsOn = it; state.saveSettings()
                }
                ToggleRow(Icons.Outlined.Email, T.t(l, "email_notifications"), state.emailOn) {
                    state.emailOn = it; state.saveSettings()
                }
                // Language (the "language" key from the original's en.json)
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 16.dp, onClick = { showLang = true }) {
                    Row(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Language, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(T.t(l, "language"), color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(if (l == Lang.RU) "Русский" else "English", color = TextSecondary, fontSize = 14.sp)
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 16.dp, onClick = { showGlm = true }) {
                    Row(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(T.t(l, "app_version"), color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        AxelFab(onClick = onAxel, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp))
    }

    if (showGlm) GlmConfigDialog(onDismiss = { showGlm = false })
    if (showLang) {
        LanguageDialog(current = l, onSelect = { state.lang = it; state.saveSettings(); showLang = false }, onDismiss = { showLang = false })
    }
}

/** Language selection: RU (default) / EN. */
@Composable
private fun LanguageDialog(current: Lang, onSelect: (Lang) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.t(current, "language")) },
        text = {
            Column {
                Lang.entries.forEach { lang ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(lang) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = lang == current,
                            onClick = { onSelect(lang) },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = TextSecondary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (lang) {
                                Lang.RU -> "Русский"
                                Lang.EN -> "English"
                            },
                            color = TextPrimary,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(T.t(current, "back")) } },
    )
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth(), radius = 16.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
        }
    }
}

/** Edit Profile (CLONE_SPEC §2.7): General|Additional segment, chips, Save. */
@Composable
fun EditProfileScreen(state: AppState, onBack: () -> Unit) {
    val l = state.lang
    var tab by remember { mutableStateOf(1) } // 0 = General, 1 = Additional (as on the screenshot)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        // Header: back · Edit Profile · Save
        Box(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                null,
                tint = TextSecondary,
                modifier = Modifier.align(Alignment.CenterStart).size(26.dp).clickable { onBack() },
            )
            Text(
                T.t(l, "edit_profile"),
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp),
            )
            Text(
                T.t(l, "save"),
                color = Accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable {
                        state.saveProfile()
                        onBack()
                    }
                    .padding(8.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        // General | Additional segment
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(5.dp),
        ) {
            Row(Modifier.fillMaxSize()) {
                SegmentTab(T.t(l, "general"), tab == 0, Modifier.weight(1f)) { tab = 0 }
                SegmentTab(T.t(l, "additional"), tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
        }

        Spacer(Modifier.height(18.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (tab == 0) {
                // General — recovered from logic, not shown in the screenshots (CLONE_SPEC §6.3)
                FieldRow {
                    BasicTextField(
                        value = state.name,
                        onValueChange = { state.name = it },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                        cursorBrush = Brush.verticalGradient(listOf(Accent, Accent)),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (state.name.isEmpty()) Text(T.t(l, "name"), color = TextDim, fontSize = 16.sp)
                            inner()
                        },
                    )
                }
                FieldRow {
                    NumberField(value = state.heightCm, placeholder = T.t(l, "height_cm"), onChange = { state.heightCm = it })
                }
                FieldRow {
                    NumberField(value = state.age, placeholder = T.t(l, "age"), onChange = { state.age = it })
                }
            } else {
                // Additional — 1:1 per the screenshot
                FieldRow {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Scale, null, tint = Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        BasicTextField(
                            value = state.weightKg,
                            onValueChange = { state.weightKg = it.filter(Char::isDigit).take(3) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                            cursorBrush = Brush.verticalGradient(listOf(Accent, Accent)),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.weightKg.isEmpty()) Text(T.t(l, "weight_placeholder"), color = TextDim, fontSize = 16.sp)
                                    inner()
                                    if (state.weightKg.isNotEmpty()) Text(" ${T.t(l, "weight_placeholder").substringAfter(" ")}", color = TextSecondary, fontSize = 15.sp)
                                }
                            },
                        )
                    }
                }

                ChipSection(T.t(l, "skill_level"))
                FlowChips(
                    labels = SkillLevel.entries.map { T.skillLabel(it, l) },
                    selectedIndex = SkillLevel.entries.indexOf(state.skill),
                ) { idx -> state.skill = SkillLevel.entries[idx] }

                ChipSection(T.t(l, "years_experience"))
                FlowChips(
                    labels = Experience.entries.map { T.expLabel(it, l) },
                    selectedIndex = Experience.entries.indexOf(state.experience),
                ) { idx -> state.experience = Experience.entries[idx] }

                ChipSection(T.t(l, "dominant_hand"))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectionChip(T.handLabel(Handedness.RIGHT, l), state.hand == Handedness.RIGHT, Icons.Outlined.PanTool, Modifier.weight(1f)) {
                        state.hand = Handedness.RIGHT
                    }
                    SelectionChip(T.handLabel(Handedness.LEFT, l), state.hand == Handedness.LEFT, Icons.Outlined.PanTool, Modifier.weight(1f)) {
                        state.hand = Handedness.LEFT
                    }
                }

                ChipSection(T.t(l, "main_goal"))
                FlowChips(
                    labels = MainGoal.entries.map { T.goalChip(it, l) },
                    selectedIndex = MainGoal.entries.indexOf(state.goal),
                    rowSizes = listOf(2, 3),
                ) { idx -> state.goal = MainGoal.entries[idx] }
            }
        }
    }
}

@Composable
private fun NumberField(value: String, placeholder: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
        cursorBrush = Brush.verticalGradient(listOf(Accent, Accent)),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isEmpty()) Text(placeholder, color = TextDim, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
private fun SegmentTab(title: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxSize().clickable { onClick() }, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title,
                color = if (active) OnAccent else TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FieldRow(content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), radius = 16.dp) {
        Box(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}

@Composable
private fun ChipSection(title: String) {
    Text(
        title,
        style = MonoLabel.copy(fontSize = 13.sp, letterSpacing = 2.sp),
        color = TextSecondary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** Wrapping chips sized to content (as on the screenshot: MAIN GOAL = 2+3). */
@Composable
private fun FlowChips(
    labels: List<String>,
    selectedIndex: Int,
    rowSizes: List<Int>? = null,
    onSelectIndex: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = rowSizes?.let { sizes ->
            var idx = 0
            sizes.map { n -> labels.indices.drop(idx).take(n).toList().also { idx += n } }.filter { it.isNotEmpty() }
        } ?: labels.indices.chunked(2)
        rows.forEach { rowIndices ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowIndices.forEach { i ->
                    SelectionChip(labels[i], i == selectedIndex, null) { onSelectIndex(i) }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (selected) Accent else CardBg)
            .then(if (selected) Modifier else Modifier.border(1.dp, CardBorder, RoundedCornerShape(26.dp)))
            .clickable { onClick() }
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) OnAccent else TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                label,
                color = if (selected) OnAccent else TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}
