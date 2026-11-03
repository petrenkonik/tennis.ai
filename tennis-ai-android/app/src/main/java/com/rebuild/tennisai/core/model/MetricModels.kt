package com.rebuild.tennisai.core.model

enum class MetricStatus { GREEN, RED, NA }

enum class ValueKind { DEGREES, RATIO, SPEED_MS, DIST_SH, PERCENT;

    fun unit(lang: Lang) = when (this) {
        DEGREES -> if (lang == Lang.RU) "°" else "°"
        RATIO -> "x"
        SPEED_MS -> "m/s"
        DIST_SH -> if (lang == Lang.RU) "плеч" else "sh"
        PERCENT -> "%"
    }
}

/**
 * Результат одной метрики в одной фазе.
 * value/proMin/proMax — доминантная сторона; valueNd/proMinNd/proMaxNd —
 * недоминантная (пары D/ND из оригинала). Если ND-диапазон задан, GREEN
 * только когда обе стороны в своих диапазонах.
 */
data class MetricResult(
    val id: String,
    val value: Float,
    val proMin: Float,
    val proMax: Float,
    val kind: ValueKind,
    val status: MetricStatus,
    val correction: CorrectionText? = null,
    val valueNd: Float? = null,
    val proMinNd: Float? = null,
    val proMaxNd: Float? = null,
    /** Классификация вместо числа (например, стойка: Open/Semi-open/Closed/Neutral). */
    val classification: String? = null,
    /** Дословные строки сравнения с профи из оригинала (offset'ы в реверсе). */
    val proNote: String? = null,
)

data class CorrectionText(val title: String, val body: String)

/** Метрики одной фазы + сводка. */
data class MomentResult(
    val moment: Moment,
    val frameFrom: Int,
    val frameTo: Int,
    val metrics: List<MetricResult>,
) {
    val greenCount get() = metrics.count { it.status == MetricStatus.GREEN }
    val redCount get() = metrics.count { it.status == MetricStatus.RED }
    val score: Float
        get() {
            val n = metrics.count { it.status != MetricStatus.NA }
            return if (n == 0) Float.NaN else 100f * greenCount / n
        }
}

data class AnalysisResult(
    val stroke: StrokeType,
    val handedness: Handedness,
    val moments: List<MomentResult>,
    val overallScore: Int,
    val frameCount: Int,
    val durationMs: Long,
) {
    val redMetrics: List<Pair<Moment, MetricResult>>
        get() = moments.flatMap { m -> m.metrics.filter { it.status == MetricStatus.RED }.map { m.moment to it } }
}
