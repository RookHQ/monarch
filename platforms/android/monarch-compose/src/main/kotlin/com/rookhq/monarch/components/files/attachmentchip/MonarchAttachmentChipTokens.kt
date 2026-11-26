package com.rookhq.monarch.components.files.attachmentchip

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.NumberFormat

/** Component tokens and rules of MonarchAttachmentChip, built from primitives and roles only. */
internal object MonarchAttachmentChipTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val LineGap: Dp = MonarchTokens.Space.space0_5.dp
    val Padding: PaddingValues = PaddingValues(
        start = MonarchTokens.Space.space2.dp,
        end = MonarchTokens.Space.space1.dp,
        top = MonarchTokens.Space.space1.dp,
        bottom = MonarchTokens.Space.space1.dp,
    )

    /** The surface: the default surface and border, or the subtle danger fill and border when the upload failed. */
    fun colors(status: MonarchUploadStatus, colors: MonarchColors): MonarchSurfaceColors =
        if (status == MonarchUploadStatus.Failed) {
            MonarchSurfaceColors(colors.dangerSubtle, colors.textDefault, colors.dangerBorder)
        } else {
            MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
        }

    /** The colour of the name: the default text colour; an underline, not a loud colour, says it opens the file. */
    @Suppress("UNUSED_PARAMETER", "UnusedParameter")
    fun nameColor(opens: Boolean, colors: MonarchColors): Color = colors.textDefault

    /** The colour of the line under the name: muted, and the strong danger colour for a failure. */
    fun metaColor(status: MonarchUploadStatus, colors: MonarchColors): Color =
        if (status == MonarchUploadStatus.Failed) colors.dangerStrong else colors.textMuted

    /** The progress limited to 0 through 1, or `null` for an upload of unknown length. */
    fun fraction(progress: Float?): Float? = progress?.takeIf { !it.isNaN() }?.coerceIn(0f, 1f)

    /** "45%" in the user's locale. */
    fun percent(fraction: Float): String = NumberFormat.getPercentInstance().format(fraction.toDouble())

    /** Whether the name opens the file: only once uploaded, and only with a callback. */
    fun opens(status: MonarchUploadStatus, hasCallback: Boolean): Boolean =
        hasCallback && status == MonarchUploadStatus.Done
}
