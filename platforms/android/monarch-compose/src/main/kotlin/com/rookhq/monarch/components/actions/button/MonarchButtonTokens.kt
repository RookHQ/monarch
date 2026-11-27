package com.rookhq.monarch.components.actions.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a button: the fill and its hover and pressed fills, the content, and the outline.
 *
 * [hover] and [pressed] default to the plain container for a variant that does not change on interaction.
 */
@Immutable
internal data class MonarchButtonColors(
    val container: Color,
    val content: Color,
    val border: Color?,
    val hover: Color = container,
    val pressed: Color = container,
)

/** Component tokens of MonarchButton, built from primitives and roles only. */
internal object MonarchButtonTokens {
    val BorderWidth: Dp = MonarchControlMetrics.BorderWidth
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp
    val SpinnerStrokeWidth: Dp = MonarchControlMetrics.SpinnerStroke

    /** The visible height: the web's 32, 36 and 40. The 48dp touch target is reserved around it. */
    fun minHeight(size: MonarchButtonSize): Dp = when (size) {
        MonarchButtonSize.Sm -> MonarchControlMetrics.ButtonSmall
        MonarchButtonSize.Md -> MonarchControlMetrics.ButtonMedium
        MonarchButtonSize.Lg -> MonarchControlMetrics.ButtonLarge
    }

    /** The side of the square icon-only button: the visible height, as on the web. */
    fun iconButtonSide(size: MonarchButtonSize): Dp = minHeight(size)

    /** The padding from the edge of the visible button to its content: the web padding plus the 1dp border. */
    fun contentPadding(size: MonarchButtonSize): PaddingValues {
        val horizontal = when (size) {
            MonarchButtonSize.Sm -> MonarchTokens.Space.space2_5.dp
            MonarchButtonSize.Md -> MonarchTokens.Space.space3.dp
            MonarchButtonSize.Lg -> MonarchTokens.Space.space4.dp
        }
        return PaddingValues(horizontal = horizontal + BorderWidth, vertical = BorderWidth)
    }

    /** The size of icons and of the loading indicator: 16dp at every size, as on the web. */
    @Suppress("UNUSED_PARAMETER")
    fun iconSize(size: MonarchButtonSize): Dp = MonarchControlMetrics.IconSize

    /**
     * 14sp medium at every size. The web's small button is 12sp, but on a phone a 12sp label next to 14sp body
     * text reads as a caption, and the small ghost button (no fill, no outline) has its label as the only
     * affordance, so every size keeps the 14sp label. The small button is still 32dp high.
     */
    @Suppress("UNUSED_PARAMETER")
    fun textStyle(
        size: MonarchButtonSize,
        typography: MonarchTypography,
        variant: MonarchButtonVariant = MonarchButtonVariant.Secondary,
    ): TextStyle {
        val base = typography.label
        return if (variant == MonarchButtonVariant.Link) base.copy(textDecoration = TextDecoration.Underline) else base
    }

    /**
     * Buttons draw no shadow at any variant: they are inline controls, and only presented surfaces (menu,
     * dialog, sheet, toast) are elevated. The web's xs shadow is dropped on purpose (PREMIUM rule 7).
     */
    const val HasShadow: Boolean = false

