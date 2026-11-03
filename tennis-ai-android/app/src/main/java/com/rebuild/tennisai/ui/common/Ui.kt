package com.rebuild.tennisai.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rebuild.tennisai.ui.theme.Accent
import com.rebuild.tennisai.ui.theme.AccentDeep
import com.rebuild.tennisai.ui.theme.Bar
import com.rebuild.tennisai.ui.theme.CardBg
import com.rebuild.tennisai.ui.theme.CardBorder
import com.rebuild.tennisai.ui.theme.Hairline
import com.rebuild.tennisai.ui.theme.MonoSmall
import com.rebuild.tennisai.ui.theme.TextDim
import com.rebuild.tennisai.ui.theme.TextPrimary
import com.rebuild.tennisai.ui.theme.TextSecondary

/** Текст с неоновым свечением (заголовки «AXEL AI», «SELECT DASHBOARD»). */
@Composable
fun GlowText(
    text: String,
    color: Color = Accent,
    style: TextStyle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = 1.5.sp,
    ),
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier = modifier,
        style = style.copy(
            shadow = Shadow(color = color.copy(alpha = 0.85f), blurRadius = 22f),
        ),
        color = color,
        textAlign = TextAlign.Center,
    )
}

/** Секционный заголовок: зелёная черта + белый bold («Recent Activity», «Settings»). */
@Composable
fun SectionBar(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Карточка клона: тёмная заливка + тонкая граница + радиус. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    background: Color = CardBg,
    borderColor: Color = CardBorder,
    radius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        content = content,
    )
}

/** Цветная иконка-плитка (модули Home, карточки Analyses/Videos, Ambassador). */
@Composable
fun IconTile(
    icon: ImageVector,
    color: Color,
    size: Dp = 52.dp,
    radius: Dp = 14.dp,
    filled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(if (filled) color.copy(alpha = 0.14f) else Color.Transparent)
            .border(BorderStroke(1.dp, color.copy(alpha = if (filled) 0.35f else 0.0f)), RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

/** Кольцо процента (модули Home): серый трек + цветная дуга + «0%» в центре. */
@Composable
fun RingPercent(percent: Int, color: Color, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color(0xFF2E2E32), style = stroke)
            if (percent > 0) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * percent / 100,
                    useCenter = false,
                    style = stroke,
                )
            }
        }
        Text("$percent%", color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** FAB Axel: круг с зелёной границей и «sparkles». */
@Composable
fun AxelFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C0F08))
            .border(BorderStroke(1.5.dp, Accent.copy(alpha = 0.7f)), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.AutoAwesome, null, tint = Accent, modifier = Modifier.size(24.dp))
    }
}

/** Pill-бейдж («✓ 0», «🕐 0») — тонкая цветная граница. */
@Composable
fun PillBadge(icon: ImageVector, label: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.55f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Строка настроек с тумблером. */
@Composable
fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 16.dp) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentDeep,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF3A3A3E),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

private enum class Tab(val route: String, val icon: ImageVector) {
    HOME("home", Icons.Outlined.Home),
    VIDEOS("videos", Icons.Outlined.Videocam),
    ANALYSES("analyses", Icons.AutoMirrored.Outlined.ShowChart),
    DASHBOARD("dashboard", Icons.Outlined.GridView),
    PROFILE("profile", Icons.Outlined.Person),
}

/** Нижняя навигация: 5 табов, активный — зелёный с чертой у верхнего края. */
@Composable
fun BottomNavBar(currentRoute: String?, onSelect: (String) -> Unit) {
    val current = when {
        currentRoute == "dashboard_player" || currentRoute == "dashboard_coach" -> "dashboard"
        else -> currentRoute
    }
    Surface(color = Bar, border = BorderStroke(0.dp, Color.Transparent)) {
        Column(Modifier.background(Bar)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hairline),
            )
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { tab ->
                    val active = current == tab.route
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (active) {
                            Box(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .width(26.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Accent),
                            )
                        }
                        Icon(
                            tab.icon,
                            contentDescription = tab.name,
                            tint = if (active) Accent else TextDim,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onSelect(tab.route) },
                        )
                    }
                }
            }
        }
    }
}

/** Подпись-«шпонка» monospace (METKA, no_data, VIDEOS…). */
@Composable
fun MonoCaption(text: String, color: Color = TextSecondary, modifier: Modifier = Modifier, fontSize: Int = 12) {
    Text(
        text,
        modifier = modifier,
        style = MonoSmall.copy(fontSize = fontSize.sp, letterSpacing = (fontSize * 0.2f).sp),
        color = color,
    )
}

@Composable
fun VerticalCenteredColumn(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
}
