package com.rookhq.monarch.components.overlays.popover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchPopover, built from primitives and roles only. */
internal object MonarchPopoverTokens {
    /** The web's 24rem. */
    val MaxWidth: Dp = (MonarchTokens.Space.space24 * 4).dp

    /** The distance between the anchor and the panel: the web's sideOffset of 6. */
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The smallest distance between the panel and the edges of the window: the web's 1rem. */
    val WindowMargin: Dp = MonarchTokens.Space.space4.dp

    /** Transparent room around the panel inside the popup window, so the large shadow is not cut off. */
    val ShadowRoom: Dp = MonarchTokens.Space.space6.dp

    val ContentPadding: PaddingValues = PaddingValues(MonarchTokens.Space.space4.dp)
    val ContentGap: Dp = MonarchTokens.Space.space3.dp

    /** The size the panel grows from when it opens, as on the web. */
    const val ENTER_SCALE: Float = 0.98f

    /**
     * The panel: the raised background, the default text and a subtle
     * border, which becomes the strong border in the high-contrast themes
     * (the web uses the system text colour there).
     */
    fun colors(colors: MonarchColors, highContrast: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgRaised,
        content = colors.textDefault,
        border = if (highContrast) colors.borderStrong else colors.borderSubtle,
    )
}

/**
 * Places a popover next to its anchor: aligned with the start edge of the
 * anchor (the end edge flips in right to left), on the preferred side when
 * the panel fits there and on the other side otherwise, and moved inside the
 * window with [margin] to spare.
 *
 * @property side the preferred side.
 * @property gap the distance from the anchor, in pixels.
 * @property margin the distance from the window edges, in pixels.
 * @property inset the transparent room around the panel inside the popup, in pixels; the panel, not the
 * popup, is placed.
 * @property anchorX when set, the panel is centred on this point instead of aligned with the start edge of the
 * anchor: pixels from the left edge of the anchor, in the physical direction (already mirrored in right to left).
 */
internal data class MonarchPopoverPositionProvider(
    val side: MonarchPopoverSide,
    val gap: Int,
    val margin: Int,
    val inset: Int = 0,
    val anchorX: Int? = null,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val panel = IntSize(
            (popupContentSize.width - inset * 2).coerceAtLeast(0),
            (popupContentSize.height - inset * 2).coerceAtLeast(0),
        )
        val placed = placePanel(anchorBounds, windowSize, layoutDirection, panel)
        return IntOffset(placed.x - inset, placed.y - inset)
    }

    private fun placePanel(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val start = if (anchorX != null) {
            anchorBounds.left + anchorX - popupContentSize.width / 2
        } else if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = start.coerceIn(margin, maxOf(margin, windowSize.width - popupContentSize.width - margin))

        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - popupContentSize.height
        val fitsBelow = below + popupContentSize.height <= windowSize.height - margin
        val fitsAbove = above >= margin
        val y = when (side) {
            MonarchPopoverSide.Bottom -> if (fitsBelow || !fitsAbove) below else above
            MonarchPopoverSide.Top -> if (fitsAbove || !fitsBelow) above else below
        }
        return IntOffset(x, y.coerceIn(0, maxOf(0, windowSize.height - popupContentSize.height)))
    }
}
