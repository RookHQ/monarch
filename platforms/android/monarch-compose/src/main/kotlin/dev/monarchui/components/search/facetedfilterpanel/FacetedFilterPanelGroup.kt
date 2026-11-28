package dev.monarchui.components.search.facetedfilterpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.numberinput.MonarchNumberInput
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/** One facet: its header with the active count and Clear, then its options or range while open. */
@Suppress("LongParameterList")
@Composable
internal fun FacetedFilterPanelGroup(
    facet: MonarchFacet,
    value: Map<String, MonarchFacetSelection>,
    onValueChange: (Map<String, MonarchFacetSelection>) -> Unit,
    state: FacetedFilterPanelGroupState,
    enabled: Boolean,
    expandIcon: (@Composable () -> Unit)?,
    text: FacetedFilterPanelText,
    colors: FacetedFilterPanelColors,
    locale: Locale,
) {
    val tokens = MonarchFacetedFilterPanelTokens
    val active = FacetedFilterPanelModel.activeCount(facet, value)
    val motion = MonarchTheme.motion
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MonarchTouchTarget.Minimum)
                    .monarchFocusRing(MonarchShapeDefaults.Row)
                    .clickable(enabled = enabled, role = Role.Button) { state.onOpenChange(!state.open) }
                    .semantics {
                        heading()
                        stateDescription = if (state.open) text.toggles.expanded else text.toggles.collapsed
                    }
                    .padding(vertical = tokens.HeaderVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (expandIcon != null) {
                    Box(modifier = Modifier.rotate(if (state.open) tokens.OPEN_DEGREES else 0f)) { expandIcon() }
                }
                Column {
                    Text(text = facet.label, style = MonarchTheme.typography.label, color = colors.title)
                    if (active > 0) {
                        Text(
                            text = text.activeCount(active),
                            style = MonarchTheme.typography.caption,
                            color = colors.active,
                        )
                    }
                }
            }
            if (active > 0) {
                val clearName = text.clearGroupLabel(facet.label)
                MonarchButton(
                    text = text.clearGroup,
                    onClick = { onValueChange(FacetedFilterPanelModel.clear(facet, value)) },
                    modifier = Modifier.semantics { contentDescription = clearName },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = enabled,
                )
            }
        }
        AnimatedVisibility(
            visible = state.open,
            enter = expandVertically(motion.spec()),
            exit = shrinkVertically(motion.spec()),
        ) {
            Column(modifier = Modifier.padding(bottom = tokens.GroupBottomPadding)) {
                when (val kind = facet.kind) {
                    is MonarchFacetKind.Options ->
                        FacetedFilterPanelOptions(facet, kind, value, onValueChange, state, enabled, text, locale)
                    is MonarchFacetKind.Range ->
                        FacetedFilterPanelRange(facet, kind, value, onValueChange, enabled, text, colors, locale)
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun FacetedFilterPanelOptions(
    facet: MonarchFacet,
    kind: MonarchFacetKind.Options,
    value: Map<String, MonarchFacetSelection>,
    onValueChange: (Map<String, MonarchFacetSelection>) -> Unit,
    state: FacetedFilterPanelGroupState,
    enabled: Boolean,
    text: FacetedFilterPanelText,
    locale: Locale,
) {
    val tokens = MonarchFacetedFilterPanelTokens
    val themeColors = MonarchTheme.colors
    if (kind.options.isEmpty()) {
        Text(
            text = text.noOptions,
            style = MonarchTheme.typography.caption,
            color = tokens.optionColor(themeColors, enabled, muted = true),
        )
        return
    }
    val checked = FacetedFilterPanelModel.checked(facet, value)
    val visible = FacetedFilterPanelModel.visible(kind, checked, state.showAll)
    visible.forEach { option ->
        val on = option.id in checked
        val optionEnabled = enabled && option.enabled
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MonarchTouchTarget.Minimum)
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .toggleable(value = on, enabled = optionEnabled, role = Role.Checkbox) {
                    onValueChange(FacetedFilterPanelModel.toggle(facet, option.id, it, value))
                },
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchCheckbox(checked = on, onCheckedChange = null, enabled = optionEnabled)
            Text(
                text = option.label,
                modifier = Modifier.weight(1f, fill = false),
                style = MonarchTheme.typography.body,
                color = tokens.optionColor(themeColors, optionEnabled, muted = false),
            )
            option.count?.let { count ->
                Text(
                    text = FacetedFilterPanelFormat.countText(count, locale),
                    style = MonarchTheme.typography.caption,
                    color = tokens.optionColor(themeColors, optionEnabled, muted = true),
                )
            }
        }
    }
    val hidden = kind.options.size - visible.size
    if (hidden > 0 || state.showAll && kind.options.size > kind.maxVisible) {
        MonarchButton(
            text = if (state.showAll) text.toggles.showFewer else text.toggles.showMore(hidden),
            onClick = { state.onShowAllChange(!state.showAll) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun FacetedFilterPanelRange(
    facet: MonarchFacet,
    kind: MonarchFacetKind.Range,
    value: Map<String, MonarchFacetSelection>,
    onValueChange: (Map<String, MonarchFacetSelection>) -> Unit,
    enabled: Boolean,
    text: FacetedFilterPanelText,
    colors: FacetedFilterPanelColors,
    locale: Locale,
) {
    val range = FacetedFilterPanelModel.range(facet, kind, value)
    val shown = text.range(
        FacetedFilterPanelFormat.valueText(range.start, kind.unit, locale),
        FacetedFilterPanelFormat.valueText(range.endInclusive, kind.unit, locale),
    )
    val bounds = kind.bounds
    val unitText = kind.unit
    val fieldValue = { number: Float, bound: Float -> number.takeIf { it != bound }?.toDouble() }
    val labelColor = colors.count
    val stacked = LocalDensity.current.fontScale >= LargeFontScale
    val lowField = @Composable { modifier: Modifier ->
        MonarchNumberInput(
            value = fieldValue(range.start, bounds.start),
            onValueChange = { next ->
                val low = (next?.toFloat() ?: bounds.start).coerceIn(bounds.start, bounds.endInclusive)
                onValueChange(FacetedFilterPanelModel.setRange(facet, low..maxOf(low, range.endInclusive), value))
            },
            modifier = modifier,
            label = text.minimum,
            enabled = enabled,
            showSteppers = false,
        )
    }
    val highField = @Composable { modifier: Modifier ->
        MonarchNumberInput(
            value = fieldValue(range.endInclusive, bounds.endInclusive),
            onValueChange = { next ->
                val high = (next?.toFloat() ?: bounds.endInclusive).coerceIn(bounds.start, bounds.endInclusive)
                onValueChange(FacetedFilterPanelModel.setRange(facet, minOf(range.start, high)..high, value))
            },
            modifier = modifier,
            label = text.maximum,
            enabled = enabled,
            showSteppers = false,
        )
    }
    val describe = Modifier.fillMaxWidth().semantics {
        contentDescription = facet.label
        stateDescription = shown
    }
    if (stacked) {
        Column(modifier = describe, verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            lowField(Modifier.fillMaxWidth())
            highField(Modifier.fillMaxWidth())
            if (!unitText.isNullOrEmpty()) {
                Text(text = unitText, style = MonarchTheme.typography.caption, color = labelColor)
            }
        }
    } else {
        Row(
            modifier = describe,
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.Bottom,
        ) {
            lowField(Modifier.weight(1f))
            Text(
                text = "–",
                modifier = Modifier.padding(bottom = MonarchTheme.spacing.space3),
                style = MonarchTheme.typography.caption,
                color = labelColor,
            )
            highField(Modifier.weight(1f))
            if (!unitText.isNullOrEmpty()) {
                Text(
                    text = unitText,
                    modifier = Modifier.padding(bottom = MonarchTheme.spacing.space3),
                    style = MonarchTheme.typography.caption,
                    color = labelColor,
                )
            }
        }
    }
}

private const val LargeFontScale = 1.3f
