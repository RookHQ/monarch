package com.rookhq.monarch.components.datadisplay.favicon

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens of MonarchFavicon, built from primitives and roles only. */
internal object MonarchFaviconTokens {
    /** The letter is this fraction of the tile, as on the web. */
    const val LETTER_SCALE: Float = 0.6f

    /** The weight of the letter. */
    const val LETTER_WEIGHT: Int = MonarchTokens.FontWeight.semibold

    /** The side of the tile. A favicon is a picture and does not grow with the font size setting. */
    fun size(size: MonarchFaviconSize): Dp = when (size) {
        MonarchFaviconSize.Sm -> MonarchTokens.Space.space4.dp
        MonarchFaviconSize.Md -> MonarchTokens.Space.space6.dp
        MonarchFaviconSize.Lg -> MonarchTokens.Space.space8.dp
    }

    /** The clip of the image and of the letter tile. */
    fun shape(shape: MonarchFaviconShape, shapes: MonarchShapes = MonarchShapes.Default): CornerBasedShape =
        when (shape) {
            MonarchFaviconShape.Rounded -> shapes.xs
            MonarchFaviconShape.Circle -> shapes.full
            MonarchFaviconShape.Square -> shapes.none
        }

    /** The letter tile: the muted background, muted text and a subtle border. */
    fun fallbackColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgMuted, content = colors.textMuted, border = colors.borderSubtle)

    /**
     * The letter of the tile: the first letter or digit of [name], upper-cased
     * in [locale], or "?" when there is none. A whole code point is taken, so
     * a letter outside the Basic Multilingual Plane is not split.
     */
    fun letter(name: String, locale: Locale = Locale.getDefault()): String {
        var index = 0
        while (index < name.length) {
            val codePoint = name.codePointAt(index)
            if (Character.isLetterOrDigit(codePoint)) {
                return String(Character.toChars(codePoint)).uppercase(locale)
            }
            index += Character.charCount(codePoint)
        }
        return FALLBACK_LETTER
    }

    private const val FALLBACK_LETTER = "?"
}
