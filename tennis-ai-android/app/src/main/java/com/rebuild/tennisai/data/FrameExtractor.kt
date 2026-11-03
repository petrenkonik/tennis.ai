package com.rebuild.tennisai.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.engine.PoseEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Извлечение кадров из видео и перевод их в позы.
 * Для клипов ≤5 c @ 30fps берём каждый кадр до MAX_FRAMES.
 */
class FrameExtractor(private val context: Context) {

    data class Progress(val stage: String, val done: Int, val total: Int)

    suspend fun extractPoses(
        uri: Uri,
        maxFrames: Int = 150,
        onProgress: suspend (Progress) -> Unit = {},
    ): List<PoseFrame> = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0) return@withContext emptyList()

            val fps = guessFps(retriever) ?: 30f
            val totalFrames = ((durationMs / 1000f) * fps).toInt().coerceAtLeast(1)
            val step = if (totalFrames <= maxFrames) 1f else totalFrames.toFloat() / maxFrames
            val count = (totalFrames / step).toInt().coerceAtMost(maxFrames)

            val engine = PoseEngine.create(context, preferGpu = false) // батч: CPU надёжнее в фоне
            val out = ArrayList<PoseFrame>(count)
            try {
                for (i in 0 until count) {
                    val frameIdx = (i * step).toInt()
                    val timeUs = (frameIdx * 1_000_000L / fps.toLong()).coerceIn(0L, durationMs * 1000L - 1)
                    val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bmp != null) {
                        val scaled = downscale(bmp, 640)
                        val pf = engine.detectBitmap(scaled, timeUs / 1000L)
                        if (pf != null) out.add(pf)
                        if (scaled !== bmp) scaled.recycle()
                        bmp.recycle()
                    }
                    if (i % 10 == 0) onProgress(Progress("DETECTING KEYPOINTS", i, count))
                }
            } finally {
                engine.close()
            }
            onProgress(Progress("CALCULATING ANGLES", count, count))
            out
        } finally {
            retriever.release()
        }
    }

    private fun guessFps(retriever: MediaMetadataRetriever): Float? {
        // API 29+: METADATA_KEY_CAPTURE_FRAMERATE
        return try {
            val m = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            m?.takeIf { it > 5f } ?: 30f
        } catch (_: Exception) {
            30f
        }
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val maxDim = maxOf(src.width, src.height)
        if (maxDim <= maxSide) return src
        val k = maxSide.toFloat() / maxDim
        return Bitmap.createScaledBitmap(src, (src.width * k).toInt().coerceAtLeast(1), (src.height * k).toInt().coerceAtLeast(1), true)
    }
}
