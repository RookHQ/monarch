package com.rookhq.monarch.components.files.mediaupload

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.math.roundToInt

/** Why a picked file is refused before upload. */
internal enum class MediaUploadRefusal { VideoNotAllowed, TooLarge }

/** Component tokens and the pure rules of MonarchMediaUpload. */
internal object MonarchMediaUploadTokens {
    /** The gap between the parts of the field. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The width over height of the preview. */
    const val PREVIEW_RATIO: Float = 16f / 9f

    /** The least side of the preview when it is a button. */
    val PreviewMinSize: Dp = MonarchTouchTarget.Minimum

    /** The accepted types for the photo picker and the checks. */
    const val IMAGES: String = "image/*"
    const val VIDEOS: String = "video/*"

    private val videoExtension = Regex("\\.(mp4|m4v|mov|webm|ogv|avi|3gp|mkv)(?:[?#]|$)", RegexOption.IGNORE_CASE)

    /** The kind of a file from its MIME type, or its name when the type is unknown. */
    fun kindOf(mimeType: String?, name: String): MonarchMediaUploadKind = when {
        mimeType != null && mimeType.lowercase().startsWith("video/") -> MonarchMediaUploadKind.Video
        mimeType != null && mimeType.lowercase().startsWith("image/") -> MonarchMediaUploadKind.Image
        videoExtension.containsMatchIn(name) -> MonarchMediaUploadKind.Video
        else -> MonarchMediaUploadKind.Image
    }

    /** The kind of the stored file, guessed from its address: a video extension or `data:video/`, else an image. */
    fun kindOfValue(value: String): MonarchMediaUploadKind =
        if (value.trim().lowercase().startsWith("data:video/") || videoExtension.containsMatchIn(value.trim())) {
            MonarchMediaUploadKind.Video
        } else {
            MonarchMediaUploadKind.Image
        }

    /** The MIME types the picker offers: images, and videos when taken or when their refusal is explained. */
    fun accept(kinds: Set<MonarchMediaUploadKind>, explainsVideo: Boolean): List<String> = buildList {
        if (MonarchMediaUploadKind.Image in kinds) add(IMAGES)
        if (MonarchMediaUploadKind.Video in kinds || explainsVideo) add(VIDEOS)
    }

    /** Whether videos are offered. */
    fun takesVideo(kinds: Set<MonarchMediaUploadKind>): Boolean = MonarchMediaUploadKind.Video in kinds

    /** Why a file of [kind] and [size] bytes is refused, or `null` when it may be uploaded. */
    fun refusal(
        kind: MonarchMediaUploadKind,
        size: Long?,
        kinds: Set<MonarchMediaUploadKind>,
        maxImageSize: Long?,
        maxVideoSize: Long?,
    ): MediaUploadRefusal? {
        val limit = if (kind == MonarchMediaUploadKind.Video) maxVideoSize else maxImageSize
        return when {
            kind !in kinds -> MediaUploadRefusal.VideoNotAllowed
            limit != null && size != null && size > limit -> MediaUploadRefusal.TooLarge
            else -> null
        }
    }

    /** Whether a video of [durationMillis] is too long for [maxMillis]; an unknown length is decided elsewhere. */
    fun tooLong(durationMillis: Long, maxMillis: Long?): Boolean = maxMillis != null && durationMillis > maxMillis

    /** A reported fraction as a whole percentage, or `null` while unknown. */
    fun percent(fraction: Float?): Int? = fraction?.let { (it.coerceIn(0f, 1f) * PERCENT).roundToInt() }

    private const val PERCENT = 100

    /** The tile behind a preview while the app's painter is `null`. */
    fun placeholder(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the status text while checking or uploading. */
    fun statusColor(colors: MonarchColors): Color = colors.textMuted
}
