package com.rookhq.monarch.components.intelligence.aiimagegeneration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.NumberFormat
import java.util.Locale

/** Component tokens and pure logic of MonarchAiImageGeneration, built from primitives and roles only. */
internal object MonarchAiImageGenerationTokens {
    /** The gap between the status, the progress bar and the grid. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between grid cells, both ways. */
    val GridGap: Dp = MonarchTokens.Space.space4.dp

    /** The gap between an image, its field and its actions. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest width of a column at the default font size, the web's 10rem; scaled with the font size. */
    val MinColumnWidth: Dp = (MonarchTokens.Space.space20 * 2).dp

    /** The blur over a refining preview. Drawn on Android 12 and later; earlier versions show it sharp. */
    val RefiningBlur: Dp = MonarchTokens.Space.space1.dp

    /** The width of the ring round the chosen image. */
    val SelectedRingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The space round the "Selected" mark and inside "Preview unavailable". */
    val MarkPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The padding of the mark's text inside its pill. */
    val MarkInset: Dp = MonarchTokens.Space.space1.dp

    /** What a slot shows. */
    enum class Slot {
        /** The final image. */
        Image,

        /** The image, still refining. */
        Refining,

        /** A placeholder that pulses while generating. */
        Placeholder,

        /** "Preview unavailable". */
        Unavailable,
    }

    /** Whether generation is under way. */
    fun generating(status: MonarchAiImageGenerationStatus): Boolean =
        status == MonarchAiImageGenerationStatus.Pending || status == MonarchAiImageGenerationStatus.Progressive

    /** Progress clamped to 0..1, or `null` when unknown or not a number. */
    fun clamped(progress: Float?): Float? = progress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)

    /** The progress as a percentage in [locale], such as "40%", or `null` when unknown. */
    fun percent(progress: Float?, locale: Locale): String? =
        clamped(progress)?.let { NumberFormat.getPercentInstance(locale).format(it.toDouble()) }

    /** What a slot shows for an image in a status. */
    fun slot(image: MonarchAiGeneratedImage, status: MonarchAiImageGenerationStatus): Slot = when {
        image.image != null && status != MonarchAiImageGenerationStatus.Pending ->
            if (status == MonarchAiImageGenerationStatus.Progressive) Slot.Refining else Slot.Image
        generating(status) -> Slot.Placeholder
        else -> Slot.Unavailable
    }

    /** Whether the alt text field shows its error: empty alt text once images can be seen, not while pending. */
    fun altInvalid(alt: String, status: MonarchAiImageGenerationStatus): Boolean =
        status != MonarchAiImageGenerationStatus.Pending && alt.isBlank()

    /** Whether an image's actions show: finished, shown, and at least one action given. */
    fun showsActions(
        image: MonarchAiGeneratedImage,
        status: MonarchAiImageGenerationStatus,
        canSelect: Boolean,
        canVary: Boolean,
    ): Boolean = status == MonarchAiImageGenerationStatus.Done && slot(image, status) == Slot.Image &&
        (canSelect || canVary)

    /** Whether a status change announces completion: only on the way into `Done`. */
    fun announcesCompletion(from: MonarchAiImageGenerationStatus?, to: MonarchAiImageGenerationStatus): Boolean =
        to == MonarchAiImageGenerationStatus.Done && from != null && from != MonarchAiImageGenerationStatus.Done

    /** The number of columns for the available width and font scale. */
    fun columns(maxWidth: Dp, fontScale: Float): Int =
        maxOf(1, ((maxWidth + GridGap) / (MinColumnWidth * maxOf(1f, fontScale) + GridGap)).toInt())

    /** The colour of the ring round an image: the solid assistant colour when chosen. */
    fun ringColor(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.aiSolid else colors.borderSubtle

    /** The ring width: thicker when chosen, so the choice does not rest on colour. */
    fun ringWidth(selected: Boolean, border: Dp): Dp = if (selected) SelectedRingWidth else border
}
