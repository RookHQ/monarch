package dev.monarchui.components.files.mediaupload

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.files.fileupload.MonarchFileUploadLabels
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.internal.MonarchImage
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme
import java.util.concurrent.TimeUnit

/** The resolved strings of a media field, from the labels or the built-in resources. */
@Suppress("TooManyFunctions")
internal class MediaUploadTexts(
    private val resources: Resources,
    private val labels: MonarchMediaUploadLabels,
    private val formatSize: (Long) -> String,
) {
    private fun text(override: String?, id: Int): String = override ?: resources.getString(id)

    val uploadFailed: String get() = text(labels.uploadFailed, R.string.monarch_mediaupload_upload_failed)
    val cancel: String get() = text(labels.cancel, R.string.monarch_mediaupload_cancel)
    val cancelled: String get() = text(labels.cancelled, R.string.monarch_mediaupload_cancelled)
    val previewImage: String get() = resources.getString(R.string.monarch_mediaupload_preview_image)
    val previewVideo: String get() = text(labels.previewVideo, R.string.monarch_mediaupload_preview_video)
    val play: String get() = text(labels.play, R.string.monarch_mediaupload_play)
    val warnings: String get() = text(labels.warnings, R.string.monarch_mediaupload_warnings)

    fun checking(name: String): String =
        labels.checking?.invoke(name) ?: resources.getString(R.string.monarch_mediaupload_checking, name)

    fun uploading(name: String, percent: Int?): String = labels.uploading?.invoke(name, percent)
        ?: if (percent == null) {
            resources.getString(R.string.monarch_mediaupload_uploading, name)
        } else {
            resources.getString(R.string.monarch_mediaupload_uploading_percent, name, percent)
        }

    fun uploaded(kind: MonarchMediaUploadKind): String = when (kind) {
        MonarchMediaUploadKind.Image -> text(labels.imageUploaded, R.string.monarch_mediaupload_image_uploaded)
        MonarchMediaUploadKind.Video -> text(labels.videoUploaded, R.string.monarch_mediaupload_video_uploaded)
    }

    fun replace(kind: MonarchMediaUploadKind): String = when (kind) {
        MonarchMediaUploadKind.Image -> text(labels.replaceImage, R.string.monarch_mediaupload_replace_image)
        MonarchMediaUploadKind.Video -> text(labels.replaceVideo, R.string.monarch_mediaupload_replace_video)
    }

    fun remove(kind: MonarchMediaUploadKind): String = when (kind) {
        MonarchMediaUploadKind.Image -> text(labels.removeImage, R.string.monarch_mediaupload_remove_image)
        MonarchMediaUploadKind.Video -> text(labels.removeVideo, R.string.monarch_mediaupload_remove_video)
    }

    fun removed(kind: MonarchMediaUploadKind): String = when (kind) {
        MonarchMediaUploadKind.Image -> text(labels.imageRemoved, R.string.monarch_mediaupload_image_removed)
        MonarchMediaUploadKind.Video -> text(labels.videoRemoved, R.string.monarch_mediaupload_video_removed)
    }

    /** A length: "1:30 min" from a minute, "45 s" under it. */
    fun duration(millis: Long): String {
        labels.formatDuration?.let { return it(millis) }
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)
        return if (minutes > 0) {
            resources.getString(R.string.monarch_mediaupload_duration_minutes, minutes, seconds % SECONDS_PER_MINUTE)
        } else {
            resources.getString(R.string.monarch_mediaupload_duration_seconds, seconds)
        }
    }

    /** The hint lines: images, videos or why videos are unavailable. */
    fun hint(limits: MediaUploadLimits, videoUnavailable: String?): String {
        val lines = mutableListOf<String>()
        if (MonarchMediaUploadKind.Image in limits.kinds) {
            val size = limits.maxImageSize?.let(formatSize)
            lines += labels.imageHint?.invoke(size) ?: if (size == null) {
                resources.getString(R.string.monarch_mediaupload_image_hint)
            } else {
                resources.getString(R.string.monarch_mediaupload_image_hint_size, size)
            }
        }
        if (MonarchMediaUploadKind.Video in limits.kinds) {
            lines += videoHint(limits.maxVideoSize?.let(formatSize), limits.maxVideoDurationMillis?.let(::duration))
        } else if (videoUnavailable != null) {
            lines += videoUnavailable
        }
        return lines.joinToString("\n")
    }

    private fun videoHint(size: String?, length: String?): String = labels.videoHint?.invoke(size, length) ?: when {
        size != null && length != null -> resources.getString(
            R.string.monarch_mediaupload_video_hint_both,
            size,
            length,
        )
        size != null -> resources.getString(R.string.monarch_mediaupload_video_hint_size, size)
        length != null -> resources.getString(R.string.monarch_mediaupload_video_hint_duration, length)
        else -> resources.getString(R.string.monarch_mediaupload_video_hint)
    }

    /** The refusal of a picked file before upload, or `null` to take it. */
    fun refusal(file: MonarchPickedFile, limits: MediaUploadLimits): String? {
        val kind = MonarchMediaUploadTokens.kindOf(file.mimeType, file.name)
        val refusal = MonarchMediaUploadTokens.refusal(
            kind,
            file.size,
            limits.kinds,
            limits.maxImageSize,
            limits.maxVideoSize,
        )
        val limit = if (kind == MonarchMediaUploadKind.Video) limits.maxVideoSize else limits.maxImageSize
        return when (refusal) {
            MediaUploadRefusal.VideoNotAllowed -> labels.videoNotAllowed?.invoke(file.name)
                ?: resources.getString(R.string.monarch_mediaupload_video_not_allowed, file.name)
            MediaUploadRefusal.TooLarge -> formatSize(limit ?: 0L).let { max ->
                labels.tooLarge?.invoke(file.name, max)
                    ?: resources.getString(R.string.monarch_mediaupload_too_large, file.name, max)
            }
            null -> null
        }
    }

    /** The refusal of a video after its length is read, or `null` to upload it. */
    fun durationRefusal(name: String, durationMillis: Long?, limits: MediaUploadLimits): String? {
        val max = limits.maxVideoDurationMillis ?: return null
        return when {
            durationMillis == null -> labels.durationUnreadable?.invoke(name)
                ?: resources.getString(R.string.monarch_mediaupload_duration_unreadable, name)
            MonarchMediaUploadTokens.tooLong(durationMillis, max) -> duration(max).let { length ->
                labels.tooLong?.invoke(name, length)
                    ?: resources.getString(R.string.monarch_mediaupload_too_long, name, length)
            }
            else -> null
        }
    }

    /** The file picker's strings: the app's, with the media field's area text and button. */
    fun fileUploadLabels(
        labels: MonarchMediaUploadLabels,
        kinds: Set<MonarchMediaUploadKind>,
        browse: String?,
    ): MonarchFileUploadLabels {
        val base = labels.fileUpload
        val video = MonarchMediaUploadKind.Video in kinds
        return MonarchFileUploadLabels(
            label = base.label ?: if (video) {
                text(labels.addMedia, R.string.monarch_mediaupload_add_media)
            } else {
                text(labels.addImage, R.string.monarch_mediaupload_add_image)
            },
            browse = browse ?: base.browse ?: if (video) {
                text(labels.chooseMedia, R.string.monarch_mediaupload_choose_media)
            } else {
                text(labels.chooseImage, R.string.monarch_mediaupload_choose_image)
            },
            limits = base.limits,
            limitReached = base.limitReached,
            invalidType = base.invalidType,
            tooLarge = base.tooLarge,
            tooMany = base.tooMany,
            added = base.added ?: { "" },
            formatSize = formatSize,
        )
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}

