package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonTokens

/**
 * Pulls a ghost (text) button, or a row that starts with one, left by the start padding of its label, so the label
 * lines up with the text above it, the way a text button sits on the text edge in iOS and Material. The reported
 * width shrinks by the same amount, so the next control keeps its normal gap. The button keeps its full 48dp
 * height; only the empty strip at its outer edge falls outside the parent.
 */
internal fun Modifier.monarchGhostStartAlign(size: MonarchButtonSize = MonarchButtonSize.Sm): Modifier =
    layout { measurable, constraints ->
        val pull = MonarchButtonTokens.contentPadding(size).calculateStartPadding(layoutDirection).roundToPx()
        val placeable = measurable.measure(
            Constraints(
                minWidth = 0,
                maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + pull else constraints.maxWidth,
                minHeight = constraints.minHeight,
                maxHeight = constraints.maxHeight,
            ),
        )
        val width = (placeable.width - pull).coerceAtLeast(0)
        layout(width, placeable.height) {
            // The pull is toward the start edge, so in right-to-left layouts the content is placed pull to the right.
            val x = if (layoutDirection == LayoutDirection.Ltr) -pull else 0
            placeable.place(x, 0)
        }
    }
