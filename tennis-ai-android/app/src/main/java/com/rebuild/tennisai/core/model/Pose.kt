package com.rebuild.tennisai.core.model

/**
 * BlazePose (MediaPipe Pose Landmarker) — 33 landmarks.
 * Coordinates are normalized to [0..1]; y points down (as in image space).
 */
data class Vec3(val x: Float, val y: Float, val z: Float = 0f) {
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(k: Float) = Vec3(x * k, y * k, z * k)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun norm() = kotlin.math.sqrt(dot(this))
    fun distTo(o: Vec3) = (this - o).norm()
    fun dist2To(o: Vec3): Float {
        val dx = x - o.x; val dy = y - o.y; val dz = z - o.z
        return dx * dx + dy * dy + dz * dz
    }
}

enum class Lm(val idx: Int) {
    NOSE(0),
    LEFT_EYE_INNER(1), LEFT_EYE(2), LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4), RIGHT_EYE(5), RIGHT_EYE_OUTER(6),
    LEFT_EAR(7), RIGHT_EAR(8),
    MOUTH_LEFT(9), MOUTH_RIGHT(10),
    LEFT_SHOULDER(11), RIGHT_SHOULDER(12),
    LEFT_ELBOW(13), RIGHT_ELBOW(14),
    LEFT_WRIST(15), RIGHT_WRIST(16),
    LEFT_PINKY(17), RIGHT_PINKY(18),
    LEFT_INDEX(19), RIGHT_INDEX(20),
    LEFT_THUMB(21), RIGHT_THUMB(22),
    LEFT_HIP(23), RIGHT_HIP(24),
    LEFT_KNEE(25), RIGHT_KNEE(26),
    LEFT_ANKLE(27), RIGHT_ANKLE(28),
    LEFT_HEEL(29), RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31), RIGHT_FOOT_INDEX(32);
}

/** A single pose frame. tMillis is the time from the start of the clip. visibility 0..1. */
data class PoseFrame(
    val tMillis: Long,
    val points: Array<Vec3>,   // size == 33
    val visibility: FloatArray = FloatArray(33) { 1f },
    val personBox: BBox? = null,
) {
    fun p(l: Lm): Vec3 = points[l.idx]
    fun vis(l: Lm): Float = visibility[l.idx]

    val shoulderMid: Vec3 get() = (p(Lm.LEFT_SHOULDER) + p(Lm.RIGHT_SHOULDER)) * 0.5f
    val hipMid: Vec3 get() = (p(Lm.LEFT_HIP) + p(Lm.RIGHT_HIP)) * 0.5f

    /** Shoulder width in normalized coordinates — the base scale unit. */
    val shoulderWidth: Float get() = p(Lm.LEFT_SHOULDER).distTo(p(Lm.RIGHT_SHOULDER))

    override fun equals(other: Any?): Boolean =
        other is PoseFrame && other.tMillis == tMillis && other.points.contentEquals(points)
    override fun hashCode(): Int = tMillis.hashCode() * 31 + points.contentHashCode()
}

data class BBox(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width get() = maxX - minX
    val height get() = maxY - minY
    val centerX get() = (minX + maxX) / 2f
    val centerY get() = (minY + maxY) / 2f
}

/** The player's dominant hand. */
enum class Handedness { RIGHT, LEFT;

    fun dom(side: Side): Side = if (this == RIGHT) side else side.flip()
}

enum class Side { LEFT, RIGHT;

    fun flip() = if (this == LEFT) RIGHT else LEFT
}

/** Landmark access accounting for the dominant hand: player.dom(Side.RIGHT).lm(frame) */
data class SideRef(val side: Side) {
    fun shoulder() = if (side == Side.LEFT) Lm.LEFT_SHOULDER else Lm.RIGHT_SHOULDER
    fun elbow() = if (side == Side.LEFT) Lm.LEFT_ELBOW else Lm.RIGHT_ELBOW
    fun wrist() = if (side == Side.LEFT) Lm.LEFT_WRIST else Lm.RIGHT_WRIST
    fun hip() = if (side == Side.LEFT) Lm.LEFT_HIP else Lm.RIGHT_HIP
    fun knee() = if (side == Side.LEFT) Lm.LEFT_KNEE else Lm.RIGHT_KNEE
    fun ankle() = if (side == Side.LEFT) Lm.LEFT_ANKLE else Lm.RIGHT_ANKLE
    fun heel() = if (side == Side.LEFT) Lm.LEFT_HEEL else Lm.RIGHT_HEEL
    fun foot() = if (side == Side.LEFT) Lm.LEFT_FOOT_INDEX else Lm.RIGHT_FOOT_INDEX
}
