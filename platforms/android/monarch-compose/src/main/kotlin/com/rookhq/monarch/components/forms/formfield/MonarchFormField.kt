package com.rookhq.monarch.components.forms.formfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.field.LocalMonarchFieldInvalid
import com.rookhq.monarch.internal.monarchString

/**
 * A visible label, one control, help text and an error message, with the
 * label and the error given to the control for TalkBack.
 *
 * Built by Monarch from a `Column` (or a `Row`): Compose has no label-for
 * wiring, so the field passes [control] a `Modifier` that the control must
 * apply. It names the control with [label] (and "Required" when [required])
 * and puts the control in the error state with [errorText], so TalkBack reads
 * the label, the role and the error together on the control. The visible
 * label, the required mark and the visible error are hidden from TalkBack to
 * avoid reading them twice; the help text stays readable as its own element.
 * A new error is announced through a polite live region.
 *
 * A Monarch field box inside (text field, select, date field) draws its
 * error border while the field has an error. The control is not told it is
 * disabled: pass `enabled` to it as well, for example to a switch. Do not
 * give the control a visible label of its own.
 *
 * @param label the visible label, and the TalkBack name of the control.
 * @param modifier the modifier of the field.
 * @param layout the label above the control, or at the start of a row with the control at the end.
 * @param enabled whether the field is enabled; a disabled field draws its texts in the disabled colour.
 * @param required shows a mark after the label and adds "Required" to the name of the control.
 * @param helperText help text, under the control or, in the row layout, under the label.
 * @param errorText the error message. Shown under the field in the danger colour and set as the error of the
 * control. In the stack layout it sits under the help text. A blank string is no error.
 * @param optionalText text after the label of an optional field, for example "Optional". Ignored when [required].
 * @param errorIcon an icon before the error message, for example a warning `MonarchIcon` of size `Sm` with a
 * `null` content description. Monarch bundles no icons; the message is text either way.
 * @param requiredLabel the word added to the name of the control of a required field. `null` uses the built-in
 * string `monarch_formfield_required`, "Required".
 * @param control the control, which must apply the `Modifier` it is given.
 */
@Composable
public fun MonarchFormField(
    label: String,
    modifier: Modifier = Modifier,
    layout: MonarchFormFieldLayout = MonarchFormFieldLayout.Stack,
    enabled: Boolean = true,
    required: Boolean = false,
    helperText: String? = null,
    errorText: String? = null,
    optionalText: String? = null,
    errorIcon: (@Composable () -> Unit)? = null,
    requiredLabel: String? = null,
    control: @Composable (controlModifier: Modifier) -> Unit,
) {
    val tokens = MonarchFormFieldTokens
    val error = tokens.error(errorText)
    val requiredWord = monarchString(requiredLabel, R.string.monarch_formfield_required)
    val requiredName = stringResource(R.string.monarch_formfield_name_required, label, requiredWord)
    val name = tokens.controlName(label, required, requiredName)
    val controlModifier = Modifier.semantics {
        contentDescription = name
        if (error != null) error(error)
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        when (layout) {
            MonarchFormFieldLayout.Stack -> {
                MonarchFieldLabel(label, enabled, required, optionalText)
                FieldControl(error != null) { control(controlModifier) }
                if (helperText != null) MonarchFieldHelper(helperText)
            }
            MonarchFormFieldLayout.Row -> Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                    MonarchFieldLabel(label, enabled, required, optionalText)
                    if (helperText != null) MonarchFieldHelper(helperText)
                }
                FieldControl(error != null) { control(controlModifier) }
            }
        }
        if (error != null) MonarchFieldMessage(error, MonarchFieldMessageTone.Error, errorIcon, hidden = true)
        // Always present, so a new error is announced once as a change of this live region.
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (error != null) contentDescription = error
            },
        )
    }
}

/** The control, told whether the field has an error so a Monarch field box inside draws its error border. */
@Composable
private fun FieldControl(invalid: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMonarchFieldInvalid provides invalid, content = content)
}
