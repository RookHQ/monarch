package dev.monarchui.components.intelligence.aiautofillfield

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
