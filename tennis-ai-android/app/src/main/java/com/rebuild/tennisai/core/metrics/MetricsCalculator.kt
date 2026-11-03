package com.rebuild.tennisai.core.metrics

import com.rebuild.tennisai.core.math.angleDeg
import com.rebuild.tennisai.core.math.lineAngleDeg
import com.rebuild.tennisai.core.math.tiltFromVerticalDeg
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lm
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.Side
import com.rebuild.tennisai.core.model.SideRef
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Рост игрока в см — база перевода нормализованных координат в сантиметры. */
var playerHeightCm: Float = 175f

/** Результат вычисления метрики: значение D, опционально ND, опционально классификация. */
data class Computed(
    val value: Float?,
    val valueNd: Float? = null,
    val classification: String? = null,
)

/**
 * Расчёт метрик по фазе. Формулы — наша геометрия (в снапшоте восстанавливаются
 * только пороги/описания), диапазоны — оригинальные.
 */
object MetricsCalculator {

    fun compute(
        id: String,
        frames: List<PoseFrame>,
        h: Handedness,
        allFrames: List<PoseFrame>,
        contactIdx: Int,
    ): Computed {
        if (frames.isEmpty() && id != MetricDefs.STANCE_TYPE.id) return Computed(null)
        val dom = SideRef(if (h == Handedness.RIGHT) Side.RIGHT else Side.LEFT)
        val ndom = SideRef(dom.side.flip())

        return when (id) {
            MetricDefs.KNEE_FLEXION.id -> Computed(medAngle(frames, dom) { p, s -> angleDeg(p.p(s.hip()), p.p(s.knee()), p.p(s.ankle())) }, medAngle(frames, ndom) { p, s -> angleDeg(p.p(s.hip()), p.p(s.knee()), p.p(s.ankle())) })
            MetricDefs.DOM_KNEE.id -> Computed(medAngle(frames, dom) { p, s -> angleDeg(p.p(s.hip()), p.p(s.knee()), p.p(s.ankle())) })
            MetricDefs.NDOM_KNEE.id -> Computed(medAngle(frames, ndom) { p, s -> angleDeg(p.p(s.hip()), p.p(s.knee()), p.p(s.ankle())) })
            MetricDefs.ANKLE_HIP_SHOULDER.id -> Computed(medAngle(frames, dom) { p, s -> angleDeg(p.p(s.ankle()), p.p(s.hip()), p.p(s.shoulder())) })
            MetricDefs.HEEL_HIP_SHOULDER.id -> Computed(medAngle(frames, dom) { p, s -> angleDeg(p.p(s.heel()), p.p(s.hip()), p.p(s.shoulder())) })
            MetricDefs.STANCE_WIDTH.id -> Computed(stanceWidth(frames))
            MetricDefs.TRUNK_TILT.id -> Computed(median(frames.map { tiltFromVerticalDeg(it.hipMid, it.shoulderMid) }))
            MetricDefs.SHOULDERS_LEVEL.id -> Computed(shouldersLevel(frames))
            MetricDefs.BODY_CENTER.id -> Computed(bodyCenter(frames))
            MetricDefs.M1_ARMS_POSITION.id, MetricDefs.ARMS_POSITION_G.id -> Computed(armsPosition(frames))
            MetricDefs.M1_FOOT_ANGLE.id -> Computed(footAngle(frames, dom))
            MetricDefs.M2_KNEES_POSITION.id -> Computed(frames.map { angleDeg(it.p(dom.hip()), it.p(dom.knee()), it.p(dom.ankle())) }.minOrNull())
            MetricDefs.M2_UNIT_TURN.id, MetricDefs.M2_WEIGHT_TRANSFER.id -> Computed(weightTransfer(frames, h))
            MetricDefs.M3_TRIPLE_EXTENSION.id -> Computed(tripleExtension(frames, dom))
            MetricDefs.M3_TROPHY_POSITION.id -> trophyArms(frames, dom, ndom)
            MetricDefs.M4_TRUNK_ROTATION.id -> Computed(median(frames.map { tiltFromVerticalDeg(it.hipMid, it.shoulderMid) }))
            MetricDefs.M5_CONTACT_POINT_HEIGHT_DISTANCE.id -> Computed(contactHeightRatio(frames, contactIdx))
            MetricDefs.M5_RACKET_HEAD_POSITION.id -> Computed(null) // требует детекции ракетки (YOLO) — NA
            MetricDefs.TOSS_ARM_EXTENSION.id -> Computed(medAngle(frames, ndom) { p, s -> angleDeg(p.p(s.shoulder()), p.p(s.elbow()), p.p(s.wrist())) })
            MetricDefs.TOSS_ARM_SPEED.id -> Computed(maxSpeedMs(allFrames, ndom, phaseRange(frames, allFrames)))
            MetricDefs.WRIST_SPEED.id -> Computed(speedMs(allFrames, dom, contactIdx))
            MetricDefs.DOM_ARM_EXTENSION.id -> Computed(medAngle(frames, dom) { p, s -> angleDeg(p.p(s.shoulder()), p.p(s.elbow()), p.p(s.wrist())) })
            MetricDefs.NON_DOM_ELBOW.id -> Computed(medAngle(frames, ndom) { p, s -> angleDeg(p.p(s.shoulder()), p.p(s.elbow()), p.p(s.wrist())) })
            MetricDefs.BACK_LEG_LANDING.id -> Computed(backLegLanding(frames, ndom))
            MetricDefs.UNIT_TURN.id, MetricDefs.TORSO_ROTATION.id -> Computed(unitTurn(frames))
            MetricDefs.WRIST_OVER_HIP.id -> Computed(wristOverHip(frames, dom))
            MetricDefs.CONTACT_IN_FRONT.id -> Computed(contactInFront(frames, dom, h))
            MetricDefs.RACQUET_PATH.id -> Computed(null) // требует детекции ракетки — NA
            MetricDefs.STANCE_TYPE.id -> Computed(null, null, stanceClassification(frames, h))
            else -> Computed(null)
        }
    }

