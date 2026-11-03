package com.rebuild.tennisai.engine

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.rebuild.tennisai.core.model.BBox
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.Vec3

/**
 * Обёртка MediaPipe Pose Landmarker (BlazePose, 33 точки).
 * GPU-делегат с автоматическим откатом на CPU (на эмуляторе GPU-делегат недоступен).
 */
class PoseEngine private constructor(private val landmarker: PoseLandmarker) {

    fun detectBitmap(bitmap: Bitmap, tMillis: Long): PoseFrame? {
        val mpResult = landmarker.detect(BitmapImageBuilder(bitmap).build())
        return mpResult.toPoseFrame(tMillis)
    }

    fun close() = landmarker.close()

    companion object {
        private const val MODEL = "pose_landmarker_full.task"

        fun create(context: Context, preferGpu: Boolean = true): PoseEngine {
            fun make(delegate: Delegate): PoseLandmarker {
                val base = BaseOptions.builder()
                    .setModelAssetPath(MODEL)
                    .setDelegate(delegate)
                    .build()
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(base)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setMinPosePresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()
                return PoseLandmarker.createFromOptions(context, options)
            }

            return try {
                if (preferGpu) make(Delegate.GPU) else make(Delegate.CPU)
            } catch (_: Exception) {
                make(Delegate.CPU)
            }.let(::PoseEngine)
        }
    }
}

fun PoseLandmarkerResult.toPoseFrame(tMillis: Long): PoseFrame? {
    if (landmarks().isEmpty()) return null
    val lm = landmarks()[0]
    val pts = Array(lm.size) { i ->
        val p = lm[i]
        Vec3(p.x(), p.y(), p.z())
    }
    if (pts.size < 33) return null
    // bounding box по видимым точкам
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    for (p in pts) {
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    return PoseFrame(
        tMillis = tMillis,
        points = pts,
        personBox = BBox(minX, minY, maxX, maxY),
    )
}
