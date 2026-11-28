package dev.monarchui.components.files.videothumbnail

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale
import kotlin.math.roundToLong

/** Component tokens of MonarchVideoThumbnail, built from primitives and roles only. */
internal object MonarchVideoThumbnailTokens {
    /** The width divided by the height when none is given: 16 to 9. */
    const val DEFAULT_ASPECT_RATIO: Float = 16f / 9f

    /** The diameter of the round play mark. */
    val PlaySize: Dp = MonarchTokens.Space.space12.dp

    /** The side of the triangle inside the play mark. */
    val PlayGlyphSize: Dp = MonarchTokens.Space.space6.dp

    /** The side of the triangle of the placeholder. */
    val PlaceholderGlyphSize: Dp = MonarchTokens.Space.space10.dp

    /** The distance of the duration badge from the corner. */
    val BadgeInset: Dp = MonarchTokens.Space.space2.dp

    /** The padding before and after the text of the badge. */
    val BadgeHorizontalPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The play triangle, the web's glyph, in a 24-unit viewport (SVG path data). */
    const val PLAY_PATH: String = "M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z"

    /** The side of the viewport of [PLAY_PATH]. */
    const val VIEWPORT: Float = 24f

    /** The default second at which a frame is captured, in milliseconds. */
    const val DEFAULT_CAPTURE_MILLIS: Long = 1_000L

    /** The part of a short video's length at which a frame is captured. */
    const val SHORT_VIDEO_CAPTURE_RATIO: Double = 0.1

    /** How far before the end the last frame that can be captured is, in milliseconds. */
    const val CAPTURE_END_MARGIN_MILLIS: Long = 50L

    private const val MILLIS_PER_SECOND = 1_000.0
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L

    /**
     * The colours of a thumbnail.
     *
     * @property background the fill behind the poster, shown while there is no image and beside a fitted one.
     * @property placeholderGlyph the triangle of the placeholder.
     * @property playDisc the disc of the play mark.
     * @property playGlyph the triangle of the play mark.
     * @property badge the fill of the duration badge.
     * @property badgeText the text of the duration badge.
     */
    data class Colors(
        val background: Color,
        val placeholderGlyph: Color,
        val playDisc: Color,
        val playGlyph: Color,
        val badge: Color,
        val badgeText: Color,
    )

    /**
     * The colours of a thumbnail. Opaque roles, not the web's translucent
     * disc and badge, because Monarch does not dim with alpha. A disabled
     * thumbnail mutes the play mark. The letterbox is the emphasis fill, one step off the canvas in every theme
     * (the inverse colour turned white in the dark theme).
     */
    fun colors(enabled: Boolean, colors: MonarchColors): Colors = Colors(
        background = colors.bgEmphasis,
        placeholderGlyph = colors.textMuted,
        playDisc = if (enabled) colors.bgSurface else colors.bgMuted,
        playGlyph = if (enabled) colors.textDefault else colors.textDisabled,
        badge = colors.bgInverse,
        badgeText = colors.textInverse,
    )

    /** The aspect ratio to draw: the given one when it is a positive number, otherwise 16 to 9. */
    fun aspectRatio(ratio: Float?): Float =
        if (ratio != null && ratio.isFinite() && ratio > 0f) ratio else DEFAULT_ASPECT_RATIO

    /** The name of the video: the title without surrounding spaces, or [fallback] when it is empty. */
    fun name(title: String, fallback: String): String = title.trim().ifEmpty { fallback }

    /** Whole seconds of a length, never negative; `null` when there is none. */
    fun wholeSeconds(durationMillis: Long?): Long? =
        durationMillis?.let { (it.coerceAtLeast(0L) / MILLIS_PER_SECOND).roundToLong() }

    /** The length as shown on the badge: `1:05`, or `1:01:05` from an hour. */
    fun durationText(durationMillis: Long?, locale: Locale): String? {
        val seconds = wholeSeconds(durationMillis) ?: return null
        val hours = seconds / SECONDS_PER_HOUR
        val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(locale, "%d:%02d", minutes, rest)
        }
    }

    /** The length as TalkBack reads it, in words for [locale]: "1 minute, 5 seconds". */
    @Suppress("SpreadOperator") // MeasureFormat takes a vararg; the copy is of at most three measures.
    fun spokenDuration(durationMillis: Long?, locale: Locale): String? {
        val seconds = wholeSeconds(durationMillis) ?: return null
        val parts = listOf(
            seconds / SECONDS_PER_HOUR to MeasureUnit.HOUR,
            seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE to MeasureUnit.MINUTE,
            seconds % SECONDS_PER_MINUTE to MeasureUnit.SECOND,
        ).filter { (amount, _) -> amount > 0 }
            .map { (amount, unit) -> Measure(amount, unit) }
            .ifEmpty { listOf(Measure(0, MeasureUnit.SECOND)) }
        return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE).formatMeasures(*parts.toTypedArray())
    }

    /**
     * The time at which a frame is captured, in milliseconds: the requested
     * one, or one second in, or a tenth of the length for a video shorter
     * than ten seconds. Always inside the video.
     */
    fun captureTimeMillis(requestedMillis: Long?, durationMillis: Long): Long {
        val length = durationMillis.coerceAtLeast(0L)
        val wanted = requestedMillis
            ?: minOf(DEFAULT_CAPTURE_MILLIS, (length * SHORT_VIDEO_CAPTURE_RATIO).roundToLong())
        return wanted.coerceIn(0L, (length - CAPTURE_END_MARGIN_MILLIS).coerceAtLeast(0L))
    }
}
