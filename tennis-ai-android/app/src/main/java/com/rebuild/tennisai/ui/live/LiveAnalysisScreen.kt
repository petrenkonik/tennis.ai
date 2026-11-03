package com.rebuild.tennisai.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.rebuild.tennisai.AppState
import com.rebuild.tennisai.core.math.angleDeg
import com.rebuild.tennisai.core.metrics.MetricDefs
import com.rebuild.tennisai.core.model.CorrectionText
import com.rebuild.tennisai.core.model.Handedness
import com.rebuild.tennisai.core.model.Lm
import com.rebuild.tennisai.core.model.MetricStatus
import com.rebuild.tennisai.core.model.PoseFrame
import com.rebuild.tennisai.core.model.Side
import com.rebuild.tennisai.core.model.SideRef
import com.rebuild.tennisai.engine.PoseEngine
import java.util.concurrent.Executors
import kotlin.math.sqrt

/** BlazePose skeleton: connection pairs. */
private val CONNECTIONS = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 24 to 26, 26 to 28,
    27 to 31, 28 to 32, 27 to 29, 28 to 30,
)

@Composable
fun LiveAnalysisScreen(state: AppState, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var lastFrame by remember { mutableStateOf<PoseFrame?>(null) }
    var frameDims by remember { mutableStateOf(Pair(0, 0)) }
    var correction by remember { mutableStateOf<CorrectionText?>(null) }
    var fps by remember { mutableLongStateOf(0L) }
    val engine = remember { PoseEngine.create(context, preferGpu = true) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            engine.close()
            analyzerExecutor.shutdown()
        }
    }

    val h = state.handedness
    val dom = SideRef(if (h == Handedness.RIGHT) Side.RIGHT else Side.LEFT)
    val ru = state.lang == com.rebuild.tennisai.core.model.Lang.RU

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { ai ->
                                ai.setAnalyzer(analyzerExecutor) { proxy: ImageProxy ->
                                    try {
                                        val t = System.currentTimeMillis()
                                        val raw = proxy.toBitmap()
                                        // ImageAnalysis frames arrive in the sensor orientation (usually +90°).
                                        // Rotate before inference so the coordinates match the preview.
                                        val rotation = proxy.imageInfo.rotationDegrees
                                        val bmp = if (rotation == 0) raw else {
                                            val m = android.graphics.Matrix()
                                            m.postRotate(rotation.toFloat())
                                            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
                                        }
                                        val pf = engine.detectBitmap(bmp, t)
                                        if (pf != null) {
                                            lastFrame = pf
                                            frameDims = Pair(bmp.width, bmp.height)
                                            updateLiveMetrics(pf, dom, h) { correction = it }
                                            fps = t
                                        }
                                        if (bmp !== raw) bmp.recycle()
                                        raw.recycle()
                                    } catch (_: Exception) {
                                    } finally {
                                        proxy.close()
                                    }
                                }
                            }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, analysis,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            // skeleton over the preview
            lastFrame?.let { pf ->
                SkeletonOverlay(pf, frameDims)
            }

            // top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xAA101814))) {
                    Text(
                        text = (if (ru) "LIVE · " else "LIVE · ") + strokeLabel(state),
                        color = Color(0xFFD4FF3F),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text(if (ru) "Стоп" else "Stop", color = Color.White)
                }
            }

            // correction card
            correction?.let { c ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE6FF5A5A)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(c.body, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (ru) "Нужно разрешение на камеру" else "Camera permission required",
                    color = Color.White,
                )
            }
        }
    }
}

private fun strokeLabel(state: AppState): String {
    val ru = state.lang == com.rebuild.tennisai.core.model.Lang.RU
    return when (state.stroke) {
        com.rebuild.tennisai.core.model.StrokeType.SERVE -> if (ru) "Подача" else "Serve"
        com.rebuild.tennisai.core.model.StrokeType.FOREHAND -> if (ru) "Форхенд" else "Forehand"
        com.rebuild.tennisai.core.model.StrokeType.BACKHAND -> if (ru) "Бэкхенд" else "Backhand"
    }
}

/** Real-time: all available per-frame metrics (as in the original — "coach all metrics"). */
private val LIVE_METRIC_IDS = listOf(
    "KNEE_FLEXION", "ALIGNMENT", "TRUNK_TILT", "SHOULDERS_LEVEL", "STANCE_WIDTH", "WRIST_OVER_HIP", "ARMS_POSITION",
)

private fun updateLiveMetrics(pf: PoseFrame, dom: SideRef, h: Handedness, set: (CorrectionText?) -> Unit) {
    for (id in LIVE_METRIC_IDS) {
        val def = com.rebuild.tennisai.core.metrics.MetricDefs.ALL[id] ?: continue
        val c = com.rebuild.tennisai.core.metrics.MetricsCalculator.compute(id, listOf(pf), h, listOf(pf), 0)
        val v = c.value ?: continue
        if (v < def.proMin || v > def.proMax) {
            set(def.correction(com.rebuild.tennisai.core.model.Lang.RU, v))
            return
        }
    }
    set(null)
}

@Composable
private fun SkeletonOverlay(pf: PoseFrame, dims: Pair<Int, Int>) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val hgt = size.height
        val bw = dims.first
        val bh = dims.second
        if (bw < 1 || bh < 1) return@Canvas
        // PreviewView FILL_CENTER: scale the frame while keeping its aspect ratio and center it,
        // otherwise the skeleton drifts when the camera and screen aspect ratios differ.
        val scale = maxOf(w / bw, hgt / bh)
        val scaledW = bw * scale
        val scaledH = bh * scale
        val offX = (w - scaledW) / 2f
        val offY = (hgt - scaledH) / 2f
        fun pt(i: Int) = Offset(offX + pf.points[i].x * scaledW, offY + pf.points[i].y * scaledH)
        val line = Color(0xFFD4FF3F)
        val joint = Color.White
        CONNECTIONS.forEach { (a, b) ->
            drawLine(line, pt(a), pt(b), strokeWidth = 5f)
        }
        for (i in listOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)) {
            drawCircle(joint, radius = sqrt(9f), center = pt(i))
        }
    }
}
