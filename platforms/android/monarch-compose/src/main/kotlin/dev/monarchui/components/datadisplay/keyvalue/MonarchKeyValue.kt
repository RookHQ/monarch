package dev.monarchui.components.datadisplay.keyvalue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Compact metadata as inline label and value pairs, such as
 * "Region eu-west · Plan Team · ID 4f2a…".
 *
 * Built by Monarch from a `FlowRow` (or a `Column` in the stack layout) of
 * texts, with a `MonarchCopyButton` after a pair that can be copied. Each
 * label and its value are one TalkBack element, read together; a copy button
 * is a separate element named "Copy" with the label as its target. Separators
 * are decoration and hidden from TalkBack.
 *
 * @param items the pairs, in reading order.
 * @param modifier the modifier of the list.
 * @param separator what sits between pairs in the wrap layout.
 * @param size caption or body text.
 * @param layout `Wrap` flows pairs onto more lines; `Stack` puts each pair on its own line.
 * @param copyIcon the icon of the copy buttons, for example a copy `MonarchIcon`. With an icon the buttons show
 * only the icon; without one they show the word "Copy". Monarch bundles no icons.
 * @param onCopy called with the copied text after a copy button is used.
 * @param emptyLabel what TalkBack reads for an empty value. `null` uses `monarch_keyvalue_empty`, "Not set".
 */
@Composable
public fun MonarchKeyValue(
    items: List<MonarchKeyValueItem>,
    modifier: Modifier = Modifier,
    separator: MonarchKeyValueSeparator = MonarchKeyValueSeparator.Dot,
    size: MonarchKeyValueSize = MonarchKeyValueSize.Sm,
    layout: MonarchKeyValueLayout = MonarchKeyValueLayout.Wrap,
    copyIcon: (@Composable () -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    emptyLabel: String? = null,
) {
    val empty = monarchString(emptyLabel, R.string.monarch_keyvalue_empty)
    val pair: @Composable (MonarchKeyValueItem) -> Unit = { item -> KeyValuePair(item, size, empty, copyIcon, onCopy) }
    when (layout) {
        MonarchKeyValueLayout.Stack -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MonarchKeyValueTokens.LineGap),
        ) { items.forEach { pair(it) } }
        MonarchKeyValueLayout.Wrap -> FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(MonarchKeyValueTokens.ItemGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchKeyValueTokens.ItemGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index > 0) Separator(separator, size)
                    pair(item)
                }
            }
        }
    }
}

@Composable
private fun KeyValuePair(
    item: MonarchKeyValueItem,
    size: MonarchKeyValueSize,
    emptyLabel: String,
    copyIcon: (@Composable () -> Unit)?,
    onCopy: ((String) -> Unit)?,
) {
    val colors = MonarchKeyValueTokens.colors(MonarchTheme.colors)
    val typography = MonarchTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f, fill = false).semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(MonarchKeyValueTokens.PairGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.label,
                style = MonarchKeyValueTokens.textStyle(size, monospace = false, typography = typography),
                color = colors.label,
            )
            val value = item.value
            if (value.isNullOrEmpty()) {
                Text(
                    text = MonarchKeyValueTokens.EMPTY_MARK,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = emptyLabel },
                    style = MonarchKeyValueTokens.textStyle(size, monospace = false, typography = typography),
                    color = colors.label,
                )
            } else {
                Text(
                    text = value,
                    style = MonarchKeyValueTokens.textStyle(size, item.monospace, typography),
                    color = colors.value,
                )
            }
        }
        val copyText = item.copyText
        if (copyText != null) {
            if (copyIcon != null) {
                MonarchCopyButton(value = copyText, icon = copyIcon, target = item.label, onCopy = onCopy)
            } else {
                MonarchCopyButton(
                    value = copyText,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    target = item.label,
                    onCopy = onCopy,
                )
            }
        }
    }
}

@Composable
private fun Separator(separator: MonarchKeyValueSeparator, size: MonarchKeyValueSize) {
    val colors = MonarchKeyValueTokens.colors(MonarchTheme.colors)
    when (separator) {
        MonarchKeyValueSeparator.Dot -> Text(
            text = MonarchKeyValueTokens.DOT,
            modifier = Modifier.clearAndSetSemantics { },
            style = MonarchKeyValueTokens.textStyle(size, monospace = false, typography = MonarchTheme.typography),
            color = colors.dot,
        )
        MonarchKeyValueSeparator.Divider -> Box(
            Modifier
                .width(MonarchShapeDefaults.BorderWidth)
                .height(MonarchKeyValueTokens.DividerHeight * LocalDensity.current.fontScale)
                .background(colors.divider)
                .clearAndSetSemantics { },
        )
        MonarchKeyValueSeparator.None -> Unit
    }
}
