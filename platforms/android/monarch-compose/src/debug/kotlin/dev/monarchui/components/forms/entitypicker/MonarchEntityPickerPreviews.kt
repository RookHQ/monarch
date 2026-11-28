package dev.monarchui.components.forms.entitypicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val people = listOf(
    MonarchEntityPickerEntity("ada", "Ada Lovelace", description = "Engineering", hint = "ada@example.com"),
    MonarchEntityPickerEntity("grace", "Grace Hopper", description = "Research"),
    MonarchEntityPickerEntity("alan", "Alan Turing", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchEntityPickerPreview() {
    PreviewFrame {
        MonarchEntityPicker(label = "Owner", entities = people, selected = null, onSelectedChange = { _: String? -> })
        MonarchEntityPicker(label = "Owner", entities = people, selected = "ada", onSelectedChange = { _: String? -> })
        MonarchEntityPicker(
            label = "Reviewers",
            entities = people,
            selected = listOf("ada", "grace"),
            onSelectedChange = { _: List<String> -> },
            helperText = "They are notified.",
        )
        MonarchEntityPicker(
            label = "Owner",
            entities = people,
            selected = null,
            onSelectedChange = { _: String? -> },
            required = true,
            errorText = "Choose an owner.",
        )
        MonarchEntityPicker(
            label = "Owner",
            entities = people,
            selected = "grace",
            onSelectedChange = { _: String? -> },
            enabled = false,
        )
    }
}
