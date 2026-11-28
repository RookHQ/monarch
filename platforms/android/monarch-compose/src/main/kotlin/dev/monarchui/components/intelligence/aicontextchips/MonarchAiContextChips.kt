package dev.monarchui.components.intelligence.aicontextchips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * What the AI will see with the next prompt: one chip per file, selection,
 * page, record, app or person, each with its kind, its name and its token
 * count with a weight mark, and the total after the chips.
 *
 * Built on a `FlowRow` of Monarch chips; a removable item is a button, as
 * `MonarchTag` does: the whole chip is the remove action, Delete or Backspace
 * on a hardware keyboard removes a focused chip, and TalkBack reads it as a
 * button with the action "Remove … from context". Items that cannot be
 * removed are drawn by Monarch with the same look and are not actionable.
 * `InputChip` is not used because it is selectable and TalkBack would read
 * every chip as a checkbox. After a removal, "… removed from context" is
 * announced through a polite live region.
 *
 * The weight mark fills one, two or three bars by the item's share of the
 * total, or of [budget] when set, and the count is drawn in the default text
 * colour when heavy, so the weight is not colour alone.
 *
 * @param items what the AI will see, in the order it was added.
 * @param modifier the modifier of the chips.
 * @param budget the tokens the context may use. When set, each weight is the item's share of the budget
 * instead of the total.
 * @param showTotal whether the total is shown after the chips. `null` shows it when any item has a token
 * count. Never shown without items.
 * @param enabled whether the chips can be removed. Disabled chips use the muted fill and disabled text.
 * @param emptyText shown when [items] is empty, in the subtle text colour. Without it nothing is shown.
 * @param onRemove called with the id of the item to remove; take it out of [items]. Without it no chip is
 * removable. Items with `removable = false` never are.
 * @param kindIcon an icon at the start of each chip for its kind, for example a `MonarchIcon` of size `Sm`
 * with a `null` content description. Tinted with the AI strong colour and hidden from TalkBack, which reads
 * the kind name. Monarch bundles no icons.
 * @param removeIcon the close mark at the end of removable chips, decorative. Monarch bundles no icons;
 * without it the chip is still removable by a tap.
 * @param labels replacements for the built-in strings.
 * @param fallbackFocusRequester where focus goes when the last removable chip is removed, typically the prompt
 * field (the web `fallbackFocusRef`). Otherwise focus moves to the next removable chip, else the previous one.
 */
@Composable
public fun MonarchAiContextChips(
    items: List<MonarchAiContextItem>,
    modifier: Modifier = Modifier,
    budget: Int? = null,
    showTotal: Boolean? = null,
    enabled: Boolean = true,
    emptyText: String? = null,
    onRemove: ((String) -> Unit)? = null,
    kindIcon: (@Composable (MonarchAiContextKind) -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAiContextChipsLabels = MonarchAiContextChipsLabels(),
    fallbackFocusRequester: FocusRequester? = null,
) {
    val tokens = MonarchAiContextChipsTokens
    val removableIds = items.filter { MonarchAiContextChipsLogic.removable(it, onRemove != null) }.map { it.id }
    val removalFocus = rememberMonarchRemovalFocus(removableIds, fallbackFocusRequester)
    var removedText by remember { mutableStateOf("") }
    val removedFormat = stringResource(R.string.monarch_aicontextchips_removed)
    Column(modifier = modifier) {
        if (items.isEmpty()) {
            if (emptyText != null) {
                Text(text = emptyText, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textSubtle)
            }
        } else {
            val total = MonarchAiContextChipsLogic.total(items)
            val groupLabel = monarchString(labels.label, R.string.monarch_aicontextchips_label)
            FlowRow(
                modifier = Modifier.semantics {
                    contentDescription = groupLabel
                    isTraversalGroup = true
                },
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val remove = if (MonarchAiContextChipsLogic.removable(item, onRemove != null)) {
                        {
                            removedText = labels.removed?.invoke(item.label) ?: removedFormat.format(item.label)
                            removalFocus.removing(removableIds, item.id)
                            onRemove?.invoke(item.id)
                            Unit
                        }
                    } else {
                        null
                    }
                    ContextChip(
                        item = item,
                        weight = MonarchAiContextChipsLogic.weight(item, total, budget),
                        enabled = enabled,
                        onRemove = remove,
                        kindIcon = kindIcon,
                        removeIcon = removeIcon,
                        labels = labels,
                        focus = removalFocus.requester(item.id),
                    )
                }
                if (MonarchAiContextChipsLogic.showsTotal(items, showTotal)) {
                    Text(
                        text = totalText(total, labels),
                        modifier = Modifier.padding(start = tokens.Gap),
                        style = MonarchTheme.typography.caption,
                        color = if (enabled) MonarchTheme.colors.textMuted else MonarchTheme.colors.textDisabled,
                    )
                }
            }
        }
        // Always present, so a removal is announced once as a change of this live region.
        MonarchLiveStatus(text = removedText.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun ContextChip(
    item: MonarchAiContextItem,
    weight: MonarchAiContextWeight?,
    enabled: Boolean,
    onRemove: (() -> Unit)?,
    kindIcon: (@Composable (MonarchAiContextKind) -> Unit)?,
    removeIcon: (@Composable () -> Unit)?,
    labels: MonarchAiContextChipsLabels,
    focus: FocusRequester,
) {
    val tokens = MonarchAiContextChipsTokens
    val colors = MonarchTheme.colors
    val chipColors = tokens.chipColors(enabled, colors)
    val formatted = item.tokens?.let { MonarchAiContextChipsLogic.formatTokens(it, currentLocale()) }
    val spoken = spokenChipLabel(item, formatted, labels)
    val shape = MonarchShapeDefaults.Mark
    val content: @Composable () -> Unit = {
        ChipContent(item, weight, formatted, enabled, kindIcon, labels)
    }
    if (onRemove == null) {
        Row(
            modifier = Modifier
                .heightIn(min = tokens.MinHeight)
                .monarchSurface(chipColors, shape)
                .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding)
                .clearAndSetSemantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides chipColors.content, content = content)
        }
        return
    }
    val removeName = labels.remove?.invoke(item.label)
        ?: stringResource(R.string.monarch_aicontextchips_remove, item.label)
    // The outer node is the one TalkBack element; the inner row takes the tap and keyboard focus.
    Box(
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = removeName) {
                    if (enabled) onRemove()
                    enabled
                }
                if (!enabled) disabled()
            }
            .onPreviewKeyEvent { event ->
                val delete = event.key == Key.Delete || event.key == Key.Backspace
                if (enabled && delete && event.type == KeyEventType.KeyUp) {
                    onRemove()
                    true
                } else {
                    false
                }
            },
    ) {
        Row(
            modifier = Modifier
                .focusRequester(focus)
                .clickable(enabled = enabled, onClickLabel = removeName, role = Role.Button, onClick = onRemove)
                .monarchMinimumTouchTarget()
                .monarchFocusRing(shape)
                .heightIn(min = tokens.MinHeight)
                .monarchSurface(chipColors, shape)
                .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides chipColors.content, content = content)
            if (removeIcon != null) {
                val iconColor = if (enabled) colors.textMuted else chipColors.content
                CompositionLocalProvider(LocalContentColor provides iconColor, content = removeIcon)
            }
        }
    }
}

