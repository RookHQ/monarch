package dev.monarchui.components.files.diffviewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and colour rules of MonarchDiffViewer, built from primitives and roles only. */
internal object MonarchDiffViewerTokens {
    /** From this width the viewer shows side by side by default and offers the layout switch (the `md` breakpoint). */
    val SplitMinWidth: Dp = MonarchTokens.Breakpoint.md.dp
    val HeaderGap: Dp = MonarchTokens.Space.space2.dp
    val HeaderPadding: Dp = MonarchTokens.Space.space4.dp
    val CellPadding: Dp = MonarchTokens.Space.space2.dp
    val RowVerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val CurrentBarWidth: Dp = MonarchTokens.Space.space1.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The fill of a line or a side: subtle success when added, subtle danger when removed, subtle when empty. */
    fun lineFill(kind: MonarchDiffViewerLineKind?, colors: MonarchColors): Color = when (kind) {
        MonarchDiffViewerLineKind.Added -> colors.successSubtle
        MonarchDiffViewerLineKind.Removed -> colors.dangerSubtle
        MonarchDiffViewerLineKind.Same -> Color.Transparent
        null -> colors.bgSubtle
    }

    /** The fill of a changed word: the success or danger border colour. */
    fun wordFill(kind: MonarchDiffViewerLineKind, colors: MonarchColors): Color = when (kind) {
        MonarchDiffViewerLineKind.Added -> colors.successBorder
        MonarchDiffViewerLineKind.Removed -> colors.dangerBorder
        MonarchDiffViewerLineKind.Same -> Color.Transparent
    }

    /** The + and − markers and the totals: the strong success and danger roles. */
    fun markerColor(kind: MonarchDiffViewerLineKind, colors: MonarchColors): Color = when (kind) {
        MonarchDiffViewerLineKind.Added -> colors.successStrong
        MonarchDiffViewerLineKind.Removed -> colors.dangerStrong
        MonarchDiffViewerLineKind.Same -> colors.textSubtle
    }

    /** Line numbers use the subtle text role. */
    fun numberColor(colors: MonarchColors): Color = colors.textSubtle

    /** The bar at the start of the current change. */
    fun currentBar(colors: MonarchColors): Color = colors.accentSolid

    /** The outline of the viewer. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** The background of the viewer. */
    fun container(colors: MonarchColors): Color = colors.bgSurface

    /** The text of the lines. */
    fun text(colors: MonarchColors): Color = colors.textDefault

    /** "No changes." uses the muted text role. */
    fun emptyText(colors: MonarchColors): Color = colors.textMuted
}
