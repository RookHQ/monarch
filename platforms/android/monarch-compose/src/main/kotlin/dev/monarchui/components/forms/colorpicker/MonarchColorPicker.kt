package dev.monarchui.components.forms.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Picks a colour from named swatches, or any colour by its hex value.
 *
 * Built by Monarch: Android has no system colour picker. The swatches are a
 * group of radio buttons named like "Blue, #2563eb", and the chosen one has
 * a ring and a mark in the middle, so the choice never depends on seeing the
 * colour. The hex field is a `MonarchTextField` whose text is checked and
 * normalised to lower-case `#rrggbb` when the user presses Done or leaves it.
 *
 * @param value the colour as `#rrggbb`, or `null` for none. Text that is not a hex colour is treated as none.
 * @param onValueChange called with lower-case `#rrggbb` when a swatch or a valid hex text selects a
 * different colour.
 * @param modifier the modifier of the picker.
 * @param swatches the presets. An empty list shows only the hex field. Swatches with the same colour select
 * together; keep them distinct.
 * @param enabled whether the user can change the colour.
 * @param allowCustom shows the hex field for any colour.
 * @param label the visible label above the swatches, already localised.
 * @param helperText guidance shown below the picker.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchColorPicker(
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    swatches: List<MonarchColorSwatch> = MonarchColorPickerDefaults.swatches(),
    enabled: Boolean = true,
    allowCustom: Boolean = true,
    label: String? = null,
    helperText: String? = null,
    labels: MonarchColorPickerLabels = MonarchColorPickerLabels(),
) {
    val colors = MonarchTheme.colors
    val selection = MonarchColorPickerTokens.selection(value)
    val groupName = monarchString(labels.swatches, R.string.monarch_colorpicker_swatches)
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(MonarchColorPickerTokens.Gap),
    ) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.label,
                color = MonarchColorPickerTokens.textColor(enabled, colors, strong = true),
            )
        }
        if (swatches.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .semantics { contentDescription = groupName },
            ) {
                swatches.forEach { swatch ->
                    val spoken = labels.swatch?.invoke(swatch.name, swatch.hex)
                        ?: stringResource(R.string.monarch_colorpicker_swatch, swatch.name, swatch.hex)
                    Swatch(
                        swatch = swatch,
                        selected = swatch.hex == selection,
                        enabled = enabled,
                        contentDescription = spoken,
                        onClick = { if (swatch.hex != selection) onValueChange(swatch.hex) },
                    )
                }
            }
        }
        if (allowCustom) {
            HexField(selection = selection, onValueChange = onValueChange, enabled = enabled, labels = labels)
        }
        if (helperText != null) {
            Text(
                text = helperText,
                style = MonarchTheme.typography.caption,
                color = MonarchColorPickerTokens.textColor(enabled, colors, strong = false),
            )
        }
    }
}

@Composable
private fun Swatch(
    swatch: MonarchColorSwatch,
    selected: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val ring = MonarchColorPickerTokens.swatchColors(selected, MonarchTheme.colors)
    val outline = if (selected) MonarchColorPickerTokens.RingWidth else MonarchShapeDefaults.BorderWidth
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(MonarchColorPickerTokens.SwatchSize)
                .border(outline, ring.ring, CircleShape)
                .padding(if (selected) MonarchColorPickerTokens.SelectedInset else outline)
                .background(swatch.color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                // The web draws a check on the chosen swatch.
                MonarchIcon(
                    MonarchGlyphs.Check,
                    contentDescription = null,
                    size = MonarchIconSize.Sm,
                    tint = ring.mark,
                )
            }
        }
    }
}

@Composable
private fun HexField(
    selection: String?,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    labels: MonarchColorPickerLabels,
) {
    var draft by rememberSaveable(selection) { mutableStateOf(selection.orEmpty()) }
    var invalid by rememberSaveable(selection) { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }
    val invalidText = monarchString(labels.invalid, R.string.monarch_colorpicker_invalid)
    val commit = {
        val result = MonarchColorPickerTokens.commit(draft, selection)
        draft = result.draft
        invalid = result.invalid
        result.hex?.let(onValueChange)
    }
    val preview = MonarchColorSwatch.color(selection.orEmpty())
    MonarchTextField(
        value = draft,
        onValueChange = {
            draft = it
            invalid = false
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (focused && !state.isFocused) commit()
                focused = state.isFocused
            },
        label = monarchString(labels.hex, R.string.monarch_colorpicker_hex),
        placeholder = MonarchColorPickerTokens.PLACEHOLDER,
        errorText = if (invalid) invalidText else null,
        enabled = enabled,
        leadingIcon = preview?.let { color ->
            {
                Box(
                    modifier = Modifier
                        .size(MonarchColorPickerTokens.PreviewSize)
                        .clearAndSetSemantics { }
                        .border(MonarchShapeDefaults.BorderWidth, MonarchTheme.colors.borderDefault, CircleShape)
                        .background(color, CircleShape),
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
}
