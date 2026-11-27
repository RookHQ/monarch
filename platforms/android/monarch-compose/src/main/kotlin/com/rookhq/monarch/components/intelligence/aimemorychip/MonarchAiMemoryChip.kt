package com.rookhq.monarch.components.intelligence.aimemorychip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One thing the assistant remembers, from a [MonarchAiMemoryPart] of the
 * conversation model, shown as "Remembered: …" with a menu to edit or
 * forget it.
 *
 * The [MonarchAiMemoryChipVariant.Chip] variant is a chip in the AI colours,
 * 32dp high in a 48dp touch target; the [MonarchAiMemoryChipVariant.Row]
 * variant is a full-width row for lists. When [onEdit] or [onForget] is given,
 * tapping the chip or the row opens a `MonarchMenu` with Edit and Forget, and
 * TalkBack offers both as actions of the element. Edit opens a `MonarchDialog` with a
 * text field; Save is disabled while the text is empty or unchanged. Forget
 * runs at once, without confirmation. A forgotten memory is muted, struck
 * through, says "Forgotten:" and offers no commands. Without callbacks the
 * memory is read-only text.
 *
 * The prefix follows the part's action: "Used memory:", "Remembered:" or
 * "Forgotten:".
 *
 * @param memory the memory from the conversation model: its id, action, text and scope.
 * @param modifier the modifier of the chip or the row.
 * @param variant an inline chip with the prefix, or a full-width row without it.
 * @param showScope whether the scope is shown under the text. By default on the chip and not on the row.
 * @param icon an AI glyph at the start, as a `MonarchIcon` with a `null` content description. Drawn by default, as on
 * the web; `null` draws none.
 * @param labels replacements for the prefixes, the menu, the actions and the edit dialog strings.
 * @param onEdit adds Edit. Receives the id and the trimmed new text; never called with empty or unchanged text.
 * @param onForget adds Forget. Receives the id; runs at once, without confirmation.
 * @param interactionSource the source of the press, focus and hover interactions of the chip or the row.
 * @param editIcon with the Row variant, an icon for an edit button at the end of the row, in place of the options menu.
 * @param forgetIcon the same for a forget button. Either icon replaces the menu with buttons. Monarch bundles no icons.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
