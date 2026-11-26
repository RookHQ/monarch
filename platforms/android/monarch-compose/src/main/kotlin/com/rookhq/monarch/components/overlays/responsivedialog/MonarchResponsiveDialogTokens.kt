package com.rookhq.monarch.components.overlays.responsivedialog

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchResponsiveDialog. */
internal object MonarchResponsiveDialogTokens {
    /** The gap between the content and the actions of the sheet. */
    val SectionGap: Dp = MonarchTokens.Space.space5.dp

    /** The gap between the actions. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest window width, in dp, that shows the dialog instead of the sheet. */
    fun minDialogWidthDp(breakpoint: MonarchResponsiveDialogBreakpoint): Int = when (breakpoint) {
        MonarchResponsiveDialogBreakpoint.Medium -> WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        MonarchResponsiveDialogBreakpoint.Expanded -> WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    }

    /** Whether the window class is too narrow for the dialog, so the sheet is shown. */
    fun usesSheet(windowSizeClass: WindowSizeClass, breakpoint: MonarchResponsiveDialogBreakpoint): Boolean =
        !windowSizeClass.isWidthAtLeastBreakpoint(minDialogWidthDp(breakpoint))
}
