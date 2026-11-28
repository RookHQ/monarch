package com.rookhq.monarch.components.files.mediaupload

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.files.fileupload.MonarchPickedFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The state of a media field: checks, uploads, cancels and removes, and what to show and announce. */
internal class MediaUploadSession {
    var progress by mutableStateOf<MediaUploadProgress?>(null)
        private set
    var lastKind by mutableStateOf<MonarchMediaUploadKind?>(null)
        private set
    var failure by mutableStateOf<String?>(null)
        private set
    var warnings by mutableStateOf<List<String>>(emptyList())
        private set
    var announcement by mutableStateOf("")
        private set

    private var job: Job? = null
    private lateinit var scope: CoroutineScope
    private lateinit var texts: MediaUploadTexts
    private lateinit var limits: MediaUploadLimits
    private lateinit var readDuration: suspend (Uri) -> Long?
    private lateinit var onUpload: suspend (MonarchPickedFile, MonarchMediaUploadContext) -> MonarchMediaUploadResult
    private lateinit var onValueChange: (String?) -> Unit

    /** Takes what the latest composition passed. */
    @Suppress("LongParameterList")
    fun bind(
        scope: CoroutineScope,
        texts: MediaUploadTexts,
        limits: MediaUploadLimits,
        readDuration: suspend (Uri) -> Long?,
        onUpload: suspend (MonarchPickedFile, MonarchMediaUploadContext) -> MonarchMediaUploadResult,
        onValueChange: (String?) -> Unit,
    ) {
        this.scope = scope
        this.texts = texts
        this.limits = limits
        this.readDuration = readDuration
        this.onUpload = onUpload
        this.onValueChange = onValueChange
    }

    /** Checks the length of a video when there is a limit, then uploads [file]. */
    fun start(file: MonarchPickedFile) {
        val kind = MonarchMediaUploadTokens.kindOf(file.mimeType, file.name)
        failure = null
        warnings = emptyList()
        job = scope.launch {
            try {
                val refusal = check(file, kind)
                if (refusal != null) failure = refusal else upload(file, kind)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                failure = error.message ?: texts.uploadFailed
            } finally {
                progress = null
                job = null
            }
        }
    }

    private suspend fun check(file: MonarchPickedFile, kind: MonarchMediaUploadKind): String? {
        if (kind != MonarchMediaUploadKind.Video || limits.maxVideoDurationMillis == null) return null
        progress = MediaUploadProgress(texts.checking(file.name), null, checking = true)
        return texts.durationRefusal(file.name, readDuration(file.uri), limits)
    }

    private suspend fun upload(file: MonarchPickedFile, kind: MonarchMediaUploadKind) {
        progress = MediaUploadProgress(texts.uploading(file.name, null), null, checking = false)
        val context = MonarchMediaUploadContext(kind) { fraction ->
            val percent = MonarchMediaUploadTokens.percent(fraction)
            progress = MediaUploadProgress(texts.uploading(file.name, percent), fraction, checking = false)
        }
        val result = onUpload(file, context)
        lastKind = kind
        warnings = result.warnings
        announcement = texts.uploaded(kind)
        onValueChange(result.url)
    }

    /** Cancels the check or the upload. */
    fun cancel() {
        job?.cancel()
        progress = null
        announcement = texts.cancelled
    }

    /** Removes the file. */
    fun remove(kind: MonarchMediaUploadKind) {
        failure = null
        warnings = emptyList()
        announcement = texts.removed(kind)
        onValueChange(null)
    }
}
