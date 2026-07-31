package ru.ritual.app.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.ui.theme.RitualTheme
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoCircleRecorderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            RitualTheme {
                VideoCircleRecorder(
                    onSaved = { output ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_URI, output.uri.toString())
                                .putExtra(EXTRA_NAME, output.file.name),
                        )
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_URI = "recorded_video_uri"
        const val EXTRA_NAME = "recorded_video_name"
    }
}

@Composable
private fun VideoCircleRecorder(onSaved: (MediaOutput) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val storage = remember(context) { MediaNoteStorage(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var activeOutput by remember { mutableStateOf<MediaOutput?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lensFacing) {
        val view = previewView ?: return@LaunchedEffect
        runCatching {
            val provider = context.awaitCameraProvider()
            val preferredSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val alternateFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            val alternateSelector = CameraSelector.Builder().requireLensFacing(alternateFacing).build()
            val hasPreferred = provider.hasCamera(preferredSelector)
            val hasAlternate = provider.hasCamera(alternateSelector)
            val selector = when {
                hasPreferred -> preferredSelector
                hasAlternate -> alternateSelector
                else -> error("No available camera")
            }
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            // The default selector can fall back to a resolution synthesized by CameraX on
            // devices (and emulators) that do not publish complete encoder profiles.
            val recorder = Recorder.Builder().build()
            val capture = VideoCapture.withOutput(recorder)
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            cameraProvider = provider
            videoCapture = capture
            canSwitchCamera = hasPreferred && hasAlternate
            errorText = null
        }.onFailure {
            videoCapture = null
            errorText = "Не удалось открыть камеру"
        }
    }

    LaunchedEffect(isRecording, startedAt) {
        while (isRecording) {
            elapsedMs = SystemClock.elapsedRealtime() - startedAt
            if (elapsedMs >= 60_000L) {
                recording?.stop()
                break
            }
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recording?.close()
            cameraProvider?.unbindAll()
        }
    }

    val stopOrClose: () -> Unit = {
        if (isRecording) {
            recording?.stop()
            Unit
        } else {
            onCancel()
        }
    }
    BackHandler(onBack = stopOrClose)

    Column(
        modifier = Modifier.fillMaxSize().background(Ink)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = stopOrClose) {
                Icon(if (isRecording) Icons.Outlined.Stop else Icons.Outlined.Close, "Закрыть", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isRecording) formatVideoTime(elapsedMs) else "ЗАПИСЬ ВИДЕО", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(if (isRecording) "до 60 секунд" else "кадр войдёт в круг", color = Color.White.copy(.55f), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = { if (!isRecording) lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT },
                enabled = !isRecording && canSwitchCamera,
            ) {
                Icon(
                    Icons.Outlined.Cameraswitch,
                    "Сменить камеру",
                    tint = Color.White.copy(alpha = if (canSwitchCamera) 1f else .3f),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).aspectRatio(1f)
                .clip(CircleShape).background(Color.Black).border(3.dp, if (isRecording) Color(0xFFFF5A5F) else Lime, CircleShape),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { currentContext ->
                    PreviewView(currentContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = this
                    }
                },
            )
        }
        Spacer(Modifier.weight(1f))

        errorText?.let {
            Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(76.dp).clip(CircleShape)
                    .background(if (isRecording) Color(0xFFFF5A5F) else Color.White)
                    .clickable(enabled = videoCapture != null) {
                        if (isRecording) {
                            recording?.stop()
                        } else {
                            val capture = videoCapture ?: return@clickable
                            val output = storage.createVideoOutput()
                            activeOutput = output
                            var pending = capture.output.prepareRecording(context, FileOutputOptions.Builder(output.file).build())
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                pending = pending.withAudioEnabled()
                            }
                            recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> {
                                        isRecording = true
                                        startedAt = SystemClock.elapsedRealtime()
                                        elapsedMs = 0L
                                    }
                                    is VideoRecordEvent.Finalize -> {
                                        isRecording = false
                                        recording = null
                                        val completed = activeOutput
                                        activeOutput = null
                                        if (!event.hasError() && completed != null && completed.file.length() > 0L) {
                                            onSaved(completed)
                                        } else {
                                            storage.delete(completed)
                                            errorText = "Запись не сохранилась. Попробуйте ещё раз."
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isRecording) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(Color.White))
                } else {
                    Box(Modifier.size(58.dp).border(3.dp, Ink.copy(.2f), CircleShape).clip(CircleShape))
                }
            }
        }
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener(
        {
            runCatching { future.get() }
                .onSuccess { continuation.resume(it) }
                .onFailure { continuation.resumeWithException(it) }
        },
        ContextCompat.getMainExecutor(this),
    )
    continuation.invokeOnCancellation { future.cancel(true) }
}

private fun formatVideoTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
