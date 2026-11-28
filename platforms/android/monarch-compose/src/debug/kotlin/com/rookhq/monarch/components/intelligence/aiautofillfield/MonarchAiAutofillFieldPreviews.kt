package com.rookhq.monarch.components.intelligence.aiautofillfield

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiAutofillFieldPreview() {
    PreviewFrame {
        MonarchAiAutofillState.entries.forEach { state ->
            MonarchAiAutofillField(
                state = state,
                suggestion = "Acme Ltd",
                onAccept = {},
                onClear = {},
                fieldName = "Company",
                source = "invoice.pdf",
                confidence = 0.92f,
            ) {
                MonarchTextField(value = "Acme Ltd", onValueChange = {}, label = "Company")
            }
        }
    }
}
