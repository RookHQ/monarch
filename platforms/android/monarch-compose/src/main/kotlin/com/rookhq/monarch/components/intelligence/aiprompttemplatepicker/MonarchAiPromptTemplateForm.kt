package com.rookhq.monarch.components.intelligence.aiprompttemplatepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The template page of `MonarchAiPromptTemplatePicker`: Back, the title and
 * description, one field per blank (or the template text when it has none),
 * the live preview and Use template.
 */
@Composable
internal fun MonarchAiPromptTemplateForm(
    template: MonarchAiPromptTemplate,
    labels: MonarchAiPromptTemplatePickerLabels,
    onBack: () -> Unit,
    onUse: (text: String, template: MonarchAiPromptTemplate, values: Map<String, String>) -> Unit,
) {
    val tokens = MonarchAiPromptTemplatePickerTokens
    val variables = template.resolvedVariables
    var values by rememberSaveable(template.id) { mutableStateOf(tokens.initialValues(variables)) }
    var showErrors by rememberSaveable(template.id) { mutableStateOf(false) }
    var announcement by rememberSaveable(template.id) { mutableStateOf("") }
    val resources = LocalResources.current
    val missingText: (String) -> String = { label ->
        labels.missing?.invoke(label)
            ?: resources.getString(R.string.monarch_aiprompttemplatepicker_missing, label.lowercase())
    }
    val missing = tokens.missing(variables, values)
    MonarchButton(
        text = monarchString(labels.back, R.string.monarch_aiprompttemplatepicker_back),
        onClick = onBack,
        variant = MonarchButtonVariant.Ghost,
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Text(
            text = template.title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.heading,
            color = MonarchTheme.colors.textDefault,
        )
        val description = template.description
        if (!description.isNullOrBlank()) {
            Text(text = description, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        }
    }
    if (variables.isEmpty()) {
        Block(monarchString(labels.template, R.string.monarch_aiprompttemplatepicker_template), template.body)
    }
    val optional = monarchString(labels.optional, R.string.monarch_aiprompttemplatepicker_optional)
    variables.forEach { variable ->
        MonarchTextField(
            value = values[variable.name].orEmpty(),
            onValueChange = { values = values + (variable.name to it) },
            modifier = Modifier.fillMaxWidth(),
            label = tokens.fieldLabel(variable, optional),
            placeholder = variable.placeholder,
            helperText = variable.helper,
            errorText = if (showErrors && variable.name in missing) missingText(variable.label) else null,
            singleLine = !variable.multiline,
        )
    }
    if (variables.isNotEmpty()) {
        Block(
            monarchString(labels.preview, R.string.monarch_aiprompttemplatepicker_preview),
            tokens.preview(template.body, variables, values),
        )
    }
    MonarchButton(
        text = monarchString(labels.use, R.string.monarch_aiprompttemplatepicker_use),
        onClick = {
            val first = missing.firstOrNull()
            if (first != null) {
                showErrors = true
                announcement = missingText(variables.first { it.name == first }.label)
            } else {
                val submitted = tokens.submitted(variables, values)
                onUse(MonarchAiPromptTemplate.fill(template.body, submitted), template, submitted)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        variant = MonarchButtonVariant.Primary,
    )
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
            if (announcement.isNotEmpty() && missing.isNotEmpty()) contentDescription = announcement
        },
    )
}

@Composable
private fun Block(title: String, text: String) {
    val colors = MonarchAiPromptTemplatePickerTokens.blockColors(MonarchTheme.colors)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchAiPromptTemplatePickerTokens.Gap)) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = MonarchAiPromptTemplatePickerTokens.headingColor(MonarchTheme.colors),
        )
        SelectionContainer {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .monarchSurface(colors, MonarchShapeDefaults.Control)
                    .padding(MonarchAiPromptTemplatePickerTokens.BlockPadding),
                style = MonarchTheme.typography.body,
                color = colors.content,
            )
        }
    }
}
