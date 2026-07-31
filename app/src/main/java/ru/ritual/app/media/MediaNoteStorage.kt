package ru.ritual.app.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaOutput(val file: File, val uri: Uri)

class MediaNoteStorage(private val context: Context) {
    fun createVideoOutput(): MediaOutput = createOutput("video", "video", "mp4")

    fun createPhotoOutput(): MediaOutput = createOutput("photo", "photo", "jpg")

    fun createAudioOutput(): MediaOutput = createOutput("audio", "voice", "m4a")

    fun delete(output: MediaOutput?) {
        output?.file?.takeIf(File::exists)?.delete()
    }

    private fun createOutput(directory: String, prefix: String, extension: String): MediaOutput {
        val folder = File(context.filesDir, "media/$directory").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(folder, "${prefix}_$stamp.$extension")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return MediaOutput(file, uri)
    }
}

class VoiceNoteRecorder(private val context: Context) {
    private val storage = MediaNoteStorage(context)
    private var recorder: MediaRecorder? = null
    private var output: MediaOutput? = null

    @Suppress("DEPRECATION")
    fun start() {
        cancel()
        val nextOutput = storage.createAudioOutput()
        val nextRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        try {
            nextRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(nextOutput.file.absolutePath)
                prepare()
                start()
            }
            output = nextOutput
            recorder = nextRecorder
        } catch (error: Throwable) {
            runCatching { nextRecorder.release() }
            storage.delete(nextOutput)
            throw error
        }
    }

    fun stop(): MediaOutput? {
        val activeRecorder = recorder ?: return null
        val completedOutput = output
        recorder = null
        output = null
        return try {
            activeRecorder.stop()
            activeRecorder.release()
            completedOutput
        } catch (_: Throwable) {
            runCatching { activeRecorder.release() }
            storage.delete(completedOutput)
            null
        }
    }

    fun cancel() {
        val activeRecorder = recorder
        val abandonedOutput = output
        recorder = null
        output = null
        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.release() }
        }
        storage.delete(abandonedOutput)
    }
}