public fun MonarchAiMemoryChip(
    memory: MonarchAiMemoryPart,
    modifier: Modifier = Modifier,
    variant: MonarchAiMemoryChipVariant = MonarchAiMemoryChipVariant.Chip,
    showScope: Boolean? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    labels: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
    onEdit: ((id: String, text: String) -> Unit)? = null,
    onForget: ((id: String) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    editIcon: (@Composable () -> Unit)? = null,
    forgetIcon: (@Composable () -> Unit)? = null,
) {
    val actionable = MonarchAiMemoryChipTokens.actionable(memory, onEdit != null, onForget != null)
    val colors = MonarchAiMemoryChipTokens.colors(variant, memory.action, MonarchTheme.colors)
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val editLabel = monarchString(labels.edit, R.string.monarch_aimemorychip_edit)
    val forgetLabel = monarchString(labels.forget, R.string.monarch_aimemorychip_forget)
    val actions = if (actionable) {
        listOfNotNull(
            onEdit?.let {
                CustomAccessibilityAction(editLabel) {
                    editing = true
                    true
                }
            },
            onForget?.let { forget ->
                CustomAccessibilityAction(forgetLabel) {
                    forget(memory.id)
                    true
                }
            },
        )
    } else {
        emptyList()
    }
    val label: @Composable () -> Unit = {
        MemoryText(memory, variant, MonarchAiMemoryChipTokens.showsScope(memory, variant, showScope), colors, labels)
    }
    val withActions = Modifier.semantics { if (actions.isNotEmpty()) customActions = actions }
    val inline = actionable && variant == MonarchAiMemoryChipVariant.Row && (editIcon != null || forgetIcon != null)
    Box(modifier = modifier) {
        MemorySurface(
            variant = variant,
            colors = colors,
            icon = if (inline) null else icon,
            onClick = if (actionable && !inline) ({ menuOpen = true }) else null,
            trailing = if (inline) {
                {
                    if (onEdit != null && editIcon != null) {
                        MonarchIconButton(label = editLabel, onClick = { editing = true }, icon = editIcon)
                    }
                    if (onForget != null && forgetIcon != null) {
                        MonarchIconButton(label = forgetLabel, onClick = { onForget(memory.id) }, icon = forgetIcon)
                    }
                }
            } else {
                null
            },
            optionsLabel = monarchString(labels.options, R.string.monarch_aimemorychip_options),
            modifier = withActions,
            interactionSource = interactionSource,
            content = label,
        )
        if (actionable && !inline) {
            MemoryMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                editLabel = editLabel.takeIf { onEdit != null },
                forgetLabel = forgetLabel.takeIf { onForget != null },
                onEdit = { editing = true },
                onForget = { onForget?.invoke(memory.id) },
            )
        }
    }
    if (editing && onEdit != null) {
        MonarchAiMemoryChipEditDialog(
            original = memory.text,
            labels = labels,
            onSave = { text ->
                editing = false
                onEdit(memory.id, text)
            },
            onDismiss = { editing = false },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun MemorySurface(
    variant: MonarchAiMemoryChipVariant,
    colors: MonarchSurfaceColors,
    icon: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    optionsLabel: String,
    modifier: Modifier,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val chip = variant == MonarchAiMemoryChipVariant.Chip
    val shape = if (chip) MonarchTheme.shapes.md else MonarchShapeDefaults.Row
    val interaction = if (onClick != null) {
        Modifier
            .monarchFocusRing(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = optionsLabel,
                onClick = onClick,
            )
    } else {
        Modifier.semantics(mergeDescendants = true) { }
    }
    Row(
        modifier = Modifier
            .then(if (chip) Modifier else Modifier.fillMaxWidth())
            .then(if (chip && onClick != null) Modifier.monarchMinimumTouchTarget() else Modifier)
            .heightIn(min = if (chip) MonarchAiMemoryChipTokens.ChipMinHeight else MonarchTouchTarget.Minimum)
            .monarchSurface(colors, shape)
            .then(interaction)
            .then(modifier)
            .padding(
                horizontal = if (chip) {
                    MonarchAiMemoryChipTokens.ChipHorizontalPadding
                } else {
                    MonarchAiMemoryChipTokens.RowHorizontalPadding
                },
                vertical = if (chip) {
                    MonarchAiMemoryChipTokens.ChipVerticalPadding
                } else {
                    MonarchAiMemoryChipTokens.RowVerticalPadding
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiMemoryChipTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.aiStrong, content = icon)
        }
        Box(Modifier.weight(1f, fill = false)) { content() }
        trailing?.invoke(this)
    }
}

@Composable
private fun MemoryText(
    memory: MonarchAiMemoryPart,
    variant: MonarchAiMemoryChipVariant,
    showScope: Boolean,
    colors: MonarchSurfaceColors,
    labels: MonarchAiMemoryChipLabels,
) {
    val forgotten = memory.action == MonarchAiMemoryAction.Forget
    val prefix = labels.prefix?.invoke(memory.action) ?: stringResource(prefixResource(memory.action))
    val showPrefix = MonarchAiMemoryChipTokens.showsPrefix(memory, variant)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiMemoryChipTokens.LineGap)) {
        Text(
            text = buildAnnotatedString {
                if (showPrefix) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(prefix) }
                    append(" ")
                }
                withStyle(SpanStyle(textDecoration = if (forgotten) TextDecoration.LineThrough else null)) {
                    append(memory.text)
                }
            },
            style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
            color = colors.content,
        )
        val scope = memory.scope
        if (showScope && scope != null) {
            Text(text = scope, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MemoryMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    editLabel: String?,
    forgetLabel: String?,
    onEdit: () -> Unit,
    onForget: () -> Unit,
) {
    MonarchMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (editLabel != null) {
            MonarchMenuItem(
                text = editLabel,
                onClick = {
                    onDismiss()
                    onEdit()
                },
            )
        }
        if (forgetLabel != null) {
            MonarchMenuItem(
                text = forgetLabel,
                onClick = {
                    onDismiss()
                    onForget()
                },
                destructive = true,
            )
        }
    }
}

private fun prefixResource(action: MonarchAiMemoryAction): Int = when (action) {
    MonarchAiMemoryAction.Read -> R.string.monarch_aimemorychip_prefix_read
    MonarchAiMemoryAction.Write -> R.string.monarch_aimemorychip_prefix_write
    MonarchAiMemoryAction.Forget -> R.string.monarch_aimemorychip_prefix_forget
}
