package com.rookhq.monarch.components.overlays.dialog

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchElevation
import com.rookhq.monarch.foundation.MonarchShadow
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * Component tokens of MonarchDialog, from the web's `dialog.css`: a panel up
 * to 512dp wide with 16dp from the window edges, 16dp corners, the raised
 * background, a subtle border, 20dp padding, an 18/600 title and a 14
 * description in the muted text colour.
 */
internal object MonarchDialogTokens {
    /** The web's `min(32rem, 100vw - 2rem)`. */
    val MaxWidth: Dp = (MonarchTokens.Space.space16 * 8).dp

    /** The smallest distance between the panel and the window edges. */
    val WindowMargin: Dp = MonarchTokens.Space.space2.dp

    /** The padding of the panel. */
    val Padding: Dp = MonarchTokens.Space.space5.dp

    /** Between the title and the description. */
    val HeaderGap: Dp = MonarchTokens.Space.space1_5.dp

    /** Between the header, the content and the actions. */
    val SectionGap: Dp = MonarchTokens.Space.space5.dp

    /** Between two actions. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The panel colours: the raised background with a subtle border, the
     * strong border in the high-contrast themes.
     */
    fun colors(colors: MonarchColors, highContrast: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgRaised,
        content = colors.textDefault,
        border = if (highContrast) colors.borderStrong else colors.borderSubtle,
    )

    /**
     * The one soft shadow of a presented surface, in the light themes only. A
     * dark theme separates the panel by its lighter surface step and border.
     */
    fun shadow(elevation: MonarchElevation, dark: Boolean): MonarchShadow? = if (dark) null else elevation.lg

    /** The description: 14 regular with the normal line height. */
    fun descriptionStyle(typography: MonarchTypography): TextStyle = typography.body.copy(
        fontSize = MonarchTokens.FontSize.sm.sp,
        lineHeight = MonarchTokens.LineHeight.normal.em,
    )

    /**
     * How much of the window a dim of the scrim role covers: the alpha of
     * the role, since a dialog window dims with black.
     */
    fun dimAmount(colors: MonarchColors): Float = colors.overlayScrim.alpha
}

/**
 * Keeps [margin] between the element and every edge of the space it is given,
 * without adding the margin to its size, so a tap beside the panel is still
 * outside it.
 */
internal fun Modifier.monarchWindowMargin(margin: Dp): Modifier = layout { measurable, constraints ->
    val inset = margin.roundToPx() * 2
    val inner = constraints.offset(horizontal = -inset, vertical = -inset)
    val placeable = measurable.measure(inner)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
