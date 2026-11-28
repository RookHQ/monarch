package dev.monarchui.components.files.mediaupload

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import dev.monarchui.components.files.fileupload.MonarchFileUpload
import dev.monarchui.components.files.fileupload.MonarchFileUploadPicker
import dev.monarchui.components.files.fileupload.MonarchFileUploadVariant
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchFormatBytes
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A field that holds one image or video: pick it with the system photo
 * picker, see it checked and uploaded, then preview it with Replace and
 * Remove.
 *
 * Built from Monarch parts: `MonarchFileUpload` with the photo picker
 * (`ActivityResultContracts.PickVisualMedia`, no storage permission) for
 * choosing and its refusal notice, `MonarchProgress` while uploading, the
 * app's `Painter` as the preview, `MonarchButton`s for Cancel upload and
 * Remove, and `MonarchInlineNotice` for errors and warnings. Before upload a
 * file is checked against [kinds] and the size limits, and a video's length
 * is read from its metadata with `MediaMetadataRetriever` when
 * [maxVideoDurationMillis] is set. Uploads, cancellations and removals are
 * announced.
 *
 * The value is hoisted: [onUpload] stores the file and returns its address,
 * which is passed to [onValueChange]; Remove passes `null`. Monarch loads no
 * images: the app passes the stored image, or a video's poster, as [preview].
 *
 * @param label the visible label, already localised. It is a heading.
 * @param value the address of the stored file, or `null` when empty.
 * @param onValueChange called with the new address after an upload, and with `null` after Remove.
 * @param onUpload stores the file, reports progress through the context and returns the stored address and any
 * warnings. Cancel upload cancels its coroutine. Throw to show the error.
 * @param modifier the modifier of the field.
 * @param kind the kind of the current file. `null` uses the kind of the latest upload, or a guess from [value].
 * @param preview the stored image or the video's poster, loaded by the app. A tinted tile while `null`.
 * @param helperText guidance under the label.
 * @param kinds the kinds that are taken.
 * @param maxImageSize the largest image, in bytes.
 * @param maxVideoSize the largest video, in bytes.
 * @param maxVideoDurationMillis the longest video, in milliseconds.
 * @param enabled whether files can be picked and removed.
 * @param videoUnavailable why videos cannot be added, in the product's words. Shown in the hint when [kinds] has no
 * video, and videos can then be picked so that the refusal explains itself.
 * @param previewLabel the spoken name of the image preview. "Uploaded image" when `null`.
 * @param onPlay plays the video, for example in the system player. Makes the video preview a button.
 * @param labels replacements for the built-in strings.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchMediaUpload(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    onUpload: suspend (MonarchPickedFile, MonarchMediaUploadContext) -> MonarchMediaUploadResult,
    modifier: Modifier = Modifier,
    kind: MonarchMediaUploadKind? = null,
    preview: Painter? = null,
    helperText: String? = null,
    kinds: Set<MonarchMediaUploadKind> = setOf(MonarchMediaUploadKind.Image),
    maxImageSize: Long? = null,
    maxVideoSize: Long? = null,
    maxVideoDurationMillis: Long? = null,
    enabled: Boolean = true,
    videoUnavailable: String? = null,
    previewLabel: String? = null,
    onPlay: (() -> Unit)? = null,
    labels: MonarchMediaUploadLabels = MonarchMediaUploadLabels(),
) {
    val context = LocalContext.current
    val formatSize: (Long) -> String = labels.fileUpload.formatSize ?: { monarchFormatBytes(it) }
    val texts = mediaUploadTexts(labels, formatSize)
    val limits = MediaUploadLimits(kinds, maxImageSize, maxVideoSize, maxVideoDurationMillis)
    val session = remember { MediaUploadSession() }
    session.bind(
        scope = rememberCoroutineScope(),
        texts = texts,
        limits = limits,
        readDuration = { uri -> readDuration(context, uri) },
        onUpload = onUpload,
        onValueChange = onValueChange,
    )
    val currentKind = kind ?: session.lastKind ?: value?.let(MonarchMediaUploadTokens::kindOfValue)
        ?: MonarchMediaUploadKind.Image
    val picker = MediaUploadPickerConfig(texts, limits, videoUnavailable, labels, enabled, session::start)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchMediaUploadTokens.Gap)) {
        MediaUploadHeader(label, helperText, enabled)
        val busy = session.progress
        when {
            busy != null -> MediaUploadBusy(busy, texts.cancel, session::cancel)
            value != null -> MediaUploadFilled(
                kind = currentKind,
                preview = preview,
                description = if (currentKind == MonarchMediaUploadKind.Image) {
                    previewLabel ?: texts.previewImage
                } else {
                    texts.previewVideo
                },
                onPlay = onPlay,
                picker = picker,
                onRemove = { session.remove(currentKind) },
            )
            else -> MediaUploadPicker(picker, MonarchFileUploadVariant.Zone, browse = null)
        }
        MediaUploadNotices(session.failure, session.warnings, texts.warnings)
        // Always present, so each upload, cancellation, removal and failure is announced as a change of it.
        val spoken = session.failure ?: session.announcement
        MonarchLiveStatus(text = spoken.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The label as a heading, and the help text. */
@Composable
private fun MediaUploadHeader(label: String, helperText: String?, enabled: Boolean) {
    val colors = MonarchTheme.colors
    Text(
        text = label,
        modifier = Modifier.semantics { heading() },
        style = MonarchTheme.typography.label,
        color = if (enabled) colors.textDefault else colors.textDisabled,
    )
    if (helperText != null) {
        Text(
            text = helperText,
            style = MonarchTheme.typography.caption,
            color = if (enabled) colors.textMuted else colors.textDisabled,
        )
    }
}

/** The preview with Replace and Remove. */
@Composable
@Suppress("LongParameterList")
private fun MediaUploadFilled(
    kind: MonarchMediaUploadKind,
    preview: Painter?,
    description: String,
    onPlay: (() -> Unit)?,
    picker: MediaUploadPickerConfig,
    onRemove: () -> Unit,
) {
    MediaUploadPreview(
        kind = kind,
        preview = preview,
        description = description,
        playLabel = picker.texts.play,
        onPlay = onPlay.takeIf { kind == MonarchMediaUploadKind.Video },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchMediaUploadTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaUploadPicker(picker, MonarchFileUploadVariant.Compact, browse = picker.texts.replace(kind))
        MonarchButton(
            text = picker.texts.remove(kind),
            onClick = onRemove,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = picker.enabled,
        )
    }
}

/** The failure and the warnings after an upload. */
@Composable
private fun MediaUploadNotices(failure: String?, warnings: List<String>, warningsTitle: String) {
    failure?.let {
        MonarchInlineNotice(message = it, tone = MonarchTone.Danger, variant = MonarchInlineNoticeVariant.Plain)
    }
    if (warnings.isNotEmpty()) {
        MonarchInlineNotice(
            message = warnings.joinToString("\n"),
            tone = MonarchTone.Warning,
            variant = MonarchInlineNoticeVariant.Plain,
            title = warningsTitle,
        )
    }
}

/** The limits of a media field. */
internal class MediaUploadLimits(
    val kinds: Set<MonarchMediaUploadKind>,
    val maxImageSize: Long?,
    val maxVideoSize: Long?,
    val maxVideoDurationMillis: Long?,
)

/** What the field shows while it checks or uploads. */
internal class MediaUploadProgress(val text: String, val fraction: Float?, val checking: Boolean)

/** What the field's picker needs. */
internal class MediaUploadPickerConfig(
    val texts: MediaUploadTexts,
    val limits: MediaUploadLimits,
    val videoUnavailable: String?,
    val labels: MonarchMediaUploadLabels,
    val enabled: Boolean,
    val onFile: (MonarchPickedFile) -> Unit,
)

/** The `MonarchFileUpload` of the field: the empty area, or the compact Replace button. */
@Composable
private fun MediaUploadPicker(config: MediaUploadPickerConfig, variant: MonarchFileUploadVariant, browse: String?) {
    val tokens = MonarchMediaUploadTokens
    val limits = config.limits
    val explainsVideo = config.videoUnavailable != null && !tokens.takesVideo(limits.kinds)
    MonarchFileUpload(
        onFiles = { accepted, _ -> accepted.firstOrNull()?.let(config.onFile) },
        variant = variant,
        picker = MonarchFileUploadPicker.Media,
        accept = tokens.accept(limits.kinds, explainsVideo),
        maxFiles = 1,
        multiple = false,
        enabled = config.enabled,
        hint = if (variant == MonarchFileUploadVariant.Compact) {
            ""
        } else {
            config.texts.hint(limits, config.videoUnavailable)
        },
        validate = { file -> config.texts.refusal(file, limits) },
        labels = config.texts.fileUploadLabels(config.labels, limits.kinds, browse),
    )
}

/** Checking or uploading: the progress bar with its text, and Cancel upload. */
@Composable
private fun MediaUploadBusy(progress: MediaUploadProgress, cancel: String, onCancel: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchMediaUploadTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchMediaUploadTokens.Gap),
        ) {
            Text(
                text = progress.text,
                style = MonarchTheme.typography.caption,
                color = MonarchMediaUploadTokens.statusColor(MonarchTheme.colors),
            )
            MonarchProgress(progress = progress.fraction, label = progress.text)
        }
        MonarchButton(
            text = cancel,
            onClick = onCancel,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

/** Reads a video's length in milliseconds from its metadata, off the main thread, or `null` when unreadable. */
private suspend fun readDuration(context: Context, uri: Uri): Long? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: RuntimeException) {
        // An unreadable file has no known length; the caller refuses it with a message of its own.
        null
    } finally {
        retriever.release()
    }
}
