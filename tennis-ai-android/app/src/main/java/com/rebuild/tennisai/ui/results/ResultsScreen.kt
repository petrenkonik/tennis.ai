package com.rebuild.tennisai.ui.results

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.core.model.MetricStatus
import com.rebuild.tennisai.core.model.Moment
import com.rebuild.tennisai.ui.theme.GreenGood
import com.rebuild.tennisai.ui.theme.RedFault

@Composable
fun ResultsScreen(state: AppState, onAgain: () -> Unit) {
    val result = state.result ?: run {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(if (state.lang == Lang.RU) "Нет результата анализа" else "No analysis result")
            TextButton(onClick = onAgain) { Text(if (state.lang == Lang.RU) "Назад" else "Back") }
        }
        return
    }
    val ru = state.lang == Lang.RU
    var selected by remember { mutableIntStateOf(4) } // по умолчанию фаза контакта
    val prefs = LocalContext.current.getSharedPreferences("profile", android.content.Context.MODE_PRIVATE)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Балл /100 (в оригинале: «a score out of 100, never a percentage»)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(76.dp).background(scoreColor(result.overallScore), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${result.overallScore}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101408))
                        Text("/100", fontSize = 11.sp, color = Color(0xFF2A331A))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (ru) "АНАЛИЗ ЗАВЕРШЁН" else "ANALYSIS COMPLETE", style = MaterialTheme.typography.titleLarge)
                    val reds = result.redMetrics.size
                    Text(
                        // Правило оригинала: красный — вне диапазона лучших, зелёный — внутри
                        if (ru) "Красных метрик: $reds. Красный — вне диапазона лучших игроков, зелёный — внутри."
                        else "Red metrics: $reds. Red means outside the range the best players sit in, green means inside it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Награда оригинала: «You earned 150 XP and unlocked a new badge.»
                    Text(
                        "+150 XP 🏅",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        // Фазы
        Text(if (ru) "ФАЗЫ УДАРА" else "STROKE PHASES", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Moment.entries.forEachIndexed { i, m ->
                FilterChip(selected = selected == i, onClick = { selected = i }, label = { Text(m.key, fontSize = 12.sp) })
            }
        }

        val moment = result.moments[selected]
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${moment.moment.key} · ${moment.moment.title(result.stroke, state.lang)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                // «WHAT THIS MOMENT IS FOR»
                Text(
                    moment.moment.hint(result.stroke, state.lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (moment.metrics.isEmpty()) {
                    Text(if (ru) "Метрики фазы недоступны для этого клипа." else "No metrics available for this clip.")
                }
                moment.metrics.forEach { met ->
                    val def = com.rebuild.tennisai.core.metrics.MetricDefs.ALL[met.id]
                    MetricRow(met = met, name = def?.name(state.lang) ?: met.id, lang = state.lang)
                    met.proNote?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (met.status == MetricStatus.RED && met.correction != null) {
                        Card(
                            Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RedFault.copy(alpha = 0.15f)),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("⚡ " + met.correction.title, fontWeight = FontWeight.SemiBold, color = RedFault)
                                Spacer(Modifier.height(2.dp))
                                Text(met.correction.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        androidx.compose.material3.Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) {
            Text(if (ru) "Разобрать ещё один удар" else "Analyze another stroke")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MetricRow(met: com.rebuild.tennisai.core.model.MetricResult, name: String, lang: Lang) {
    val ru = lang == Lang.RU
    val dotColor = when (met.status) {
        MetricStatus.GREEN -> GreenGood
        MetricStatus.RED -> RedFault
        MetricStatus.NA -> Color.Gray
    }
    val def = com.rebuild.tennisai.core.metrics.MetricDefs.ALL[met.id]
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(Modifier.size(10.dp).background(dotColor, RoundedCornerShape(5.dp)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(met.id, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
        }
        when {
            met.classification != null -> Text(met.classification, fontWeight = FontWeight.SemiBold)
            else -> {
                val main = def?.fmt?.format(met.value)?.plus(def.kind.unit(lang)) ?: "%.1f".format(met.value)
                val nd = if (met.valueNd != null && def != null) {
                    " / ND ${def.fmt.format(met.valueNd)}${def.kind.unit(lang)}"
                } else ""
                Text("$main$nd", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        val range = if (met.id == com.rebuild.tennisai.core.metrics.MetricDefs.STANCE_TYPE.id) ""
        else "${def?.fmt?.format(met.proMin)}–${def?.fmt?.format(met.proMax)}${def?.kind?.unit(lang) ?: ""}"
        Text(range, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> GreenGood
    score >= 60 -> Color(0xFFD4FF3F)
    score >= 40 -> Color(0xFFFFB84D)
    else -> RedFault
}
