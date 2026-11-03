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
 * Result of a single metric within a single phase.
 * value/proMin/proMax are the dominant side; valueNd/proMinNd/proMaxNd are the
 * non-dominant one (the original's D/ND pairs). When an ND range is set, the status
 * is GREEN only if both sides are within their own ranges.
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
    /** Classification instead of a number (e.g. stance: Open/Semi-open/Closed/Neutral). */
    val classification: String? = null,
    /** Verbatim pro-comparison strings from the original (offsets in the reverse engineering). */
    val proNote: String? = null,
)

data class CorrectionText(val title: String, val body: String)

/** Metrics of a single phase + summary. */
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
