package dev.monarchui.components.workflow.prioritypicker

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.theme.MonarchTheme

/**
 * A menu button that shows and changes how urgent an issue is, on the
 * urgent, high, medium, low and no-priority scale.
 *
 * Built by Monarch from `MonarchButton` (or a `MonarchIconButton` when
 * the name is hidden) and a `MonarchMenu`. Every level has its
 * own shape ([MonarchPriorityGlyph]) as well as its own colour and its name,
 * so the priority never depends on colour alone. The trigger is read as
 * "Priority: High"; in the menu the current level is selected and marked
 * with a check.
 *
 * @param value the current level; `null` shows [MonarchPriorityLevel.None].
 * @param onValueChange called with the level the person chooses.
 * @param modifier the modifier of the trigger.
 * @param size the size of the trigger. The touch target is 48dp at both sizes.
 * @param levels the levels offered, in menu order.
 * @param showLabel shows the level's name next to its shape. `false` gives a shape-only trigger, still named for
 * TalkBack.
 * @param enabled whether the menu can be opened.
 * @param trailingIcon a decorative icon after the name, such as a down chevron. Not shown without the label.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the trigger's press and focus interactions.
 * @param initiallyOpen opens the menu when the trigger first appears, as the web example does.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchPriorityPicker(
    value: MonarchPriorityLevel?,
    onValueChange: (MonarchPriorityLevel) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchPriorityPickerSize = MonarchPriorityPickerSize.Md,
    levels: List<MonarchPriorityLevel> = MonarchPriorityLevel.entries,
    showLabel: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    labels: MonarchPriorityPickerLabels = MonarchPriorityPickerLabels(),
    interactionSource: MutableInteractionSource? = null,
    initiallyOpen: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    val current = value ?: MonarchPriorityLevel.None
    val names = levelNames(labels)
    val name = names.getValue(current)
    val description = labels.trigger?.invoke(name) ?: stringResource(R.string.monarch_prioritypicker_trigger, name)
    val described = Modifier.semantics {
        contentDescription = description
        role = Role.DropdownList
    }
    Box(modifier = modifier) {
        if (showLabel) {
            MonarchButton(
                onClick = { expanded = true },
                modifier = described,
                size = if (size == MonarchPriorityPickerSize.Sm) MonarchButtonSize.Sm else MonarchButtonSize.Md,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                MonarchPriorityGlyph(current)
                Spacer(Modifier.width(MonarchPriorityPickerTokens.Gap))
                Text(name)
                trailingIcon?.let {
                    Spacer(Modifier.width(MonarchPriorityPickerTokens.Gap))
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
            ) { MonarchPriorityGlyph(current) }
        }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            levels.forEach { level ->
                val chosen = level == current
                MonarchMenuItem(
                    onClick = {
                        expanded = false
                        if (!chosen) onValueChange(level)
                    },
                    selected = chosen,
                    leadingIcon = { MonarchPriorityGlyph(level) },
                    // A dot marks the current level, as on the web.
                    selectionMark = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(MonarchPriorityPickerTokens.DotSize)
                                    .background(MonarchTheme.colors.accentStrong, CircleShape),
                            )
                        }
                    },
                ) { Text(text = names.getValue(level), color = LocalContentColor.current) }
            }
        }
    }
}

/** The name of every level, built in or replaced. */
@Composable
private fun levelNames(labels: MonarchPriorityPickerLabels): Map<MonarchPriorityLevel, String> = mapOf(
    MonarchPriorityLevel.Urgent to (labels.urgent ?: stringResource(R.string.monarch_prioritypicker_urgent)),
    MonarchPriorityLevel.High to (labels.high ?: stringResource(R.string.monarch_prioritypicker_high)),
    MonarchPriorityLevel.Medium to (labels.medium ?: stringResource(R.string.monarch_prioritypicker_medium)),
    MonarchPriorityLevel.Low to (labels.low ?: stringResource(R.string.monarch_prioritypicker_low)),
    MonarchPriorityLevel.None to (labels.none ?: stringResource(R.string.monarch_prioritypicker_none)),
)
