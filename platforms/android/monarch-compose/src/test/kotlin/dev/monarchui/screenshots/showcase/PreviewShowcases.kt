package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Curated docs previews: for a primitive, a small composable that shows it in a few representative states with real
 * copy and real Material icons, laid out as one tidy column. A showcase is written against the public API of the
 * component only (parameter names, never internals), so it survives a restyle. A component with no showcase is shown
 * through its primary inventory example, which is right for rich components and for recipes.
 *
 * Rules: 3 to 8 states, nothing wider than the frame (rows wrap), no labels or dividers of the harness.
 */
internal object PreviewShowcases {
    /** The showcase of every component id that has one. */
    val all: Map<String, @Composable () -> Unit> by lazy {
        actionShowcases + fieldShowcases + displayShowcases + feedbackShowcases + navigationShowcases +
            selectionShowcases + overlayShowcases
    }
}

/** The column every showcase sits in: full width, 16dp between groups. */
@Composable
internal fun ShowcaseColumn(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

/** A row that wraps to the next line instead of growing wider than the frame. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Wrap(
    spacing: Arrangement.HorizontalOrVertical,
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = spacing,
        verticalArrangement = spacing,
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
