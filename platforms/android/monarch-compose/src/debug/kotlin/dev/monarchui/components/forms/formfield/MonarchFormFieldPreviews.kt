package dev.monarchui.components.forms.formfield

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchFormFieldPreview() {
    PreviewFrame {
        MonarchFormField(label = "Email", required = true, helperText = "We send receipts here.") {
            MonarchTextField(value = "", onValueChange = {}, modifier = it)
        }
        MonarchFormField(label = "Company", optionalText = "Optional", errorText = "Enter at most 80 characters.") {
            MonarchTextField(value = "Rook", onValueChange = {}, modifier = it)
        }
        MonarchFormField(
            label = "Weekly digest",
            layout = MonarchFormFieldLayout.Row,
            helperText = "A summary of activity, sent on Monday mornings.",
        ) {
            Switch(checked = true, onCheckedChange = {}, modifier = it)
        }
    }
}
