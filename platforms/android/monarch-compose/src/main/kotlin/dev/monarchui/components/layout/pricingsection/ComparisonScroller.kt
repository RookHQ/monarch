package dev.monarchui.components.layout.pricingsection

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.max

/**
 * Lays a comparison grid of [columns] equal columns (the feature column and one per option) at least
 * `MonarchPricingSectionTokens.ComparisonColumn` wide each, scaled by the font scale, and scrolls it sideways when
 * that is wider than the screen, as the web's comparison tables do, so no word is broken across lines.
 */
@Composable
internal fun ComparisonScroller(columns: Int, content: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val needed = MonarchPricingSectionTokens.ComparisonColumn * columns * fontScale
        val width = max(maxWidth, needed)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(width)) { content() }
        }
    }
}
