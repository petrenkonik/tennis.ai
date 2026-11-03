package com.rebuild.tennisai.core.phases

import com.rebuild.tennisai.core.math.argmax
import com.rebuild.tennisai.core.math.argmin
import com.rebuild.tennisai.core.math.firstIdx
import com.rebuild.tennisai.core.math.pointSpeed
import com.rebuild.tennisai.core.math.smooth
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lm
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.StrokeType
import com.rebuild.tennisai.core.model.Vec3

/**
 * Phase boundaries by frame indices: boundaries[i] = [from, to) for moment i (M0..M6).
 */
data class MomentBoundaries(val ranges: List<IntRange>) {
    operator fun get(m: Int) = ranges[m]
    companion object {
        val FULL = MomentBoundaries(List(7) { 0..0 })
    }
}

interface MomentDetector {
    fun detect(frames: List<PoseFrame>, handedness: Handedness): MomentBoundaries
}

/**
 * Serve: anchor events — toss start (the non-dominant wrist moves up),
 * toss apex, contact (peak dominant-wrist speed above the shoulders).
 */
class ServeMomentDetector : MomentDetector {

    override fun detect(frames: List<PoseFrame>, h: Handedness): MomentBoundaries {
        val n = frames.size
        if (n < 8) return MomentBoundaries.FULL

        val tossWrist = { f: PoseFrame -> f.p(if (h == Handedness.RIGHT) Lm.LEFT_WRIST else Lm.RIGHT_WRIST) }
        val hitWrist = { f: PoseFrame -> f.p(if (h == Handedness.RIGHT) Lm.RIGHT_WRIST else Lm.LEFT_WRIST) }
        val shoulderY = frames.map { it.shoulderMid.y }
        val yShoulder = shoulderMidValue(frames)

        val tossY = smooth(frames.map { tossWrist(it).y }, 5)
        val vyToss = smooth(frames.mapIndexed { i, f ->
            if (i == 0) 0f else (tossY[i] - tossY[i - 1]) / dtSec(frames, i)
        }, 5)

        // Toss start: sustained upward arm motion (y decreasing)
        val thresh = -0.35f // frames/sec in normalized coordinates (y down)
        var tossStart = firstIdx(vyToss, 1) { it < thresh }
        if (tossStart < 0) tossStart = n / 3

        // Toss apex: minimum y of the tossing wrist between the start and 3/4 of the clip
        val apex = argmin(tossY, tossStart, (n * 3) / 4 + 1).coerceAtLeast(tossStart + 1)

        // Contact: maximum hitting-wrist speed after the apex, with the wrist above the shoulders
        val speed = pointSpeed(frames) { hitWrist(it) }
        val contactWindowFrom = apex.coerceAtMost(n - 2)
        var contact = argmax(speed, contactWindowFrom, n)
        // if the speed peak landed low (backswing), search for the one closest to the upper segment
        if (contact < n - 1 && frames[contact].let { it.p(if (h == Handedness.RIGHT) Lm.RIGHT_WRIST else Lm.LEFT_WRIST).y } > yShoulder) {
            val highZone = (contactWindowFrom until n).filter { frames[it].p(if (h == Handedness.RIGHT) Lm.RIGHT_WRIST else Lm.LEFT_WRIST).y < yShoulder }
            if (highZone.isNotEmpty()) contact = argmax(speed, highZone.first(), (highZone.last() + 1).coerceAtMost(n))
        }

        val stance0End = (tossStart * 0.5f).toInt().coerceAtLeast(1)
        return MomentBoundaries(listOf(
            0 until stance0End,                                   // M0 Ready
            stance0End until tossStart,                           // M1 Stance
            tossStart until apex,                                 // M2 Toss & Backswing
            apex until ((apex + contact) / 2).coerceAtLeast(apex + 1), // M3 Loading
            ((apex + contact) / 2).coerceAtLeast(apex + 1) until contact.coerceAtLeast(apex + 1), // M4 Extension
            contact.coerceAtLeast(apex + 1) until (contact + 1).coerceAtMost(n), // M5 Contact
            (contact + 1).coerceAtMost(n - 1) until n,            // M6 Recovery
        ))
    }

