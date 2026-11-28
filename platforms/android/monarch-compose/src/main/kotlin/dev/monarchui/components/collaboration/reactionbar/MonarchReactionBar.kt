package dev.monarchui.components.collaboration.reactionbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * The emoji reactions under a message, comment or post: a chip per emoji with
 * its count, selected when the current person reacted, and an add button.
 *
 * Interactive chips are selectable Monarch chips in a `FlowRow`, so TalkBack
 * reads each as a selectable control and the chips wrap onto new rows at any
 * text size. A chip is named by the emoji's name and its count, "thumbs up, 3
 * reactions"; the emoji character and the digits are not read separately. A
 * reacted chip has a thicker border and a bold count as well as the accent
 * colours. Without [onToggle] the bar is read-only: chips are images with the
 * same names and there is no add button.
 *
 * The add button opens [picker] when given, usually `MonarchEmojiPicker` (the
 * system emoji picker) in a sheet, or otherwise a menu of [quickReactions].
 * Picking an emoji you have not reacted with calls [onToggle] with `true`.
 *
 * @param reactions the reactions in display order. A count of 0 hides the chip; a repeated emoji is shown once.
 * @param modifier the modifier of the bar.
 * @param onToggle called with the emoji and its new state when a chip is tapped, or with `true` when an emoji
 * is picked. The app updates [reactions]. Without it the bar is read-only.
 * @param size [MonarchReactionBarSize.Sm] under messages and comments, [MonarchReactionBarSize.Md] under posts.
 * @param enabled whether chips and the add button respond. Off, they use the disabled text colour.
 * @param addable whether an interactive bar shows the add button.
 * @param maxReactions at most this many distinct reactions; the add button hides when the limit is reached.
 * @param quickReactions emoji offered by the add button's menu when no [picker] is given.
 * @param addIcon the icon of the add button, for example an emoji-with-plus `MonarchIcon` with a `null`
 * content description. Without it the button shows "+". Monarch bundles no icons.
 * @param picker the emoji picker shown while the add button is open, such as
 * `{ onPick, onDismiss -> ModalBottomSheet(onDismiss) { MonarchEmojiPicker(onEmojiPicked = onPick) } }`. Call
 * `onPick` with the chosen emoji, or `onDismiss` to close it without one.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchReactionBar(
    reactions: List<MonarchReactionBarItem>,
    modifier: Modifier = Modifier,
    onToggle: ((emoji: String, reacted: Boolean) -> Unit)? = null,
    size: MonarchReactionBarSize = MonarchReactionBarSize.Sm,
    enabled: Boolean = true,
    addable: Boolean = true,
    maxReactions: Int? = null,
    quickReactions: List<String> = emptyList(),
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.AddReaction,
    picker: (@Composable (onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit)? = null,
    labels: MonarchReactionBarLabels = MonarchReactionBarLabels(),
) {
    val tokens = MonarchReactionBarTokens
    val shown = tokens.shown(reactions)
    val canAdd = tokens.canAdd(
        interactive = onToggle != null,
        addable = addable,
        shownCount = shown.size,
        maxReactions = maxReactions,
        hasSource = picker != null || quickReactions.isNotEmpty(),
    )
    if (shown.isEmpty() && !canAdd) return
    val barLabel = monarchString(labels.label, R.string.monarch_reactionbar_label)
    FlowRow(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = barLabel
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { item ->
            val name = reactionName(item, labels)
            if (onToggle == null) {
                ReadOnlyChip(item, name, size)
            } else {
                ReactionChip(item, name, size, enabled) { onToggle(item.emoji, !item.reacted) }
            }
        }
        if (canAdd && onToggle != null) {
            AddReaction(
                enabled = enabled,
                quickReactions = quickReactions,
                addIcon = addIcon,
                picker = picker,
                labels = labels,
            ) { emoji -> if (shown.firstOrNull { it.emoji == emoji }?.reacted != true) onToggle(emoji, true) }
        }
    }
}

/** The spoken name of a chip: the emoji's name and count, then who reacted. */
@Composable
private fun reactionName(item: MonarchReactionBarItem, labels: MonarchReactionBarLabels): String {
    val label = item.label ?: item.emoji
    val name = labels.reaction?.invoke(label, item.count)
        ?: pluralStringResource(R.plurals.monarch_reactionbar_reaction, item.count, label, item.count)
    val who = item.reactedBy ?: return name
    val reactedBy = labels.reactedBy?.invoke(who) ?: stringResource(R.string.monarch_reactionbar_reacted_by, who)
    return "$name. $reactedBy"
}

@Composable
private fun ChipLabel(item: MonarchReactionBarItem, size: MonarchReactionBarSize) {
    val style = MonarchReactionBarTokens.textStyle(size, MonarchTheme.typography)
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MonarchReactionBarTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = item.emoji, style = style)
        Text(
            text = item.count.toString(),
            style = style,
            fontWeight = if (item.reacted) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ReactionChip(
    item: MonarchReactionBarItem,
    name: String,
    size: MonarchReactionBarSize,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MonarchReactionBarTokens
    val colors = tokens.colors(item.reacted, enabled, MonarchTheme.colors)
    val shape = MonarchShapeDefaults.Mark
    // Drawn like the read-only chip; reacted is the accent tint with a thicker border. Selectable with the
    // checkbox role, as a filter chip, so TalkBack reads whether this person reacted.
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .selectable(selected = item.reacted, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = name }
            .heightIn(min = tokens.minHeight(size))
            .clip(shape)
            .background(colors.container)
            .border(tokens.border(item.reacted, colors), shape)
            .padding(horizontal = tokens.HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            ChipLabel(item, size)
        }
    }
}

@Composable
private fun ReadOnlyChip(item: MonarchReactionBarItem, name: String, size: MonarchReactionBarSize) {
    val tokens = MonarchReactionBarTokens
    val colors = tokens.colors(item.reacted, enabled = true, colors = MonarchTheme.colors)
    val shape = MonarchShapeDefaults.Mark
    Box(
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Image
            }
            .heightIn(min = tokens.minHeight(size))
            .monarchSurface(colors, shape)
            .padding(horizontal = tokens.HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            ChipLabel(item, size)
        }
    }
}

@Composable
private fun AddReaction(
    enabled: Boolean,
    quickReactions: List<String>,
    addIcon: (@Composable () -> Unit)?,
    picker: (@Composable (onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit)?,
    labels: MonarchReactionBarLabels,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val name = monarchString(labels.add, R.string.monarch_reactionbar_add)
    val pick: (String) -> Unit = { emoji ->
        open = false
        onPick(emoji)
    }
    Box {
        if (addIcon != null) {
            MonarchIconButton(label = name, onClick = { open = true }, enabled = enabled, icon = addIcon)
        } else {
            val text = monarchString(labels.addText, R.string.monarch_reactionbar_add_text)
            MonarchButton(
                onClick = { open = true },
                modifier = Modifier.semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            ) {
                // The button is named by its description; the "+" is not read.
                Text(text = text, modifier = Modifier.semantics { hideFromAccessibility() })
            }
        }
        if (picker != null) {
            if (open) picker(pick) { open = false }
        } else {
            MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
                quickReactions.distinct().forEach { emoji ->
                    MonarchMenuItem(text = emoji, onClick = { pick(emoji) })
                }
            }
        }
    }
}
