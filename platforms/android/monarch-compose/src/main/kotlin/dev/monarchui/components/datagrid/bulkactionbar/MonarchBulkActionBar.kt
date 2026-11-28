package dev.monarchui.components.datagrid.bulkactionbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The bar shown while items are selected: the count, the actions for the
 * selection with a More menu for the rest, and Clear selection.
 *
 * Built by Monarch from a Material 3 `Surface`, `MonarchButton`s and a
 * `MonarchMenu`. A contextual `TopAppBar` or `BottomAppBar` has a
 * fixed height that cuts the count and the actions at large font sizes, so
 * the bar is a raised surface whose controls wrap onto more lines; place it
 * at the bottom of a `Scaffold` (its `bottomBar`) or inline above a list.
 * While [count] is 0 nothing is drawn and the bar takes no room. Every change
 * of the count is announced through a polite live region ("2 of 4
 * selected"), including when the bar appears, and "Selection cleared" when
 * it closes.
 *
 * @param count the number of selected items. The bar is shown while it is above 0.
 * @param onClear empties the selection.
 * @param modifier the modifier of the bar.
 * @param totalCount the number of items that could be selected, for "3 of 120 selected".
 * @param actions the actions. A destructive one uses the danger variant.
 * @param maxVisibleActions how many actions are buttons; the rest go into a More menu. A negative value
 * counts as 0.
 * @param busy blocks every action and Clear selection while the app runs one.
 * @param busyActionId the running action shows progress; the other actions, More and Clear selection are
 * disabled.
 * @param labels replacements for the built-in strings.
 * @param accessories extra controls after the actions, such as a picker for a property.
 */
@Composable
public fun MonarchBulkActionBar(
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Int? = null,
    actions: List<MonarchBulkAction> = emptyList(),
    maxVisibleActions: Int = 3,
    busy: Boolean = false,
    busyActionId: String? = null,
    labels: MonarchBulkActionBarLabels = MonarchBulkActionBarLabels(),
    accessories: (@Composable () -> Unit)? = null,
) {
    val selectedText = bulkActionBarSelectedText(count, totalCount, labels)
    val clearedText = monarchString(labels.cleared, R.string.monarch_bulkactionbar_cleared)
    val announcement = bulkActionBarAnnouncement(count, selectedText, clearedText)
    Box(modifier = modifier) {
        if (count > 0) {
            BulkActionBarContent(
                selectedText = selectedText,
                onClear = onClear,
                actions = actions,
                maxVisibleActions = maxVisibleActions,
                busy = busy,
                busyActionId = busyActionId,
                labels = labels,
                accessories = accessories,
            )
        }
        // Always present, so TalkBack announces the count when the bar appears and "cleared" when it goes.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Suppress("LongParameterList")
@Composable
private fun BulkActionBarContent(
    selectedText: String,
    onClear: () -> Unit,
    actions: List<MonarchBulkAction>,
    maxVisibleActions: Int,
    busy: Boolean,
    busyActionId: String?,
    labels: MonarchBulkActionBarLabels,
    accessories: (@Composable () -> Unit)?,
) {
    val tokens = MonarchBulkActionBarTokens
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val split = tokens.split(actions, maxVisibleActions)
    val blocked = tokens.blocked(busy, busyActionId)
    val barName = monarchString(labels.bar, R.string.monarch_bulkactionbar_bar)
    Surface(
        modifier = Modifier.fillMaxWidth().monarchShadow(MonarchTheme.elevation.presented, shape),
        shape = shape,
        color = tokens.container(colors),
        contentColor = colors.textDefault,
        border = BorderStroke(tokens.BorderWidth, tokens.borderColor(colors)),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
                .semantics {
                    isTraversalGroup = true
                    contentDescription = barName
                },
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            // The count and Clear selection share the first line; the actions wrap on the lines below.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.label,
                    color = tokens.countText(colors),
                )
                MonarchButton(
                    text = monarchString(labels.clear, R.string.monarch_bulkactionbar_clear),
                    onClick = onClear,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = !blocked,
                    leadingIcon = MonarchDefaultIcons.Close,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                split.visible.forEach { action ->
                    MonarchButton(
                        text = action.title,
                        onClick = action.onClick,
                        variant = tokens.variant(action),
                        size = MonarchButtonSize.Sm,
                        enabled = tokens.actionEnabled(action, busy, busyActionId),
                        loading = action.id == busyActionId,
                        leadingIcon = action.icon,
                    )
                }
                if (split.overflow.isNotEmpty()) {
                    BulkActionBarMore(
                        overflow = split.overflow,
                        label = monarchString(labels.more, R.string.monarch_bulkactionbar_more),
                        enabled = !blocked,
                        busy = busy,
                        busyActionId = busyActionId,
                    )
                }
                accessories?.invoke()
            }
        }
    }
}

/** The More button and the menu of the actions that are not buttons. */
@Composable
private fun BulkActionBarMore(
    overflow: List<MonarchBulkAction>,
    label: String,
    enabled: Boolean,
    busy: Boolean,
    busyActionId: String?,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MonarchButton(
            text = label,
            onClick = { expanded = true },
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            trailingIcon = MonarchDefaultIcons.ChevronDown,
        )
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            overflow.forEach { action ->
                MonarchMenuItem(
                    text = action.title,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                    enabled = MonarchBulkActionBarTokens.actionEnabled(action, busy, busyActionId),
                    destructive = action.destructive,
                    leadingIcon = action.icon,
                )
            }
        }
    }
}

@Composable
private fun bulkActionBarSelectedText(count: Int, total: Int?, labels: MonarchBulkActionBarLabels): String =
    labels.selected?.invoke(count, total) ?: if (total != null) {
        pluralStringResource(R.plurals.monarch_bulkactionbar_selected_of, count, count, total)
    } else {
        pluralStringResource(R.plurals.monarch_bulkactionbar_selected, count, count)
    }

/** The count announced last; it starts at 0 so that a bar that appears with a selection announces it. */
private class BulkActionBarSeen(var count: Int = 0)

/** The text of the polite live region after the count changes. */
@Composable
private fun bulkActionBarAnnouncement(count: Int, selectedText: String, clearedText: String): String {
    var announcement by remember { mutableStateOf("") }
    val seen = remember { BulkActionBarSeen() }
    val selected by rememberUpdatedState(selectedText)
    val cleared by rememberUpdatedState(clearedText)
    LaunchedEffect(count) {
        val text = when (MonarchBulkActionBarTokens.announcement(seen.count, count)) {
            BulkActionBarAnnouncement.Count -> selected
            BulkActionBarAnnouncement.Cleared -> cleared
            BulkActionBarAnnouncement.None -> null
        }
        seen.count = count
        if (text != null) {
            announcement = ""
            withFrameNanos { }
            announcement = text
        }
    }
    return announcement
}