    private fun phaseRange(frames: List<PoseFrame>, all: List<PoseFrame>): Int {
        if (frames.isEmpty() || all.isEmpty()) return all.size / 2
        val last = frames.last()
        return all.indexOfFirst { it.tMillis >= last.tMillis }.coerceAtLeast(all.size / 2)
    }

    private fun medAngle(frames: List<PoseFrame>, side: SideRef, f: (PoseFrame, SideRef) -> Float): Float? =
        median(frames.map { f(it, side) })

    private fun median(vals: List<Float>): Float? {
        if (vals.isEmpty()) return null
        val s = vals.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2f
    }

    private fun stanceWidth(frames: List<PoseFrame>): Float? {
        val vals = frames.map { f ->
            val sw = f.shoulderWidth
            if (sw < 1e-5f) null else f.p(Lm.LEFT_ANKLE).distTo(f.p(Lm.RIGHT_ANKLE)) / sw
        }
        return median(vals.filterNotNull())
    }

    private fun shouldersLevel(frames: List<PoseFrame>): Float? =
        median(frames.map { f ->
            val dy = abs(f.p(Lm.LEFT_SHOULDER).y - f.p(Lm.RIGHT_SHOULDER).y)
            val dx = abs(f.p(Lm.LEFT_SHOULDER).x - f.p(Lm.RIGHT_SHOULDER).x)
            Math.toDegrees(atan2(dy, dx.coerceAtLeast(1e-6f)).toDouble()).toFloat()
        })

    private fun bodyCenter(frames: List<PoseFrame>): Float? =
        median(frames.map { ((it.hipMid.x + it.shoulderMid.x) / 2f) - 0.5f })