/** What TalkBack reads for a chip: kind, name, count and description. */
@Composable
private fun spokenChipLabel(
    item: MonarchAiContextItem,
    formatted: String?,
    labels: MonarchAiContextChipsLabels,
): String {
    val kind = labels.kind?.invoke(item.kind) ?: stringResource(MonarchAiContextChipsLogic.kindName(item.kind))
    val count = item.tokens?.let { maxOf(0, it) }
    val spokenCount = if (count != null && formatted != null) {
        labels.tokens?.invoke(formatted, count)
            ?: pluralStringResource(R.plurals.monarch_aicontextchips_tokens, count, formatted)
    } else {
        null
    }
    return MonarchAiContextChipsLogic.spokenLabel(kind, item.label, spokenCount, item.description)
}

@Composable
private fun ChipContent(
    item: MonarchAiContextItem,
    weight: MonarchAiContextWeight?,
    formatted: String?,
    enabled: Boolean,
    kindIcon: (@Composable (MonarchAiContextKind) -> Unit)?,
    labels: MonarchAiContextChipsLabels,
) {
    val tokens = MonarchAiContextChipsTokens
    val colors = MonarchTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kindIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.iconColor(enabled, colors)) {
                kindIcon(item.kind)
            }
        }
        Text(
            text = item.group?.let { "$it ${item.label}" } ?: item.label,
            modifier = Modifier.widthIn(max = tokens.LabelMaxWidth * LocalDensity.current.fontScale),
            style = MonarchTheme.typography.label,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
        if (weight != null && formatted != null) {
            WeightMark(weight, enabled)
            val count = maxOf(0, item.tokens ?: 0)
            Text(
                text = labels.tokens?.invoke(formatted, count)
                    ?: pluralStringResource(R.plurals.monarch_aicontextchips_tokens, count, formatted),
                style = MonarchTheme.typography.caption,
                color = tokens.tokensColor(weight, enabled, colors),
            )
        }
    }
}

@Composable
private fun WeightMark(weight: MonarchAiContextWeight, enabled: Boolean) {
    val tokens = MonarchAiContextChipsTokens
    val colors = MonarchTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.BarGap), verticalAlignment = Alignment.Bottom) {
        tokens.BarHeights.forEachIndexed { index, height ->
            val color = if (enabled) tokens.barColor(index, weight, colors) else colors.borderSubtle
            Box(Modifier.size(tokens.BarWidth, height).background(color))
        }
    }
}

@Composable
private fun totalText(total: Int, labels: MonarchAiContextChipsLabels): String {
    val formatted = MonarchAiContextChipsLogic.formatTokens(total, currentLocale())
    return labels.total?.invoke(formatted, total)
        ?: pluralStringResource(R.plurals.monarch_aicontextchips_total, total, formatted)
}

/** The first locale of the configuration, for compact numbers. */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