    private fun shoulderMidValue(frames: List<PoseFrame>) = frames.map { it.shoulderMid.y }.average().toFloat()

    private fun dtSec(frames: List<PoseFrame>, i: Int): Float {
        val a = frames[i - 1].tMillis
        val b = frames[i].tMillis
        return ((b - a).coerceAtLeast(1L)).toFloat() / 1000f
    }
}

/**
 * Groundstrokes (forehand/backhand): anchor events — split step (feet widening),
 * takeback (maximum shoulder turn), contact (wrist speed peak), follow-through.
 */
class GroundstrokeMomentDetector : MomentDetector {

    override fun detect(frames: List<PoseFrame>, h: Handedness): MomentBoundaries {
        val n = frames.size
        if (n < 8) return MomentBoundaries.FULL

        val hitWrist = { f: PoseFrame -> f.p(if (h == Handedness.RIGHT) Lm.RIGHT_WRIST else Lm.LEFT_WRIST) }
        val feetWidth = frames.map { it.p(Lm.LEFT_ANKLE).distTo(it.p(Lm.RIGHT_ANKLE)) }
        val fw = smooth(feetWidth, 5)

        // Split step: local feet-width expansion in the first third
        val firstThird = (n / 3).coerceAtLeast(2)
        val splitEnd = argmax(fw, 0, firstThird)

        // Shoulder turn (unit turn): angle of the shoulder line relative to the screen vertical,
        // maximum before contact
        val shoulderAngle = frames.map { tilt(it) }
        val sa = smooth(shoulderAngle, 5)

        // Contact: peak hitting-wrist speed in the second half
        val speed = pointSpeed(frames) { hitWrist(it) }
        val contact = argmax(speed, splitEnd.coerceAtMost(n - 2), n).coerceAtLeast(splitEnd + 1)

        // Takeback: maximum shoulder turn before contact
        val takeback = argmax(sa, 0, contact)

        val readyEnd = (splitEnd * 0.5f).toInt().coerceAtLeast(1)
        val drive = ((takeback + contact) / 2).coerceAtLeast(takeback + 1)
        val followEnd = (contact + (n - contact) / 2).coerceAtMost(n - 1)

        return MomentBoundaries(listOf(
            0 until readyEnd,                    // M0 Ready
            readyEnd until (splitEnd + 1),       // M1 Split step
            (splitEnd + 1) until takeback.coerceAtLeast(splitEnd + 2), // M2 Unit turn
            takeback.coerceAtLeast(splitEnd + 2) until drive,          // M3 Drive/load
            drive until contact,                 // M4 → contact
            contact until (contact + 1).coerceAtMost(n), // M5 Contact
            followEnd.coerceAtLeast(contact + 1) until n, // M6 Recovery (follow-through window)
        ))
    }

    private fun tilt(f: PoseFrame): Float {
        val ls = f.p(Lm.LEFT_SHOULDER)
        val rs = f.p(Lm.RIGHT_SHOULDER)
        val dx = rs.x - ls.x
        val dy = rs.y - ls.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len < 1e-6f) return 0f
        // angle of the shoulder line to the frame's horizontal
        return Math.toDegrees(kotlin.math.atan2(kotlin.math.abs(dy), kotlin.math.abs(dx)).toDouble()).toFloat()
    }
}

fun detectorFor(stroke: StrokeType): MomentDetector = when (stroke) {
    StrokeType.SERVE -> ServeMomentDetector()
    else -> GroundstrokeMomentDetector()
}

/** Checks that a point is "visible enough" for a metric. */
fun Vec3.visible(vis: Float, min: Float = 0.35f) = vis >= min