/** The texts of a media field in the current resources. */
@Composable
internal fun mediaUploadTexts(labels: MonarchMediaUploadLabels, formatSize: (Long) -> String): MediaUploadTexts =
    MediaUploadTexts(LocalResources.current, labels, formatSize)

/** The preview: the app's image or poster, or a tinted tile; a Play button for a video with [onPlay]. */
@Composable
internal fun MediaUploadPreview(
    kind: MonarchMediaUploadKind,
    preview: Painter?,
    description: String,
    playLabel: String,
    onPlay: (() -> Unit)?,
) {
    val tokens = MonarchMediaUploadTokens
    val shape = MonarchShapeDefaults.Container
    val play = if (onPlay != null && kind == MonarchMediaUploadKind.Video) {
        Modifier
            .clickable(role = Role.Button, onClick = onPlay)
            .semantics { contentDescription = playLabel }
    } else {
        Modifier
    }
    // The placeholder is hidden from TalkBack; while it shows, the preview still has its name.
    val placeholderName = if (preview == null && play == Modifier) {
        Modifier.semantics { contentDescription = description }
    } else {
        Modifier
    }
    MonarchImage(
        painter = preview,
        contentDescription = if (onPlay != null && kind == MonarchMediaUploadKind.Video) null else description,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = tokens.PreviewMinSize)
            .aspectRatio(MonarchMediaUploadTokens.PREVIEW_RATIO)
            .then(play)
            .then(placeholderName),
        shape = shape,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(tokens.placeholder(MonarchTheme.colors)),
        )
    }
}
