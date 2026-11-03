package com.rebuild.tennisai

import com.rebuild.tennisai.core.engine.AnalysisEngine
import com.rebuild.tennisai.core.math.angleDeg
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lm
import com.rebuild.tennisai.core.model.MetricStatus
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.core.model.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class CoreEngineTest {

    private fun v(x: Float, y: Float) = Vec3(x, y)

    @Test
    fun `angle at right angle is 90`() {
        val a = angleDeg(v(0f, 0f), v(1f, 0f), v(1f, 1f))
        assertEquals(90f, a, 0.01f)
    }

    @Test
    fun `straight line is 180`() {
        val a = angleDeg(v(0f, 0f), v(1f, 0f), v(2f, 0f))
        assertEquals(180f, a, 0.01f)
    }

    @Test
    fun `serve synthetic clip produces seven moments and scoring`() {
        val frames = syntheticServe()
        val result = AnalysisEngine.analyze(frames, StrokeType.SERVE, Handedness.RIGHT)

        assertEquals(7, result.moments.size)
        assertTrue(result.frameCount == frames.size)
        assertTrue(result.overallScore in 0..100)

        // В синтетике (палка-скелет) часть метрик честно уходит в red (наклон 0° и т.п.),
        // но известные корректные значения должны быть зелёными: KNEE_FLEXION в стойке.
        val kneeGreen = result.moments.any { m -> m.metrics.any { it.id == "KNEE_FLEXION" && it.status == MetricStatus.GREEN } }
        assertTrue(kneeGreen, "expected KNEE_FLEXION GREEN in stance")
        val greens = result.moments.sumOf { m -> m.metrics.count { it.status == MetricStatus.GREEN } }
        assertTrue(greens > 0, "expected some greens")

        // Метрика контакта существует и имеет валидный статус
        val contact = result.moments.first { it.moment == com.rebuild.tennisai.core.model.Moment.M5 }
        assertTrue(contact.metrics.isNotEmpty(), "contact moment must have metrics")
    }

    @Test
    fun `deep knees in loading turn triple extension red with correction`() {
        // В раскладке оригинала фаза загрузки (M3) — M3_TRIPLE_EXTENSION + M3_TROPHY_POSITION.
        // Согнутые колени (90°) ломают тройное разгибание (порог 140-180).
        val frames = syntheticServe(kneeBendOverride = 90f)
        val result = AnalysisEngine.analyze(frames, StrokeType.SERVE, Handedness.RIGHT)
        val red = result.moments.flatMap { it.metrics }
            .firstOrNull { it.id == "M3_TRIPLE_EXTENSION" && it.status == MetricStatus.RED }
        assertTrue(red != null, "expected M3_TRIPLE_EXTENSION RED when knees deeply bent")
        assertTrue(red!!.correction != null)
        assertEquals(MetricStatus.RED, red.status)
    }

    /**
     * Синтетическая подача: игрок стоит, подбрасывает (левая кисть вверх), правая бьёт вверх.
     * y направлен вниз. 30 кадров @ 33мс.
     */
    private fun syntheticServe(kneeBendOverride: Float? = null): List<PoseFrame> {
        val n = 30
        val out = ArrayList<PoseFrame>(n)
        var kneeAngle = 160f
        for (i in 0 until n) {
            val t = i / 30f // 0..1
            val tossY = if (i < 10) 0.60f else (0.60f - (i - 10) * 0.02f).coerceAtLeast(0.25f)
            val hitY = when {
                i < 18 -> 0.55f
                else -> (0.55f - (i - 18) * 0.035f).coerceAtLeast(0.15f)
            }
            // колени: сгиб до ~120 в середине, разгиб к контакту
            kneeAngle = if (i < 12) 165f - i * 3f else if (i < 22) 120f + (i - 12) * 4f else 160f
            if (kneeBendOverride != null && i in 12..26) kneeAngle = kneeBendOverride

            val pts = arrayOfNulls<Vec3>(33)
            // базовый «стоящий» скелет (вид сбоку, правша: правая сторона чуть левее в кадре)
            fun put(l: Lm, x: Float, y: Float) { pts[l.idx] = Vec3(x, y) }
            put(Lm.NOSE, 0.5f, 0.12f)
            put(Lm.LEFT_SHOULDER, 0.46f, 0.25f); put(Lm.RIGHT_SHOULDER, 0.54f, 0.25f)
            put(Lm.LEFT_ELBOW, 0.44f, 0.35f); put(Lm.RIGHT_ELBOW, 0.56f, 0.35f)
            put(Lm.LEFT_WRIST, 0.42f, tossY); put(Lm.RIGHT_WRIST, 0.58f, hitY)
            put(Lm.LEFT_HIP, 0.47f, 0.50f); put(Lm.RIGHT_HIP, 0.53f, 0.50f)
            // колени/стопы из угла kneeAngle вокруг бедра (радиус 0.12)
            val kneeRad = Math.toRadians((180f - kneeAngle).toDouble())
            val kneeX = 0.50f + 0.0f
            val kneeY = 0.50f + 0.12f * cos(kneeRad).toFloat()
            val ankleY = kneeY + 0.12f * cos(kneeRad).toFloat()
            val ankleX = kneeX + 0.12f * sin(kneeRad).toFloat()
            put(Lm.LEFT_KNEE, kneeX - 0.02f, kneeY); put(Lm.RIGHT_KNEE, kneeX + 0.02f, kneeY)
            put(Lm.LEFT_ANKLE, ankleX - 0.02f, ankleY); put(Lm.RIGHT_ANKLE, ankleX + 0.02f, ankleY)
            put(Lm.LEFT_HEEL, ankleX - 0.03f, ankleY + 0.02f); put(Lm.RIGHT_HEEL, ankleX + 0.01f, ankleY + 0.02f)
            put(Lm.LEFT_FOOT_INDEX, ankleX - 0.05f, ankleY + 0.03f); put(Lm.RIGHT_FOOT_INDEX, ankleX - 0.01f, ankleY + 0.03f)
            // прочие точки — тихо на месте, чтобы массив был полным
            for (k in 0 until 33) if (pts[k] == null) pts[k] = Vec3(0.5f, 0.5f)

            out.add(PoseFrame(tMillis = (i * 33L), points = pts.map { it!! }.toTypedArray()))
        }
        return out
    }
}
