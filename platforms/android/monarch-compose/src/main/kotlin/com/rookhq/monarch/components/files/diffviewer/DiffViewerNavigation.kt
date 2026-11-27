package com.rookhq.monarch.components.files.diffviewer

import androidx.compose.ui.unit.Dp

/** Whether the viewer is wide enough for two columns. An unspecified width never is. */
internal fun diffViewerSplitFits(width: Dp): Boolean = width >= MonarchDiffViewerTokens.SplitMinWidth

/** The layout when the app does not own it: side by side when it fits, unified otherwise. */
internal fun diffViewerDefaultMode(width: Dp): MonarchDiffViewerMode =
    if (diffViewerSplitFits(width)) MonarchDiffViewerMode.Split else MonarchDiffViewerMode.Unified

/** The change after [position] (or the first when nothing is current), wrapping round. */
internal fun diffViewerNext(position: Int, hunks: Int): Int = if (hunks <= 0) -1 else (position + 1).mod(hunks)

/** The change before [position] (or the last when nothing or the first is current), wrapping round. */
internal fun diffViewerPrevious(position: Int, hunks: Int): Int = when {
    hunks <= 0 -> -1
    position <= 0 -> hunks - 1
    else -> position - 1
}

/** The marker shown before a line. */
internal fun diffViewerMarker(kind: MonarchDiffViewerLineKind): String = when (kind) {
    MonarchDiffViewerLineKind.Added -> "+"
    MonarchDiffViewerLineKind.Removed -> "\u2212"
    MonarchDiffViewerLineKind.Same -> ""
}
