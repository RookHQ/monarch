package com.rookhq.monarch.components.forms.colorpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchColorPicker, built from primitives and roles only. */
internal object MonarchColorPickerTokens {
    /** The visible side of a swatch, including its selection ring. The touch target is 48dp. */
    val SwatchSize: Dp = MonarchTokens.Space.space8.dp

    /** The width of the selection ring. */
    val RingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The space between the ring and the colour of a selected swatch. */
    val RingGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The inset of the colour of a selected swatch: the ring and the gap. */
    val SelectedInset: Dp = RingWidth + RingGap

    /** The example value in the empty hex field. A value, not text, so it is not translated. */
    const val PLACEHOLDER: String = "#000000"

    /** The side of the mark in the middle of a selected swatch. */
    val MarkSize: Dp = MonarchTokens.Space.space2.dp

    /** The side of the colour shown at the start of the hex field. */
    val PreviewSize: Dp = MonarchTokens.Space.space5.dp

    /** The gap between the label, the swatches, the field and the helper text. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The colours around a swatch.
     *
     * @property ring the selection ring, or the 1dp outline of an unselected swatch.
     * @property mark the fill of the mark in the middle of a selected swatch.
     * @property markBorder the outline of that mark.
     */
    data class SwatchColors(val ring: Color, val mark: Color, val markBorder: Color)

    /**
     * The colours around a swatch. The selection is a ring in the default
     * text colour and a mark in the middle, so it does not depend on seeing
     * the colour; the colour itself is the app's and is never changed.
     */
    fun swatchColors(selected: Boolean, colors: MonarchColors): SwatchColors = SwatchColors(
        ring = if (selected) colors.textDefault else colors.borderDefault,
        mark = colors.bgSurface,
        markBorder = colors.textDefault,
    )

    /** The colour of the label and the helper text. */
    fun textColor(enabled: Boolean, colors: MonarchColors, strong: Boolean): Color = when {
        !enabled -> colors.textDisabled
        strong -> colors.textDefault
        else -> colors.textMuted
    }

    /** The value as lower-case `#rrggbb`, or `null` when it is missing or not a hex colour. */
    fun selection(value: String?): String? = value?.let(MonarchColorSwatch::normalizedHex)

    /**
     * The result of committing the text of the hex field.
     *
     * @property hex the colour to select, or `null` to select nothing new.
     * @property draft the text the field shows afterwards.
     * @property invalid whether the text was not a colour.
     */
    data class Commit(val hex: String?, val draft: String, val invalid: Boolean)

    /**
     * Commits [draft]: empty text restores the selection, text that is not a
     * colour is kept and marked invalid, and a colour is normalised.
     */
    fun commit(draft: String, selection: String?): Commit {
        if (draft.isBlank()) return Commit(null, selection.orEmpty(), invalid = false)
        val hex = MonarchColorSwatch.normalizedHex(draft) ?: return Commit(null, draft, invalid = true)
        return Commit(hex.takeIf { it != selection }, hex, invalid = false)
    }
}
