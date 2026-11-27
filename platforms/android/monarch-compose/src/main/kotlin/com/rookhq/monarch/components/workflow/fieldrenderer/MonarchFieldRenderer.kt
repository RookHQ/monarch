package com.rookhq.monarch.components.workflow.fieldrenderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroup
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.forms.combobox.MonarchComboboxOption
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.forms.multiselect.MonarchMultiSelect
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType as Type

private typealias Tokens = MonarchFieldRendererTokens

/**
 * Renders one form question from data: the right Monarch input for its type, with the label, help, required
 * mark, error and an optional "filled in automatically" note. Used for forms that admins author and store as
 * data, and for previews of a form builder.
 *
 * Built by Monarch as a `when` over [MonarchFormFieldDefinition.type]: text, email, phone and web address are a
 * `MonarchTextField` with the matching keyboard; a number is a `MonarchNumberInput`; a date and a time are a
 * `MonarchButton` that opens the Material 3 `DatePickerDialog` or `TimePickerDialog`; one option is a
 * `MonarchSelect` or `MonarchRadioRow`s; several are a `MonarchMultiSelect` or a `MonarchCheckboxGroup`; yes or
 * no is two `MonarchRadioRow`s; a consent is a `MonarchCheckboxRow`; a rating is a `MonarchRatingPicker`; files
 * are a compact `MonarchFileUpload` with the chosen files listed under it; a section is a heading. Inputs without
 * a label of their own sit in a `MonarchFormField`, which names them for TalkBack and announces a new error.
 *
 * The renderer holds no answer: [value] and [onValueChange] are the app's. [problem] (from
 * [MonarchFormFieldDefinition.problem]) shows its built-in message; [errorText], such as a server error, wins
 * over it. Use [MonarchFormFieldDefinition.isVisible] to leave out questions whose conditions do not hold.
 *
 * @param field the question.
 * @param value the current answer, or `null` for none.
 * @param onValueChange called with the new answer; `null` when it is cleared.
 * @param modifier the modifier of the question.
 * @param enabled whether the answer can be changed.
 * @param problem a problem to show as the error, with its built-in message.
 * @param errorText an error message to show; replaces the message of [problem].
 * @param confidence when the answer was filled in automatically (for example from an uploaded document), the
 * confidence from 0 to 1. Shows a note under the question; below [confidenceThreshold] it asks for a check.
 * @param confidenceThreshold the confidence under which the note asks people to check the answer.
 * @param labels replacements for the built-in strings.
 * @param yesNoAsSwitch draws a yes-or-no question as one switch row named by the question, as the web does, instead
 * of the Yes and No choices. The question stays unanswered (`null`) until the switch is turned.
 */
@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
public fun MonarchFieldRenderer(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    problem: MonarchFormFieldProblem? = null,
    errorText: String? = null,
    confidence: Float? = null,
    confidenceThreshold: Float = Tokens.DEFAULT_CONFIDENCE_THRESHOLD,
    labels: MonarchFieldRendererLabels = MonarchFieldRendererLabels(),
    yesNoAsSwitch: Boolean = false,
) {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val text = remember(labels, resources, locale) { MonarchFieldRendererText(labels, resources, locale) }
    val colors = MonarchTheme.colors
    val error = errorText?.takeIf { it.isNotBlank() } ?: problem?.let(text::message)
    val unsure = confidence != null && confidence < confidenceThreshold
    // Read-only text keeps a readable input; other read-only questions are disabled, as on the web.
    val usable = enabled && !(field.readOnly && !Tokens.keepsReadOnlyInput(field.type))
    val options = field.options
    // As on the web, the "filled in automatically" pill and the helper text sit between the label and the control
    // for a question drawn as a labelled field.
    val notesAbove = field.type != Type.Section && field.type != Type.Consent && field.type != Type.MultiSelect &&
        field.type != Type.Checkboxes && (field.type != Type.YesNo || !yesNoAsSwitch)
    val aiNote: (@Composable () -> Unit)? = if (confidence != null && field.type != Type.Section) {
        {
            val noteColor = Tokens.aiNoteColor(colors, unsure)
            Row(
                modifier = Modifier
                    .background(
                        if (unsure) colors.warningSubtle else colors.aiSubtle,
                        MonarchTheme.shapes.full,
                    )
                    .padding(horizontal = MonarchTheme.spacing.space2, vertical = MonarchTheme.spacing.space1),
                horizontalArrangement = Arrangement.spacedBy(Tokens.NoteGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides noteColor) {
                    Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Bot() }
                }
                MonarchText(
                    text = if (unsure) text.aiCheck else text.aiFilled,
                    style = MonarchTextStyle.Caption,
                    color = noteColor,
                )
            }
        }
    } else {
        null
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.Gap)) {
        when (field.type) {
            Type.Section -> {
                MonarchText(text = field.label, style = MonarchTextStyle.Subheading, heading = true)
                field.description?.let {
                    MonarchText(text = it, style = MonarchTextStyle.Caption, color = Tokens.noteColor(colors))
                }
            }
            Type.Consent -> MonarchFieldRendererConsent(field, value, onValueChange, usable, error)
            Type.YesNo -> if (yesNoAsSwitch) {
                MonarchFieldRendererSwitch(field, value, onValueChange, usable, error)
            } else {
                MonarchFormField(
                    label = field.label,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = usable,
                    required = field.required,
                    errorText = error,
                ) { controlModifier ->
                    FieldNotesAbove(field.description, aiNote) {
                        MonarchFieldRendererControl(field, value, onValueChange, controlModifier, usable, error, text)
                    }
                }
            }
            Type.MultiSelect -> MonarchMultiSelect(
                label = field.label,
                options = options.map { MonarchComboboxOption(it.value, it.label) },
                selected = (value as? MonarchFormFieldValue.Values)?.values.orEmpty(),
                onSelectedChange = { onValueChange(MonarchFormFieldValue.Values(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = usable,
                required = field.required,
                placeholder = field.placeholder ?: text.choose,
                helperText = field.description,
                errorText = error,
                maxSelections = field.maxSelections,
            )
            Type.Checkboxes -> MonarchCheckboxGroup(
                options = options.map { MonarchCheckboxGroupOption(it.value, it.label) },
                value = (value as? MonarchFormFieldValue.Values)?.values.orEmpty(),
                onValueChange = { chosen ->
                    onValueChange(MonarchFormFieldValue.Values(options.map { it.value }.filter { it in chosen }))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = usable,
                required = field.required,
                invalid = error != null,
                label = field.label,
                helperText = field.description,
                errorText = error,
            )
            else -> MonarchFormField(
                label = field.label,
                modifier = Modifier.fillMaxWidth(),
                enabled = usable,
                required = field.required,
                errorText = error,
            ) { controlModifier ->
                FieldNotesAbove(field.description, aiNote) {
                    MonarchFieldRendererControl(field, value, onValueChange, controlModifier, usable, error, text)
                }
            }
        }
        if (!notesAbove) aiNote?.invoke()
    }
}

/** The note and the helper text above the control, as the web draws them, then the control itself. */
@Composable
private fun FieldNotesAbove(helper: String?, aiNote: (@Composable () -> Unit)?, control: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1_5)) {
        aiNote?.let { Box(Modifier.align(Alignment.Start)) { it() } }
        helper?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted) }
        control()
    }
}
