package com.rookhq.monarch.components.forms.ratingpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Picks one value on a rating scale: 1 to 5 by default, or any scale with a
 * label per value, shown as tiles or as stars.
 *
 * Built by Monarch from Compose foundation layouts, because Material 3 has no
 * rating control. The values are radio buttons in one `selectableGroup`, so
 * TalkBack reads each with its position in the group and the chosen one as
 * selected. A clearable rating clears when the chosen value is tapped again,
 * and offers a "Clear rating" action.
 *
 * Monarch bundles no icons: stars are drawn as a five-pointed shape unless
 * [star] supplies the app's own.
 *
 * @param value the chosen value, or `null` for no rating yet.
 * @param onValueChange called with the new value, or `null` when a clearable rating is cleared.
 * @param modifier the modifier of the picker.
 * @param label the visible label, already localised.
 * @param options the scale, in order. By default the whole numbers from 1 to 5.
 * @param variant tiles with the number and label, or stars with the chosen label beside them.
 * @param size the size of the tiles or stars. Every target stays at least 48dp.
 * @param enabled whether the user can change the rating.
 * @param readOnly shows the rating at full strength without letting it change; TalkBack reads it as one value.
 * @param clearable choosing the chosen value again clears the rating.
 * @param star replaces the drawn star; it receives whether the star is filled. It is decoration.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchRatingPicker(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    options: List<MonarchRatingOption> = MonarchRatingOption.range(),
    variant: MonarchRatingPickerVariant = MonarchRatingPickerVariant.Scale,
    size: MonarchRatingPickerSize = MonarchRatingPickerSize.Md,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    clearable: Boolean = false,
    star: (@Composable (filled: Boolean) -> Unit)? = null,
    labels: MonarchRatingPickerLabels = MonarchRatingPickerLabels(),
) {
    val max = options.lastOrNull()?.value ?: MonarchRatingOption.DEFAULT_LAST
    val chosen = options.firstOrNull { it.value == value }
    val spokenValue = valueText(chosen?.value, max, chosen?.label, labels)
    val clearName = monarchString(labels.clear, R.string.monarch_ratingpicker_clear)
    val interactive = enabled && !readOnly
    val pick: (Int) -> Unit = { picked -> onValueChange(if (clearable && picked == value) null else picked) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchRatingPickerTokens.LabelGap),
    ) {
        if (label != null) {
            MonarchText(
                text = label,
                style = MonarchTextStyle.Label,
                color = if (enabled) MonarchTheme.colors.textDefault else MonarchTheme.colors.textDisabled,
            )
        }
        val group = when {
            readOnly -> Modifier.readOnlySemantics(label, spokenValue)
            interactive && clearable && value != null -> Modifier.clearableGroup(clearName) { onValueChange(null) }
            else -> Modifier.selectableGroup()
        }
        val scale = RatingScale(options, value, max, size, enabled, readOnly, pick)
        when (variant) {
            MonarchRatingPickerVariant.Scale -> Tiles(scale = scale, modifier = group)
            MonarchRatingPickerVariant.Stars ->
                Stars(scale = scale, chosenLabel = chosen?.label, star = star, labels = labels, modifier = group)
        }
    }
}

/** A read-only picker is one element: the label, with the value as its state. */
private fun Modifier.readOnlySemantics(label: String?, value: String): Modifier = clearAndSetSemantics {
    if (label != null) contentDescription = label
    stateDescription = value
}

/** The group of values with an action that clears the rating. */
private fun Modifier.clearableGroup(name: String, clear: () -> Unit): Modifier = selectableGroup().semantics {
    customActions = listOf(
        CustomAccessibilityAction(name) {
            clear()
            true
        },
    )
}

/** What the tiles and stars need to draw and pick a value. */
private class RatingScale(
    val options: List<MonarchRatingOption>,
    val value: Int?,
    val max: Int,
    val size: MonarchRatingPickerSize,
    val enabled: Boolean,
    val readOnly: Boolean,
    val pick: (Int) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tiles(scale: RatingScale, modifier: Modifier) {
    val tokens = MonarchRatingPickerTokens
    val shape = MonarchTheme.shapes.md
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        scale.options.forEach { option ->
            val selected = option.value == scale.value
            val colors = tokens.tile(selected, scale.enabled, MonarchTheme.colors)
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = tokens.TileMinSize, minHeight = tokens.TileMinSize)
                    .monarchFocusRing(shape)
                    .clip(shape)
                    .background(colors.container, shape)
                    .border(tokens.tileBorderWidth(selected), colors.border ?: colors.container, shape)
                    .ratingChoice(scale, option.value, selected)
                    .padding(tokens.tilePadding(scale.size)),
                verticalArrangement = Arrangement.spacedBy(tokens.TileTextGap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = option.value.toString(), style = MonarchTheme.typography.bodyStrong, color = colors.content)
                option.label?.let { Text(text = it, style = MonarchTheme.typography.caption, color = colors.content) }
            }
        }
    }
}

@Composable
private fun Stars(
    scale: RatingScale,
    chosenLabel: String?,
    star: (@Composable (Boolean) -> Unit)?,
    labels: MonarchRatingPickerLabels,
    modifier: Modifier,
) {
    val tokens = MonarchRatingPickerTokens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(tokens.StarGap)) {
            scale.options.forEach { option ->
                val filled = scale.value != null && option.value <= scale.value
                val name = valueText(option.value, scale.max, option.label, labels)
                Box(
                    modifier = Modifier
                        .monarchMinimumTouchTarget()
                        .monarchFocusRing(MonarchTheme.shapes.full)
                        .semantics { contentDescription = name }
                        .ratingChoice(scale, option.value, option.value == scale.value),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.clearAndSetSemantics { }) {
                        if (star != null) star(filled) else DrawnStar(filled, scale)
                    }
                }
            }
        }
        if (chosenLabel != null) {
            Text(
                text = chosenLabel,
                modifier = Modifier.padding(start = tokens.LabelGap),
                style = MonarchTheme.typography.label,
                color = if (scale.enabled) MonarchTheme.colors.textMuted else MonarchTheme.colors.textDisabled,
            )
        }
    }
}

@Composable
private fun DrawnStar(filled: Boolean, scale: RatingScale) {
    val tokens = MonarchRatingPickerTokens
    val color = tokens.star(filled, scale.enabled, MonarchTheme.colors)
    val shape = tokens.StarShape
    val base = Modifier.size(tokens.starSize(scale.size))
    Box(
        modifier = if (filled) {
            base.background(color, shape)
        } else {
            base.border(tokens.StarBorderWidth, color, shape)
        },
    )
}

/** The selection behaviour of one value: a radio button, or only its selected state when read-only. */
private fun Modifier.ratingChoice(scale: RatingScale, option: Int, selected: Boolean): Modifier = if (scale.readOnly) {
    semantics { this.selected = selected }
} else {
    selectable(selected = selected, enabled = scale.enabled, role = Role.RadioButton, onClick = { scale.pick(option) })
}

@Composable
private fun valueText(value: Int?, max: Int, label: String?, labels: MonarchRatingPickerLabels): String {
    labels.valueText?.let { return it(value, max, label) }
    return when {
        value == null -> stringResource(R.string.monarch_ratingpicker_none)
        label != null -> stringResource(R.string.monarch_ratingpicker_value_label, value, max, label)
        else -> stringResource(R.string.monarch_ratingpicker_value, value, max)
    }
}
