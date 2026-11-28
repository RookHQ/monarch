package dev.monarchui.components.feedback.emptystate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * What will appear in an empty view, and the action that creates it.
 *
 * Built by Monarch: Material 3 has no empty state. A centred column with an
 * icon tile, a heading, a message and a row of actions that wraps at large
 * font sizes. For a filtered view it adds a "Clear filters" button before the
 * app's actions.
 *
 * @param title what is missing, for example "No invoices yet". Marked as a heading for TalkBack.
 * @param modifier the modifier of the empty state.
 * @param kind why the view is empty: the colour of the icon tile and the clear-filters button.
 * @param size the spacing and the type sizes.
 * @param message what will appear here and how to create it.
 * @param icon an icon in the tinted tile, for example a `MonarchIcon` with a `null` content description. It
 * is hidden from TalkBack. Monarch bundles no icons, so there is no tile without it.
 * @param tip a short hint under the actions, in small muted text.
 * @param onClearFilters with [kind] `Filtered`, adds a "Clear filters" button. The app resets its filters.
 * @param actions the buttons, for example a primary `MonarchButton` that creates the first item.
 * @param clearFiltersLabel the label of the clear-filters button. `null` uses the built-in string
 * `monarch_emptystate_clear_filters`.
 */
@Composable
public fun MonarchEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    kind: MonarchEmptyStateKind = MonarchEmptyStateKind.FirstRun,
    size: MonarchEmptyStateSize = MonarchEmptyStateSize.Md,
    message: String? = null,
    icon: (@Composable () -> Unit)? = null,
    tip: String? = null,
    onClearFilters: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    clearFiltersLabel: String? = null,
) {
    val clear = if (kind == MonarchEmptyStateKind.Filtered) onClearFilters else null
    val clearLabel = monarchString(clearFiltersLabel, R.string.monarch_emptystate_clear_filters)
    val allActions: (@Composable () -> Unit)? = if (clear != null || actions != null) {
        {
            if (clear != null) MonarchButton(text = clearLabel, onClick = clear)
            actions?.invoke()
        }
    } else {
        null
    }
    EmptyStateLayout(
        title = title,
        modifier = modifier,
        size = size,
        tileColors = MonarchEmptyStateTokens.tileColors(kind, MonarchTheme.colors),
        message = message,
        icon = icon,
        tip = tip,
        actions = allActions,
    )
}
