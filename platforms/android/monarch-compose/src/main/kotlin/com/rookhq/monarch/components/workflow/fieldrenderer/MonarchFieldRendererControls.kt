package com.rookhq.monarch.components.workflow.fieldrenderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.files.fileupload.MonarchFileUpload
import com.rookhq.monarch.components.files.fileupload.MonarchFileUploadVariant
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.forms.numberinput.MonarchNumberInput
import com.rookhq.monarch.components.forms.radiorow.MonarchRadioRow
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingOption
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPicker
import com.rookhq.monarch.components.forms.ratingpicker.MonarchRatingPickerVariant
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType as Type

private typealias ControlTokens = MonarchFieldRendererTokens

/** The control of a question inside its MonarchFormField. [modifier] names it for TalkBack. */
@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
internal fun MonarchFieldRendererControl(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    modifier: Modifier,
    usable: Boolean,
    error: String?,
    text: MonarchFieldRendererText,
) {
    val chosen = (value as? MonarchFormFieldValue.Text)?.text
    when (field.type) {
        Type.Number -> MonarchNumberInput(
            value = (value as? MonarchFormFieldValue.Number)?.number?.takeUnless { it.isNaN() },
            onValueChange = { onValueChange(it?.let(MonarchFormFieldValue::Number)) },
            // A short field without steppers, as on the web: a count is a few digits, not a full-width line.
            modifier = modifier.widthIn(max = MonarchFieldRendererTokens.NumberMaxWidth).fillMaxWidth(),
            placeholder = field.placeholder,
            minimum = field.min,
            maximum = field.max,
            enabled = usable,
            readOnly = field.readOnly,
            showSteppers = false,
        )
        Type.Date -> {
            val millis = (value as? MonarchFormFieldValue.Date)?.millis
            MonarchFieldRendererDate(field, millis, onValueChange, modifier, usable, text)
        }
        Type.Time -> MonarchFieldRendererTime(chosen, onValueChange, modifier, usable, text)
        Type.Select -> MonarchSelect(
            options = field.options.map { MonarchSelectOption(it.value, it.label) },
            selected = chosen,
            onSelect = { onValueChange(MonarchFormFieldValue.Text(it)) },
            modifier = modifier.fillMaxWidth(),
            placeholder = field.placeholder ?: text.choose,
            enabled = usable,
        )
        Type.Radio -> Column(modifier.fillMaxWidth().selectableGroup()) {
            field.options.forEach { option ->
                MonarchRadioRow(
                    selected = chosen == option.value,
                    onClick = { onValueChange(MonarchFormFieldValue.Text(option.value)) },
                    label = option.label,
                    enabled = usable,
                )
            }
        }
        Type.YesNo -> Row(
            modifier = modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ControlTokens.ControlGap),
        ) {
            val answer = (value as? MonarchFormFieldValue.Flag)?.checked
            MonarchRadioRow(
                selected = answer == true,
                onClick = { onValueChange(MonarchFormFieldValue.Flag(true)) },
                label = text.yes,
                modifier = Modifier.weight(1f),
                enabled = usable,
            )
            MonarchRadioRow(
                selected = answer == false,
                onClick = { onValueChange(MonarchFormFieldValue.Flag(false)) },
                label = text.no,
                modifier = Modifier.weight(1f),
                enabled = usable,
            )
        }
        Type.Rating -> {
            val first = field.min?.toInt() ?: ControlTokens.DEFAULT_RATING_MIN
            val last = field.max?.toInt() ?: ControlTokens.DEFAULT_RATING_MAX
            MonarchRatingPicker(
                value = (value as? MonarchFormFieldValue.Number)?.number?.takeUnless { it.isNaN() }?.toInt(),
                onValueChange = { rating ->
                    onValueChange(rating?.let { MonarchFormFieldValue.Number(it.toDouble()) })
                },
                modifier = modifier,
                options = MonarchRatingOption.range(first, last),
                variant = MonarchRatingPickerVariant.Stars,
                enabled = usable,
                clearable = !field.required,
            )
        }
        Type.File -> MonarchFieldRendererFiles(field, value, onValueChange, modifier, usable, error, text)
        else -> MonarchTextField(
            value = chosen.orEmpty(),
            onValueChange = { typed ->
                onValueChange(MonarchFormFieldValue.Text(field.maxLength?.let(typed::take) ?: typed))
            },
            modifier = modifier.fillMaxWidth(),
            placeholder = field.placeholder ?: UrlPlaceholder.takeIf { field.type == Type.Url },
            enabled = usable,
            readOnly = field.readOnly,
            singleLine = field.type != Type.LongText,
            keyboardOptions = KeyboardOptions(keyboardType = ControlTokens.keyboard(field.type)),
        )
    }
}

private const val UrlPlaceholder = "https://"

/** A consent: one checkbox row with the statement, and its error under it. */
@Composable
internal fun MonarchFieldRendererConsent(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    usable: Boolean,
    error: String?,
) {
    MonarchCheckboxRow(
        checked = (value as? MonarchFormFieldValue.Flag)?.checked == true,
        onCheckedChange = { onValueChange(MonarchFormFieldValue.Flag(it)) },
        label = field.consentText ?: field.label,
        modifier = Modifier.semantics { if (error != null) error(error) },
        enabled = usable,
        description = field.description,
    )
    if (error != null) {
        MonarchText(
            text = error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTextStyle.Caption,
            color = ControlTokens.errorColor(MonarchTheme.colors),
        )
    }
}

/** A yes-or-no question as one switch row named by the question, as on the web. */
@Composable
internal fun MonarchFieldRendererSwitch(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    usable: Boolean,
    error: String?,
) {
    MonarchSwitchRow(
        checked = (value as? MonarchFormFieldValue.Flag)?.checked == true,
        onCheckedChange = { onValueChange(MonarchFormFieldValue.Flag(it)) },
        label = field.label,
        modifier = Modifier.semantics { if (error != null) error(error) },
        enabled = usable,
        description = field.description,
    )
    if (error != null) {
        MonarchText(
            text = error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTextStyle.Caption,
            color = ControlTokens.errorColor(MonarchTheme.colors),
        )
    }
}

/** A file question: the compact upload control while more files fit, then the chosen files with remove buttons. */
@Composable
@Suppress("LongParameterList")
private fun MonarchFieldRendererFiles(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    modifier: Modifier,
    usable: Boolean,
    error: String?,
    text: MonarchFieldRendererText,
) {
    val files = (value as? MonarchFormFieldValue.Files)?.files.orEmpty()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ControlTokens.ControlGap),
    ) {
        if (files.size < field.maxFiles && usable) {
            MonarchFileUpload(
                onFiles = { accepted, _ ->
                    val added = accepted.map { MonarchFormFieldFile(it.name, it.size, it) }
                    onValueChange(MonarchFormFieldValue.Files(files + added))
                },
                modifier = modifier.fillMaxWidth(),
                variant = MonarchFileUploadVariant.Compact,
                accept = field.accept,
                maxSize = field.maxFileSize,
                maxFiles = field.maxFiles,
                currentCount = files.size,
                enabled = usable,
                invalid = error != null,
            )
        }
        files.forEachIndexed { index, file ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = file.name, modifier = Modifier.weight(1f))
                if (usable) {
                    val name = text.removeFile(file.name)
                    MonarchButton(
                        text = text.remove,
                        onClick = {
                            onValueChange(MonarchFormFieldValue.Files(files.filterIndexed { i, _ -> i != index }))
                        },
                        modifier = Modifier.semantics { contentDescription = name },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
}