    /** Средний угол обеих рук (плечо-локоть-запястье), вертикальности готовности. */
    private fun armsPosition(frames: List<PoseFrame>): Float? {
        val vals = frames.map { f ->
            val l = angleDeg(f.p(Lm.LEFT_SHOULDER), f.p(Lm.LEFT_ELBOW), f.p(Lm.LEFT_WRIST))
            val r = angleDeg(f.p(Lm.RIGHT_SHOULDER), f.p(Lm.RIGHT_ELBOW), f.p(Lm.RIGHT_WRIST))
            (l + r) / 2f
        }
        return median(vals)
    }

    /** Угол стопы (пятка→носок) к горизонтали кадра. */
    private fun footAngle(frames: List<PoseFrame>, dom: SideRef): Float? =
        median(frames.map { f ->
            val heel = f.p(dom.heel())
            val toe = f.p(dom.foot())
            Math.toDegrees(atan2(abs(toe.y - heel.y), abs(toe.x - heel.x).coerceAtLeast(1e-6f)).toDouble()).toFloat()
        })

    private fun tripleExtension(frames: List<PoseFrame>, dom: SideRef): Float? =
        frames.map { f ->
            val knee = angleDeg(f.p(dom.hip()), f.p(dom.knee()), f.p(dom.ankle()))
            val hip = angleDeg(f.p(dom.shoulder()), f.p(dom.hip()), f.p(dom.knee()))
            minOf(knee, hip)
        }.maxOrNull()

    /** Trophy: угол плечо–локоть от вертикали; D 40–120, ND 79–170. */
    private fun trophyArms(frames: List<PoseFrame>, dom: SideRef, ndom: SideRef): Computed {
        fun armAngle(f: PoseFrame, s: SideRef): Float {
            val v = f.p(s.elbow()) - f.p(s.shoulder())
            val horiz = sqrt(v.x * v.x + v.z * v.z)
            val len = sqrt(horiz * horiz + v.y * v.y)
            if (len < 1e-6f) return 0f
            // угол от вертикали «вниз» — поднятая рука даёт 120–180
            return Math.toDegrees(atan2(horiz, abs(v.y)).toDouble()).toFloat() + (if (v.y < 0) 90f else 0f)
        }
        val d = median(frames.map { armAngle(it, dom) })
        val n = median(frames.map { armAngle(it, ndom) })
        return Computed(d, n)
    }

    /** Высота контакта в долях роста игрока (запястье над голеностопами / высота тела в кадре). */
    private fun contactHeightRatio(frames: List<PoseFrame>, contactIdx: Int): Float? {
        val f = frames.maxByOrNull { it.shoulderMid.x * 0f + (it.p(Lm.LEFT_WRIST).distTo(it.shoulderMid)) } ?: return null
        val ankleMidY = (f.p(Lm.LEFT_ANKLE).y + f.p(Lm.RIGHT_ANKLE).y) / 2f
        val wristY = kotlin.math.min(f.p(Lm.LEFT_WRIST).y, f.p(Lm.RIGHT_WRIST).y)
        val bodyH = bodyHeightNorm(f)
        if (bodyH < 1e-4f) return null
        return (ankleMidY - wristY) / bodyH
    }

    /** Скорость запястья в м/с через масштаб роста игрока. */
    private fun speedMs(all: List<PoseFrame>, side: SideRef, contactIdx: Int): Float? {
        if (all.size < 3) return null
        val i = contactIdx.coerceIn(1, all.size - 2)
        val a = all[i - 1]
        val b = all[i + 1]
        val dt = ((b.tMillis - a.tMillis).coerceAtLeast(1L)).toFloat() / 1000f
        val speedNorm = a.p(side.wrist()).distTo(b.p(side.wrist())) / dt
        val heightNorm = bodyHeightNorm(b)
        if (heightNorm < 1e-4f) return null
        return speedNorm * (playerHeightCm / 100f / heightNorm)
    }

