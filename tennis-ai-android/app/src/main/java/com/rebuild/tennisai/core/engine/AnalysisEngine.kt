package com.rebuild.tennisai.core.engine

import com.rebuild.tennisai.core.math.argmax
import com.rebuild.tennisai.core.math.pointSpeed
import com.rebuild.tennisai.core.metrics.MetricDefs
import com.rebuild.tennisai.core.metrics.MetricsCalculator
import com.rebuild.tennisai.core.model.AnalysisResult
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lm
import com.rebuild.tennisai.core.model.Lang
import com.rebuild.tennisai.core.model.MetricResult
import com.rebuild.tennisai.core.model.MetricStatus
import com.rebuild.tennisai.core.model.Moment
import com.rebuild.tennisai.core.model.MomentResult
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.core.phases.MomentBoundaries
import com.rebuild.tennisai.core.phases.detectorFor

/**
 * Пайплайн офлайн-анализа: позы → фазы → метрики → скоринг.
 * Структура и пороги — клон оригинала v2.0.19 (GAP_ANALYSIS.md).
 */
object AnalysisEngine {

    fun analyze(
        frames: List<PoseFrame>,
        stroke: StrokeType,
        handedness: Handedness,
        lang: Lang = Lang.RU,
    ): AnalysisResult {
        val bounds = detectorFor(stroke).detect(frames, handedness)
        val contactIdx = findContactIdx(frames, handedness, bounds)

        val layout = MetricDefs.layout(stroke)
        val momentResults = Moment.entries.mapIndexed { i, m ->
            val range = bounds[i]
            val phaseFrames = frames.filterIndexed { idx, _ -> idx in range }
            val metrics = layout[m].orEmpty().mapNotNull { (def, _) ->
                val c = MetricsCalculator.compute(def.id, phaseFrames, handedness, frames, contactIdx)

                // Классификация (стойка) — информационная метрика без скоринга.
                if (c.classification != null) {
                    return@mapNotNull MetricResult(
                        id = def.id, value = 0f, proMin = 0f, proMax = 360f, kind = def.kind,
                        status = MetricStatus.NA, classification = c.classification, proNote = def.proNote,
                    )
                }
                val value = c.value ?: return@mapNotNull null
                val nd = c.valueNd
                val inRange = value in def.proMin..def.proMax &&
                    (def.ndMin == null || def.ndMax == null || nd == null ||
                        (nd >= def.ndMin && nd <= def.ndMax))
                val status =
                    // Метрики, требующие детекции ракетки/мяча, отсутствуют в этой сборке.
                    if (def.id == MetricDefs.M5_RACKET_HEAD_POSITION.id || def.id == MetricDefs.RACQUET_PATH.id) MetricStatus.NA
                    else if (inRange) MetricStatus.GREEN else MetricStatus.RED
                MetricResult(
                    id = def.id,
                    value = value,
                    proMin = def.proMin,
                    proMax = def.proMax,
                    kind = def.kind,
                    status = status,
                    correction = if (status == MetricStatus.RED) def.correction(lang, value) else null,
                    valueNd = nd,
                    proMinNd = def.ndMin,
                    proMaxNd = def.ndMax,
                    proNote = def.proNote,
                )
            }
            MomentResult(m, range.first, range.last + 1, metrics)
        }

        // Взвешенный общий балл (веса фаз/метрик — наша калибровка; веса оригинала
        // хранились в Firestore metric_definitions и не восстановлены).
        var num = 0f
        var den = 0f
        momentResults.forEach { mr ->
            val weights = layout[mr.moment].orEmpty()
            mr.metrics.forEachIndexed { mi, met ->
                val w = weights.getOrNull(mi)?.second ?: 1f
                if (met.status != MetricStatus.NA) {
                    num += if (met.status == MetricStatus.GREEN) w else 0f
                    den += w
                }
            }
        }
        val overall = if (den > 0f) (100f * num / den).toInt() else 0

        return AnalysisResult(
            stroke = stroke,
            handedness = handedness,
            moments = momentResults,
            overallScore = overall,
            frameCount = frames.size,
            durationMs = if (frames.isEmpty()) 0 else frames.last().tMillis - frames.first().tMillis,
        )
    }

    /** Кадр контакта: пик скорости доминантного запястья в окне M4–M5. */
    fun findContactIdx(frames: List<PoseFrame>, h: Handedness, bounds: MomentBoundaries): Int {
        if (frames.isEmpty()) return 0
        val wrist = { f: PoseFrame -> f.p(if (h == Handedness.RIGHT) Lm.RIGHT_WRIST else Lm.LEFT_WRIST) }
        val speed = pointSpeed(frames) { wrist(it) }
        val from = bounds[4].first.coerceIn(0, frames.size - 1)
        val to = (bounds[5].last + 1).coerceAtMost(frames.size)
        return argmax(speed, from, to.coerceAtLeast(from + 1))
    }
}
