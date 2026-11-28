package com.rookhq.monarch.components.intelligence.aiprompttoolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * An on/off option in `MonarchAiPromptToolbar`, such as "Search the web" or
 * "Deep research".
 *
 * A Monarch toggle chip drawn on `Modifier.selectable` with the checkbox
 * role, as the Material filter chip is: TalkBack reads it with its selected or
 * not selected state, and the label never changes with the state. When on it takes the assistant subtle
 * fill, the assistant border and the strong assistant text, so the state is
 * also shown by the border and not by colour alone.
 *
 * @param text the label, and always the TalkBack name.
 * @param checked whether the option is on.
 * @param onCheckedChange called with the new state when the user toggles it. Not called when not [enabled].
 * @param modifier the modifier of the toggle.
 * @param enabled whether the user can toggle it.
 * @param hideLabel shows only [icon]. Ignored without an icon, because the toggle would be empty.
 * @param icon an icon before the label, for example a `MonarchIcon` with a `null` content description. Monarch
 * bundles no icons.
 * @param interactionSource the source of the press, focus and hover interactions of the toggle.
 */
@Composable
public fun MonarchAiPromptToolbarToggle(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hideLabel: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Control
    val iconOnly = hideLabel && icon != null
    val role = toggleRole(MonarchAiPromptToolbarTokens.toggleColors(colors), enabled, checked)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    // Hover and press take one surface step on the off state; the on state keeps its tint.
    val highlighted = pressed || hovered
    val fill = if (enabled && !checked && highlighted) colors.bgMuted else role.container
    Row(
        modifier = modifier
            .selectable(
                selected = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = source,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .then(if (iconOnly) Modifier.semantics { contentDescription = text } else Modifier)
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .heightIn(min = MonarchAiPromptToolbarTokens.ToggleMinHeight)
            .clip(shape)
            .background(fill, shape)
            .border(MonarchShapeDefaults.BorderWidth, role.border, shape)
            .padding(horizontal = MonarchAiPromptToolbarTokens.TogglePadding),
        horizontalArrangement = Arrangement.spacedBy(
            MonarchAiPromptToolbarTokens.ToggleGap,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides role.content) {
            if (icon != null) icon()
            if (!iconOnly) Text(text = text, style = MonarchTheme.typography.label, color = role.content)
        }
    }
}

private fun toggleRole(roles: MonarchAiPromptToolbarToggleColors, enabled: Boolean, checked: Boolean) = when {
    enabled && checked -> roles.on
    enabled -> roles.off
    checked -> roles.disabledOn
    else -> roles.disabledOff
}
