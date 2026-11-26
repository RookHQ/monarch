package com.rookhq.monarch.components.files.filetypeicon

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchFileTypeIcon, built from primitives and roles only. */
internal object MonarchFileTypeIconTokens {
    /** The side of the tile of the large size. */
    val TileSize: Dp = MonarchTokens.Space.space10.dp

    /** The width of the glyph's lines in the 24-unit viewport, as on the web. */
    const val STROKE_WIDTH: Float = 1.6f

    /** The side of the glyph's viewport. */
    const val VIEWPORT: Float = 24f

    /** The side of the glyph at [size]. */
    fun glyphSize(size: MonarchFileTypeIconSize): Dp = when (size) {
        MonarchFileTypeIconSize.Sm -> MonarchTokens.Space.space4.dp
        MonarchFileTypeIconSize.Md -> MonarchTokens.Space.space5.dp
        MonarchFileTypeIconSize.Lg -> MonarchTokens.Space.space6.dp
    }

    /**
     * The tint of a family. The families share five data colours, as on the
     * web; the glyph's mark, not the colour, tells them apart.
     */
    fun color(family: MonarchFileTypeFamily, colors: MonarchColors): Color = when (family) {
        MonarchFileTypeFamily.Image, MonarchFileTypeFamily.Video -> colors.data1
        MonarchFileTypeFamily.Document, MonarchFileTypeFamily.Text -> colors.data2
        MonarchFileTypeFamily.Spreadsheet, MonarchFileTypeFamily.Code -> colors.data3
        MonarchFileTypeFamily.Audio, MonarchFileTypeFamily.Presentation -> colors.data4
        MonarchFileTypeFamily.Pdf -> colors.data5
        MonarchFileTypeFamily.Archive, MonarchFileTypeFamily.Other -> colors.textMuted
    }

    /** The fill of the tile of the large size. */
    fun tileColor(colors: MonarchColors): Color = colors.bgMuted

    /** The built-in spoken name of a family. */
    @StringRes
    fun label(family: MonarchFileTypeFamily): Int = when (family) {
        MonarchFileTypeFamily.Image -> R.string.monarch_filetypeicon_image
        MonarchFileTypeFamily.Video -> R.string.monarch_filetypeicon_video
        MonarchFileTypeFamily.Audio -> R.string.monarch_filetypeicon_audio
        MonarchFileTypeFamily.Pdf -> R.string.monarch_filetypeicon_pdf
        MonarchFileTypeFamily.Document -> R.string.monarch_filetypeicon_document
        MonarchFileTypeFamily.Spreadsheet -> R.string.monarch_filetypeicon_spreadsheet
        MonarchFileTypeFamily.Presentation -> R.string.monarch_filetypeicon_presentation
        MonarchFileTypeFamily.Archive -> R.string.monarch_filetypeicon_archive
        MonarchFileTypeFamily.Code -> R.string.monarch_filetypeicon_code
        MonarchFileTypeFamily.Text -> R.string.monarch_filetypeicon_text
        MonarchFileTypeFamily.Other -> R.string.monarch_filetypeicon_other
    }

    /** The page outline with a folded corner, shared by every family (SVG path data, 24-unit viewport). */
    const val PAGE: String = "M14 2.75H6.75a2 2 0 0 0-2 2v14.5a2 2 0 0 0 2 2h10.5a2 2 0 0 0 2-2V8z M14 2.75V8h5.25"

    /** The mark inside the page of each family, the web's glyph (SVG path data). */
    fun mark(family: MonarchFileTypeFamily): String? = when (family) {
        MonarchFileTypeFamily.Image ->
            "M8.25 11a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0-2.5 0 M7.5 18l3.5-3.5 2 2 1.75-1.75 2 3.25"
        MonarchFileTypeFamily.Video -> "M10 11.25v5.5l4.5-2.75z"
        MonarchFileTypeFamily.Audio ->
            "M11 17.25v-5.5l4-1v5 M8.5 17.25a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0-2.5 0 " +
                "M12.5 15.75a1.25 1.25 0 1 0 2.5 0a1.25 1.25 0 1 0-2.5 0"
        MonarchFileTypeFamily.Pdf -> "M8.5 17.5c1.5-1 3-3.75 3.5-6.5.25 2.25 1.75 4.5 3.5 5-1.75-.25-4.75 0-7 1.5z"
        MonarchFileTypeFamily.Document -> "M8.5 12h7M8.5 15h7M8.5 18h4.5"
        MonarchFileTypeFamily.Spreadsheet -> "M8 11h8v7.5H8z M8 14.75h8M12 11v7.5"
        MonarchFileTypeFamily.Presentation -> "M8 11h8v5.5H8z M12 16.5v2"
        MonarchFileTypeFamily.Archive -> "M11 3v1.5M12.5 6v1.5M11 9v1.5M12.5 12v1.5M10.75 15.5h2v3h-2z"
        MonarchFileTypeFamily.Code -> "M10 12l-2 2.5 2 2.5M14 12l2 2.5-2 2.5"
        MonarchFileTypeFamily.Text -> "M8.5 12h7M8.5 15h7M8.5 18h7"
        MonarchFileTypeFamily.Other -> null
    }
}
