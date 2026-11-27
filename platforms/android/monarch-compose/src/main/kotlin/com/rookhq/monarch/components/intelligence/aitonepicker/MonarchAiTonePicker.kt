package com.rookhq.monarch.components.intelligence.aitonepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.forms.radio.MonarchRadio
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.LabelledControlRow
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.MonarchAiAccent
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Chooses the tone the assistant writes in: Professional, Technical, Casual
 * and Executive by default, each with a one-line description, and an optional
 * custom tone in the person's own words.
 *
 * In [MonarchAiTonePickerVariant.Cards] each tone is a row that selects as a
 * whole, built from a `MonarchRadio` with its label and description, in a
 * `selectableGroup`, so TalkBack reads "1 of 4" and the selected state. The
 * selected row has the subtle assistant fill, a thicker assistant border and
 * a filled radio mark. In [MonarchAiTonePickerVariant.Compact] the choice is
 * a `MonarchSelect`. Passing [onCustomTextChange] adds a Custom option
 * and, while it is selected, a `MonarchTextField` for the description.
 *
 * @param selection the id of the selected tone, or [MonarchAiTone.CUSTOM_ID] for the Custom option. An id
 * that matches no tone selects nothing.
 * @param onSelectionChange called with the id of the tone the user chose.
 * @param modifier the modifier of the picker.
 * @param tones the tones to offer. The default is [MonarchAiTone.defaults].
 * @param variant cards with descriptions, or a compact menu.
 * @param enabled whether the user can change the tone.
 * @param customText the person's own description of the tone. Kept when another tone is selected.
 * @param onCustomTextChange called as the custom description changes. Passing it adds the Custom option.
 * @param label the name of the choice. `null` uses `monarch_aitonepicker_label`, "Tone".
 * @param customLabel the name of the Custom option. `null` uses `monarch_aitonepicker_custom`.
 * @param customDetail the description of the Custom option. `null` uses `monarch_aitonepicker_custom_detail`.
 * @param customFieldLabel the label of the custom field. `null` uses `monarch_aitonepicker_custom_field`.
 * @param customPrompt the placeholder of the custom field. `null` uses `monarch_aitonepicker_custom_prompt`.
 */
@Composable
public fun MonarchAiTonePicker(
    selection: String,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    tones: List<MonarchAiTone> = MonarchAiTone.defaults(),
    variant: MonarchAiTonePickerVariant = MonarchAiTonePickerVariant.Cards,
    enabled: Boolean = true,
    customText: String = "",
    onCustomTextChange: ((String) -> Unit)? = null,
    label: String? = null,
    customLabel: String? = null,
    customDetail: String? = null,
    customFieldLabel: String? = null,
    customPrompt: String? = null,
) {
    val name = monarchString(label, R.string.monarch_aitonepicker_label)
    val custom = onCustomTextChange?.let {
        MonarchAiTone(
            id = MonarchAiTone.CUSTOM_ID,
            label = monarchString(customLabel, R.string.monarch_aitonepicker_custom),
            detail = monarchString(customDetail, R.string.monarch_aitonepicker_custom_detail),
        )
    }
    val options = MonarchAiTonePickerTokens.options(tones, custom)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAiTonePickerTokens.Gap)) {
        when (variant) {
            MonarchAiTonePickerVariant.Cards -> {
                Text(
                    text = name,
                    style = MonarchTheme.typography.label,
                    color = if (enabled) MonarchTheme.colors.textDefault else MonarchTheme.colors.textDisabled,
                )
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(MonarchAiTonePickerTokens.Gap),
                ) {
                    options.forEach { tone ->
                        ToneCard(tone, tone.id == selection, enabled) { onSelectionChange(tone.id) }
                    }
                }
            }
            MonarchAiTonePickerVariant.Compact -> ToneMenu(name, options, selection, enabled, onSelectionChange)
        }
        if (custom != null && selection == MonarchAiTone.CUSTOM_ID) {
            MonarchTextField(
                value = customText,
                onValueChange = { onCustomTextChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = monarchString(customFieldLabel, R.string.monarch_aitonepicker_custom_field),
                placeholder = monarchString(customPrompt, R.string.monarch_aitonepicker_custom_prompt),
                enabled = enabled,
                singleLine = false,
            )
        }
    }
}

@Composable
private fun ToneCard(tone: MonarchAiTone, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    val surface = MonarchAiTonePickerTokens.optionColors(selected, enabled, colors)
    val shape = MonarchTheme.shapes.md
    LabelledControlRow(
        label = tone.label,
        description = tone.detail.ifEmpty { null },
        enabled = enabled,
        controlAtEnd = false,
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(shape)
            .background(surface.container, shape)
            .border(MonarchAiTonePickerTokens.borderWidth(selected), surface.border ?: surface.container, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = MonarchAiTonePickerTokens.HorizontalPadding),
    ) {
        MonarchAiAccent { MonarchRadio(selected = selected, onClick = null, enabled = enabled) }
    }
}

@Composable
private fun ToneMenu(
    name: String,
    options: List<MonarchAiTone>,
    selection: String,
    enabled: Boolean,
    onSelectionChange: (String) -> Unit,
) {
    MonarchSelect(
        options = options.map { tone -> MonarchSelectOption(value = tone.id, label = tone.label) },
        selected = selection,
        onSelect = onSelectionChange,
        modifier = Modifier.fillMaxWidth(),
        label = name,
        enabled = enabled,
    )
}
