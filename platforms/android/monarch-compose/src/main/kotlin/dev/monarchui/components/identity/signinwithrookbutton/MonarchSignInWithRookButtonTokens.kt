package dev.monarchui.components.identity.signinwithrookbutton

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchContrast
import dev.monarchui.theme.MonarchScheme
import dev.monarchui.tokens.MonarchTokens

/** The colours of the button in one state. */
@Immutable
internal data class MonarchSignInWithRookButtonColors(val container: Color, val content: Color, val border: Color?)

/** Component tokens of MonarchSignInWithRookButton, built from primitives and roles only. */
internal object MonarchSignInWithRookButtonTokens {
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val SpinnerStrokeWidth: Dp = MonarchTokens.Space.space0_5.dp

    /**
     * The scheme the button is drawn in: light for `Light` and `Neutral`,
     * dark for `Dark`, and `null` (the screen's theme) for `Auto`.
     */
    fun pinnedScheme(variant: MonarchSignInWithRookButtonVariant): MonarchScheme? = when (variant) {
        MonarchSignInWithRookButtonVariant.Light, MonarchSignInWithRookButtonVariant.Neutral -> MonarchScheme.Light
        MonarchSignInWithRookButtonVariant.Dark -> MonarchScheme.Dark
        MonarchSignInWithRookButtonVariant.Auto -> null
    }

    /** The contrast of a pinned scheme: high contrast when the screen uses a high-contrast theme. */
    fun pinnedContrast(highContrast: Boolean): MonarchContrast =
        if (highContrast) MonarchContrast.More else MonarchContrast.Standard

    /** The colours, from the roles of the theme the button is drawn in. */
    fun colors(
        variant: MonarchSignInWithRookButtonVariant,
        enabled: Boolean,
        pressed: Boolean,
        colors: MonarchColors,
    ): MonarchSignInWithRookButtonColors {
        val neutral = variant == MonarchSignInWithRookButtonVariant.Neutral
        return when {
            !enabled -> MonarchSignInWithRookButtonColors(
                container = colors.bgSubtle,
                content = colors.textDisabled,
                border = if (neutral) null else colors.borderSubtle,
            )
            neutral -> MonarchSignInWithRookButtonColors(
                container = if (pressed) colors.bgEmphasis else colors.bgMuted,
                content = colors.textDefault,
                border = null,
            )
            else -> MonarchSignInWithRookButtonColors(
                container = if (pressed) colors.bgMuted else colors.bgSurface,
                content = colors.textDefault,
                border = colors.borderStrong,
            )
        }
    }

    fun minHeight(size: MonarchSignInWithRookButtonSize): Dp = when (size) {
        MonarchSignInWithRookButtonSize.Sm -> MonarchTokens.Control.sm.dp
        MonarchSignInWithRookButtonSize.Md -> MonarchTokens.Control.lg.dp
        MonarchSignInWithRookButtonSize.Lg -> MonarchTokens.Control.xl.dp
    }

    fun markSize(size: MonarchSignInWithRookButtonSize): Dp = when (size) {
        MonarchSignInWithRookButtonSize.Sm -> MonarchTokens.Space.space4.dp
        MonarchSignInWithRookButtonSize.Md -> MonarchTokens.Space.space5.dp
        MonarchSignInWithRookButtonSize.Lg -> MonarchTokens.Space.space6.dp
    }

    fun gap(size: MonarchSignInWithRookButtonSize): Dp = when (size) {
        MonarchSignInWithRookButtonSize.Sm -> MonarchTokens.Space.space2.dp
        MonarchSignInWithRookButtonSize.Md -> MonarchTokens.Space.space2_5.dp
        MonarchSignInWithRookButtonSize.Lg -> MonarchTokens.Space.space3.dp
    }

    /** Less padding before the mark than after the label, as on the web. */
    fun contentPadding(size: MonarchSignInWithRookButtonSize): PaddingValues {
        val vertical = MonarchTokens.Space.space1.dp
        return when (size) {
            MonarchSignInWithRookButtonSize.Sm ->
                PaddingValues(MonarchTokens.Space.space2.dp, vertical, MonarchTokens.Space.space3.dp, vertical)
            MonarchSignInWithRookButtonSize.Md ->
                PaddingValues(MonarchTokens.Space.space3.dp, vertical, MonarchTokens.Space.space4.dp, vertical)
            MonarchSignInWithRookButtonSize.Lg ->
                PaddingValues(MonarchTokens.Space.space3.dp, vertical, MonarchTokens.Space.space5.dp, vertical)
        }
    }

    fun textStyle(size: MonarchSignInWithRookButtonSize, typography: MonarchTypography): TextStyle {
        val base = when (size) {
            MonarchSignInWithRookButtonSize.Sm -> typography.caption
            MonarchSignInWithRookButtonSize.Md -> typography.label
            MonarchSignInWithRookButtonSize.Lg -> typography.body
        }
        return base.copy(fontWeight = FontWeight(MonarchTokens.FontWeight.medium))
    }
}
