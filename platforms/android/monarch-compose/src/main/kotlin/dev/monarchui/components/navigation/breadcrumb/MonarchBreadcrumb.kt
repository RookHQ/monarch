package dev.monarchui.components.navigation.breadcrumb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * A trail of the levels above the current screen, the current level last,
 * with the middle levels of a long trail collapsed into a menu.
 *
 * For deep hierarchies such as folders. Ordinary screens use the top app
 * bar's title and back button instead.
 *
 * Built by Monarch: a `LazyRow` of [MonarchLink]s in the subtle style,
 * separated by a mirrored angle mark, with a ghost [MonarchButton] that opens
 * a `MonarchMenu` of the collapsed levels. The trail stays on one
 * line: when it is wider than the space it scrolls sideways, and it starts
 * scrolled to the current level.
 *
 * The component does not navigate: [onItemClick] receives the level, and the
 * app moves its back stack or loads the folder.
 *
 * @param items the levels from the root to the current screen.
 * @param onItemClick goes to the chosen level. Not called for the last level or for a level that is not linked.
 * @param modifier the modifier of the trail.
 * @param maxVisible the most levels shown. Above it the first level stays, the middle levels collapse into a
 * menu and the last `maxVisible - 2` stay. Values below 3 count as 3.
 * @param enabled whether the links and the menu can be used. When not, they use the disabled text colour.
 * @param separator replaces the separator between levels. It is hidden from TalkBack; a directional glyph
 * should mirror in right-to-left layouts.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchBreadcrumb(
    items: List<MonarchBreadcrumbItem>,
    onItemClick: (MonarchBreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4,
    enabled: Boolean = true,
    separator: (@Composable () -> Unit)? = null,
    labels: MonarchBreadcrumbLabels = MonarchBreadcrumbLabels(),
) {
    val tokens = MonarchBreadcrumbTokens
    val visible = tokens.visible(items.size, maxVisible)
    val hidden = tokens.hidden(items.size, maxVisible).map { items[it] }
    val label = monarchString(labels.label, R.string.monarch_breadcrumb_label)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (visible.size - 1).coerceAtLeast(0))
    ProvideTextStyle(MonarchTheme.typography.label) {
        LazyRow(
            modifier = modifier.semantics {
                contentDescription = label
                isTraversalGroup = true
            },
            state = state,
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(visible) { position, index ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index == tokens.MORE) {
                        BreadcrumbMore(hidden, enabled, onItemClick, labels)
                    } else {
                        BreadcrumbLevel(items[index], index, items.size, enabled, onItemClick, labels)
                    }
                    if (position < visible.lastIndex) BreadcrumbSeparator(separator)
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbLevel(
    item: MonarchBreadcrumbItem,
    index: Int,
    count: Int,
    enabled: Boolean,
    onItemClick: (MonarchBreadcrumbItem) -> Unit,
    labels: MonarchBreadcrumbLabels,
) {
    val current = index == count - 1
    if (MonarchBreadcrumbTokens.isLink(index, count, item.linked)) {
        // A short level ("Drive") is narrower than its 48dp target, and the link draws its text at the start of
        // that space, which left a wide gap before the next separator. The wrapper reports the text's width to
        // the row, so every level sits the same 4dp from its separator; the link keeps its 48dp target, which
        // overhangs the separator after it.
        val measurer = rememberTextMeasurer()
        val style = LocalTextStyle.current
        val textWidth = remember(item.title, style) { measurer.measure(item.title, style, maxLines = 1).size.width }
        Box(Modifier.shortLevelOverhang(textWidth)) {
            MonarchLink(
                text = item.title,
                onClick = { onItemClick(item) },
                // The link's text takes a weight in its row; a fixed width keeps it whole in the scrolling row.
                modifier = Modifier.width(IntrinsicSize.Max),
                variant = MonarchLinkVariant.Subtle,
                enabled = enabled,
            )
        }
    } else {
        val currentLabel = monarchString(labels.current, R.string.monarch_breadcrumb_current)
        Text(
            text = item.title,
            modifier = if (current) Modifier.semantics { stateDescription = currentLabel } else Modifier,
            color = MonarchBreadcrumbTokens.textColor(current, enabled, MonarchTheme.colors),
            fontWeight = if (current) FontWeight(MonarchTokens.FontWeight.medium) else null,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun BreadcrumbMore(
    hidden: List<MonarchBreadcrumbItem>,
    enabled: Boolean,
    onItemClick: (MonarchBreadcrumbItem) -> Unit,
    labels: MonarchBreadcrumbLabels,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val name = labels.more?.invoke(hidden.size)
        ?: pluralStringResource(R.plurals.monarch_breadcrumb_more, hidden.size, hidden.size)
    Box(Modifier.moreOverhang()) {
        // The ellipsis is shown; TalkBack reads the name ("2 more pages") instead.
        MonarchButton(
            text = MonarchBreadcrumbTokens.ELLIPSIS,
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            hidden.forEach { item ->
                MonarchMenuItem(
                    text = item.title,
                    onClick = {
                        expanded = false
                        onItemClick(item)
                    },
                    enabled = item.linked,
                )
            }
        }
    }
}

private const val MoreOverhangMaxShare = 4

/**
 * The ellipsis button reserves 48dp of width for its touch target, which would open wide gaps around it. It
 * reports a narrower width to the row, so the separators sit as close to it as to the links; the button
 * itself keeps its 48dp target, which Compose still delivers taps in.
 */
private fun Modifier.moreOverhang(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overhang = MonarchBreadcrumbTokens.MoreOverhang.roundToPx()
        .coerceAtMost(placeable.width / MoreOverhangMaxShare)
    layout(placeable.width - 2 * overhang, placeable.height) { placeable.placeRelative(-overhang, 0) }
}

/** Reports [textWidth] when the level is only as wide as its touch target, so the target overhangs at the end. */
private fun Modifier.shortLevelOverhang(textWidth: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val target = MonarchTouchTarget.Minimum.roundToPx()
    val width = if (placeable.width <= target && textWidth < placeable.width) textWidth else placeable.width
    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
}

@Composable
private fun BreadcrumbSeparator(separator: (@Composable () -> Unit)?) {
    Box(Modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (separator != null) {
            separator()
        } else {
            Text(
                text = MonarchBreadcrumbTokens.SEPARATOR,
                color = MonarchBreadcrumbTokens.separatorColor(MonarchTheme.colors),
            )
        }
    }
}