    /**
     * The look of the enabled button of [variant]. Pressed is one surface step from the resting fill (a touch
     * screen has no hover, so the press is the only step the user sees). A disabled button uses [colors] with
     * `enabled = false`: a subtle fill and the disabled text, never a faded copy of the variant.
     */
    fun look(variant: MonarchButtonVariant, colors: MonarchColors): MonarchButtonColors = when (variant) {
        MonarchButtonVariant.Primary -> MonarchButtonColors(
            colors.accentSolid, colors.accentOnSolid, null, colors.accentSolidHover, colors.accentSolidActive,
        )
        MonarchButtonVariant.Secondary -> MonarchButtonColors(
            colors.bgSurface, colors.textDefault, colors.borderDefault, colors.bgMuted, colors.bgMuted,
        )
        MonarchButtonVariant.Ghost -> MonarchButtonColors(
            Color.Transparent, colors.textDefault, null, colors.bgMuted, colors.bgMuted,
        )
        MonarchButtonVariant.Danger -> MonarchButtonColors(
            colors.dangerSolid, colors.dangerOnSolid, null, colors.dangerSolidHover, colors.dangerSolidActive,
        )
        MonarchButtonVariant.DangerSubtle -> MonarchButtonColors(
            colors.dangerSubtle, colors.dangerStrong, colors.dangerBorder,
            colors.dangerSubtleHover, colors.dangerSubtleHover,
        )
        MonarchButtonVariant.Success -> MonarchButtonColors(
            colors.successSolid, colors.successOnSolid, null, colors.successSolidHover, colors.successSolidActive,
        )
        MonarchButtonVariant.Warning -> MonarchButtonColors(
            colors.warningSolid, colors.warningOnSolid, null, colors.warningSolidHover, colors.warningSolidActive,
        )
        MonarchButtonVariant.Link -> MonarchButtonColors(Color.Transparent, colors.textLink, null)
        // The inverse pair has no hover or active roles: the step is a quarter of the way to the text colour.
        MonarchButtonVariant.Inverse -> MonarchButtonColors(
            colors.bgInverse,
            colors.textInverse,
            null,
            lerp(colors.bgInverse, colors.textInverse, InverseHoverStep),
            lerp(colors.bgInverse, colors.textInverse, InverseActiveStep),
        )
        MonarchButtonVariant.Ai -> MonarchButtonColors(
            colors.aiSolid, colors.aiOnSolid, null, colors.aiSolidHover, colors.aiSolidActive,
        )
    }

    /** The pale accent fill of a disabled primary button: the accent mixed into the surface. */
    internal fun disabledPrimaryFill(colors: MonarchColors): Color =
        lerp(colors.bgSurface, colors.accentSolid, DisabledPrimaryAlpha)

    /** How much accent the disabled primary fill holds. */
    private const val DisabledPrimaryAlpha: Float = 0.45f

    private const val InverseHoverStep: Float = 0.12f
    private const val InverseActiveStep: Float = 0.2f

    /**
     * The colours of the button: [look] when [enabled], else the flat disabled roles (a subtle fill and the
     * disabled text; the outlined variant keeps a subtle outline). PREMIUM rule 10: disabled is muted text on a
     * subtle fill, not a half-transparent copy of the variant.
     */
    fun colors(variant: MonarchButtonVariant, enabled: Boolean, colors: MonarchColors): MonarchButtonColors =
        if (enabled) look(variant, colors) else disabledColors(variant, colors)

    private fun disabledColors(variant: MonarchButtonVariant, colors: MonarchColors) = when (variant) {
        // The web's disabled primary is the accent paled, not grey: the same hue at a lower strength.
        MonarchButtonVariant.Primary ->
            MonarchButtonColors(disabledPrimaryFill(colors), colors.accentOnSolid, null)
        // Likewise the disabled danger button is the red paled, not grey.
        MonarchButtonVariant.Danger ->
            MonarchButtonColors(
                lerp(colors.bgSurface, colors.dangerSolid, DisabledPrimaryAlpha),
                colors.dangerOnSolid,
                null,
            )
        MonarchButtonVariant.DangerSubtle,
        MonarchButtonVariant.Success,
        MonarchButtonVariant.Warning,
        MonarchButtonVariant.Inverse,
        MonarchButtonVariant.Ai,
        -> MonarchButtonColors(colors.bgMuted, colors.textDisabled, null)
        MonarchButtonVariant.Secondary ->
            MonarchButtonColors(colors.bgSurface, colors.textDisabled, colors.borderSubtle)
        MonarchButtonVariant.Ghost, MonarchButtonVariant.Link ->
            MonarchButtonColors(Color.Transparent, colors.textDisabled, null)
    }

    fun border(colors: MonarchButtonColors): BorderStroke? = colors.border?.let { BorderStroke(BorderWidth, it) }
}
