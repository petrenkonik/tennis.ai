package com.rebuild.tennisai.core.math

import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Angle at point b for three points a-b-c, in degrees (0..180). */
fun angleDeg(a: Vec3, b: Vec3, c: Vec3): Float {
    val u = a - b
    val v = c - b
    val du = u.norm()
    val dv = v.norm()
    if (du < 1e-9f || dv < 1e-9f) return 180f
    val cos = (u.dot(v) / (du * dv)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cos).toDouble()).toFloat()
}

/** Angle of line a-b relative to vertical (pointing down), in degrees 0..90. */
fun tiltFromVerticalDeg(a: Vec3, b: Vec3): Float {
    val v = b - a
    val horiz = sqrt(v.x * v.x + v.z * v.z)
    val len = sqrt(horiz * horiz + v.y * v.y)
    if (len < 1e-9f) return 0f
    return Math.toDegrees(kotlin.math.atan2(horiz, abs(v.y)).toDouble()).toFloat()
}

/**
 * Angle between two 2D lines (in the frame's XY plane), degrees 0..180.
 * Used for the shoulder/hip "twist".
 */
fun lineAngleDeg(p1: Vec3, p2: Vec3, q1: Vec3, q2: Vec3): Float {
    val u = Vec3(p2.x - p1.x, p2.y - p1.y)
    val v = Vec3(q2.x - q1.x, q2.y - q1.y)
    val du = u.norm(); val dv = v.norm()
    if (du < 1e-9f || dv < 1e-9f) return 0f
    val dot = (u.dot(v) / (du * dv)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(dot).toDouble()).toFloat()
}

/** Centered moving average with window w (odd). */
fun smooth(values: List<Float>, w: Int = 5): List<Float> {
    if (values.isEmpty()) return values
    val half = max(1, w) / 2
    return List(values.size) { i ->
        var s = 0f; var n = 0
        for (j in max(0, i - half)..min(values.size - 1, i + half)) { s += values[j]; n++ }
        s / n
    }
}

/** Signal velocity (units per second) across frames. */
fun velocity(frames: List<PoseFrame>, values: List<Float>): List<Float> {
    val n = values.size
    if (n < 2) return List(n) { 0f }
    return List(n) { i ->
        when (i) {
            0 -> (values[1] - values[0]) / dtSec(frames, 1)
            n - 1 -> (values[n - 1] - values[n - 2]) / dtSec(frames, n - 1)
            else -> (values[i + 1] - values[i - 1]) / (dtSec(frames, i + 1) - dtSec(frames, i - 1)).coerceAtLeast(1e-3f)
        }
    }
}

private fun dtSec(frames: List<PoseFrame>, i: Int): Float {
    val a = frames[maxOf(0, i - 1)].tMillis
    val b = frames[i].tMillis
    return max(1f, (b - a).toFloat()) / 1000f
}

/** Point speed magnitude (frame units per second), smoothed. */
fun pointSpeed(frames: List<PoseFrame>, pick: (PoseFrame) -> Vec3): List<Float> {
    val n = frames.size
    if (n == 0) return emptyList()
    val out = FloatArray(n)
    for (i in 0 until n) {
        val a = frames[maxOf(0, i - 1)]
        val b = frames[minOf(n - 1, i + 1)]
        val dt = max(1f, (b.tMillis - a.tMillis).toFloat()) / 1000f
        out[i] = pick(b).distTo(pick(a)) / dt
    }
    return smooth(out.toList(), 5)
}

/** Index of the first element matching the predicate, starting at from. */
fun firstIdx(values: List<Float>, from: Int, pred: (Float) -> Boolean): Int {
    for (i in from until values.size) if (pred(values[i])) return i
    return -1
}

fun argmin(values: List<Float>, from: Int = 0, toExclusive: Int = values.size): Int {
    var best = from.coerceIn(0, maxOf(values.size - 1, 0))
    var bv = Float.MAX_VALUE
    for (i in from until toExclusive) if (values[i] < bv) { bv = values[i]; best = i }
    return best
}

fun argmax(values: List<Float>, from: Int = 0, toExclusive: Int = values.size): Int {
    var best = from.coerceIn(0, maxOf(values.size - 1, 0))
    var bv = -Float.MAX_VALUE
    for (i in from until toExclusive) if (values[i] > bv) { bv = values[i]; best = i }
    return best
}
