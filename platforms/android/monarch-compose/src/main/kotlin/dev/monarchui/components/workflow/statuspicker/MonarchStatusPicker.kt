package dev.monarchui.components.workflow.statuspicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.overlays.menu.MonarchMenuLabel
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString

/**
 * A menu button that shows and changes the workflow status of an issue, a
 * task or a ticket. The team's own statuses are grouped in the menu under
 * five fixed categories: backlog, not started, in progress, completed and
 * cancelled.
 *
 * Built by Monarch from `MonarchButton` (or a `MonarchIconButton` when
 * the name is hidden) and a `MonarchMenu` with a heading per
 * category. Every category has its own shape ([MonarchStatusGlyph]) as well as
 * its own colour, and every status its own name, so the status never depends
 * on colour alone. The trigger is read as "Status: In progress"; in the menu
 * the current status is selected and marked with a check.
 *
 * @param statuses the statuses to choose from. Grouped by category in the menu, in their given order within a group.
 * @param value the id of the current status; `null` or an id not in [statuses] shows "No status".
 * @param onValueChange called with the id of the status the person chooses.
 * @param modifier the modifier of the trigger.
 * @param size the size of the trigger. The touch target is 48dp at both sizes.
 * @param showLabel shows the status's name next to its glyph. `false` gives a glyph-only trigger, still named for
 * TalkBack.
 * @param enabled whether the menu can be opened.
 * @param trailingIcon a decorative icon after the name, such as a down chevron. Not shown without the label.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the trigger's press and focus interactions.
 * @param initiallyOpen opens the list when the picker first appears, as the web example does.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchStatusPicker(
    statuses: List<MonarchStatusOption>,
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchStatusPickerSize = MonarchStatusPickerSize.Md,
    showLabel: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchStatusPickerLabels = MonarchStatusPickerLabels(),
    interactionSource: MutableInteractionSource? = null,
    initiallyOpen: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    val current = statuses.firstOrNull { it.id == value }
    val name = current?.name ?: monarchString(labels.none, R.string.monarch_statuspicker_none)
    val category = current?.category ?: MonarchStatusCategory.Backlog
    val description = labels.trigger?.invoke(name) ?: stringResource(R.string.monarch_statuspicker_trigger, name)
    val described = Modifier.semantics {
        contentDescription = description
        role = Role.DropdownList
    }
    Box(modifier = modifier) {
        if (showLabel) {
            MonarchButton(
                onClick = { expanded = true },
                modifier = described,
                size = if (size == MonarchStatusPickerSize.Sm) MonarchButtonSize.Sm else MonarchButtonSize.Md,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                MonarchStatusGlyph(category)
                Spacer(Modifier.width(MonarchStatusPickerTokens.Gap))
                Text(name)
                trailingIcon?.let {
                    Spacer(Modifier.width(MonarchStatusPickerTokens.Gap))
                    it()
                }
            }
        } else {
            MonarchIconButton(
                label = description,
                onClick = { expanded = true },
                modifier = Modifier.semantics { role = Role.DropdownList },
                enabled = enabled,
                interactionSource = interactionSource,
            ) { MonarchStatusGlyph(category) }
        }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            val headings = categoryNames(labels)
            MonarchStatusPickerTokens.groups(statuses).forEachIndexed { index, (group, members) ->
                if (index > 0) MonarchMenuDivider()
                MonarchMenuLabel(text = headings.getValue(group))
                members.forEach { option ->
                    val chosen = option.id == current?.id
                    MonarchMenuItem(
                        text = option.name,
                        onClick = {
                            expanded = false
                            if (!chosen) onValueChange(option.id)
                        },
                        selected = chosen,
                        leadingIcon = { MonarchStatusGlyph(option.category) },
                    )
                }
            }
        }
    }
}

/** The menu heading of every category, built in or replaced. */
@Composable
private fun categoryNames(labels: MonarchStatusPickerLabels): Map<MonarchStatusCategory, String> = mapOf(
    MonarchStatusCategory.Backlog to monarchString(labels.backlog, R.string.monarch_statuspicker_backlog),
    MonarchStatusCategory.Unstarted to monarchString(labels.unstarted, R.string.monarch_statuspicker_unstarted),
    MonarchStatusCategory.Started to monarchString(labels.started, R.string.monarch_statuspicker_started),
    MonarchStatusCategory.Completed to monarchString(labels.completed, R.string.monarch_statuspicker_completed),
    MonarchStatusCategory.Cancelled to monarchString(labels.cancelled, R.string.monarch_statuspicker_cancelled),
)
