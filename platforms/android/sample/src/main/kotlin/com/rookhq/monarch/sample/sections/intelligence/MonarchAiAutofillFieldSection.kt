package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillField
import com.rookhq.monarch.components.intelligence.aiautofillfield.MonarchAiAutofillState
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val Suggestion = "Acme Ltd"

/** The catalogue page of AiAutofillField: accept, clear or type to edit. */
object MonarchAiAutofillFieldSection : CataloguePage {
    override val title = "AiAutofillField"

    @Composable
    override fun Content() {
        var value by rememberSaveable { mutableStateOf(Suggestion) }
        var state by rememberSaveable { mutableStateOf(MonarchAiAutofillState.Suggested) }
        CatalogueCaption("Accept, clear, or type to edit the value")
        MonarchAiAutofillField(
            state = state,
            suggestion = Suggestion,
            onAccept = { state = MonarchAiAutofillState.Accepted },
            onClear = {
                value = ""
                state = MonarchAiAutofillState.Cleared
            },
            fieldName = "Company",
            source = "invoice.pdf",
            confidence = 0.92f,
            model = "Example model",
        ) {
            MonarchTextField(
                value = value,
                onValueChange = {
                    value = it
                    if (state != MonarchAiAutofillState.Cleared) state = MonarchAiAutofillState.Edited
                },
                label = "Company",
            )
        }
        MonarchButton(
            text = "Fill again",
            onClick = {
                value = Suggestion
                state = MonarchAiAutofillState.Suggested
            },
        )
    }
}