    private fun maxSpeedMs(all: List<PoseFrame>, side: SideRef, untilIdx: Int): Float? {
        if (all.size < 4) return null
        val to = untilIdx.coerceIn(2, all.size - 1)
        var best = 0f
        for (i in 1 until to) {
            val a = all[i - 1]; val b = all[i + 1]
            val dt = ((b.tMillis - a.tMillis).coerceAtLeast(1L)).toFloat() / 1000f
            val heightNorm = bodyHeightNorm(b).takeIf { it > 1e-4f } ?: continue
            val v = a.p(side.wrist()).distTo(b.p(side.wrist())) / dt * (playerHeightCm / 100f / heightNorm)
            if (v > best) best = v
        }
        return best
    }

    private fun backLegLanding(frames: List<PoseFrame>, ndom: SideRef): Float? =
        frames.map { angleDeg(it.p(ndom.hip()), it.p(ndom.knee()), it.p(ndom.ankle())) }.maxOrNull()

    /** Разворот плеч к камере (acos dx/len) — малые значения = сильная ротация. */
    private fun unitTurn(frames: List<PoseFrame>): Float? {
        val vals = frames.mapNotNull { f ->
            val ls = f.p(Lm.LEFT_SHOULDER); val rs = f.p(Lm.RIGHT_SHOULDER)
            val dx = abs(rs.x - ls.x); val dy = abs(rs.y - ls.y)
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1e-6f) null else Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        }
        return median(vals)
    }

    private fun wristOverHip(frames: List<PoseFrame>, dom: SideRef): Float? {
        val vals = frames.mapNotNull { f ->
            val sw = f.shoulderWidth
            if (sw < 1e-5f) null else (f.hipMid.y - f.p(dom.wrist()).y) / sw
        }
        return median(vals)
    }

    private fun contactInFront(frames: List<PoseFrame>, dom: SideRef, h: Handedness): Float? {
        val vals = frames.mapNotNull { f ->
            val sw = f.shoulderWidth
            if (sw < 1e-5f) null
            else {
                val sign = if (h == Handedness.RIGHT) 1f else -1f
                (f.p(dom.wrist()).x - f.hipMid.x) * sign / sw
            }
        }
        return median(vals)
    }

    private fun weightTransfer(frames: List<PoseFrame>, h: Handedness): Float? {
        if (frames.isEmpty()) return null
        val sign = if (h == Handedness.RIGHT) 1f else -1f
        val sw = frames.first().shoulderWidth
        if (sw < 1e-5f) return null
        return (frames.last().hipMid.x - frames.first().hipMid.x) * sign / sw
    }

    /**
     * Классификация стойки на контакте: угол линии стоп к горизонтали кадра.
     * <25° открытая, 25–50° полузакрытая, >50° нейтральная/закрытая.
     */
    private fun stanceClassification(frames: List<PoseFrame>, h: Handedness): String? {
        val f = frames.lastOrNull() ?: return null
        val dx = abs(f.p(Lm.RIGHT_ANKLE).x - f.p(Lm.LEFT_ANKLE).x)
        val dy = abs(f.p(Lm.RIGHT_ANKLE).y - f.p(Lm.LEFT_ANKLE).y)
        val angle = Math.toDegrees(atan2(dy, dx.coerceAtLeast(1e-6f)).toDouble()).toFloat()
        return when {
            angle < 25f -> "Open"
            angle < 50f -> "Semi-open"
            else -> if (h == Handedness.RIGHT) "Neutral/Closed" else "Neutral/Closed"
        }
    }

    private fun bodyHeightNorm(f: PoseFrame): Float {
        val ankleMidY = (f.p(Lm.LEFT_ANKLE).y + f.p(Lm.RIGHT_ANKLE).y) / 2f
        val headY = f.p(Lm.NOSE).y
        return abs(ankleMidY - headY) * 1.25f
    }
}
