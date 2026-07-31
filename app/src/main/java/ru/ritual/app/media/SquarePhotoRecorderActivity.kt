package ru.ritual.app.media

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.ui.theme.RitualTheme
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SquarePhotoRecorderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            RitualTheme {
                SquarePhotoRecorder(
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
        const val EXTRA_URI = "recorded_photo_uri"
        const val EXTRA_NAME = "recorded_photo_name"
    }
}

@Composable
private fun SquarePhotoRecorder(onSaved: (MediaOutput) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val storage = remember(context) { MediaNoteStorage(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var canSwitchCamera by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(previewView, lensFacing) {
        val view = previewView ?: return@LaunchedEffect
        runCatching {
            val provider = context.awaitPhotoCameraProvider()
            val preferred = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val alternateFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            val alternate = CameraSelector.Builder().requireLensFacing(alternateFacing).build()
            val hasPreferred = provider.hasCamera(preferred)
            val hasAlternate = provider.hasCamera(alternate)
            val selector = when {
                hasPreferred -> preferred
                hasAlternate -> alternate
                else -> error("No available camera")
            }
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview)
            cameraProvider = provider
            isCameraReady = true
            canSwitchCamera = hasPreferred && hasAlternate
            errorText = null
        }.onFailure {
            isCameraReady = false
            errorText = "Не удалось открыть камеру"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }
    BackHandler(enabled = !isSaving, onBack = onCancel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel, enabled = !isSaving) {
                Icon(Icons.Outlined.Close, "Закрыть", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isSaving) "СОХРАНЯЮ" else "ФОТО", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("квадратный кадр", color = Color.White.copy(.55f), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                },
                enabled = canSwitchCamera && !isSaving,
            ) {
                Icon(
                    Icons.Outlined.Cameraswitch,
                    "Сменить камеру",
                    tint = Color.White.copy(if (canSwitchCamera) 1f else .3f),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black)
                .border(3.dp, Lime, RoundedCornerShape(3.dp)),
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
            Text(it, color = Color(0xFFFFB4AB), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        Box(
            modifier = Modifier
                .padding(vertical = 18.dp)
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(if (isSaving) .45f else 1f))
                .clickable(enabled = isCameraReady && !isSaving) {
                    val view = previewView ?: return@clickable
                    val previewBitmap = view.bitmap
                    if (previewBitmap == null) {
                        errorText = "Кадр ещё не готов"
                        return@clickable
                    }
                    val output = storage.createPhotoOutput()
                    isSaving = true
                    cameraExecutor.execute {
                        runCatching { saveSquarePreview(previewBitmap, output.file) }
                            .onSuccess { activity.runOnUiThread { onSaved(output) } }
                            .onFailure {
                                storage.delete(output)
                                activity.runOnUiThread {
                                    isSaving = false
                                    errorText = "Фото не сохранилось. Попробуйте ещё раз."
                                }
                            }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(58.dp).border(3.dp, Ink.copy(.2f), CircleShape))
        }
    }
}

private fun saveSquarePreview(source: Bitmap, file: File) {
    val side = minOf(source.width, source.height)
    val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    FileOutputStream(file, false).use { stream ->
        check(square.compress(Bitmap.CompressFormat.JPEG, 92, stream))
    }
    if (square !== source) source.recycle()
    square.recycle()
}

private suspend fun Context.awaitPhotoCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
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
