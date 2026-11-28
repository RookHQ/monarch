package dev.monarchui.components.identity.assigneepicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewPeople = listOf(
    MonarchAssigneePerson("ada", "Ada Moss", email = "ada@example.com"),
    MonarchAssigneePerson("ben", "Ben Okafor", email = "ben@example.com", hint = "Away"),
)

@MonarchPreviews
@Composable
internal fun MonarchAssigneePickerPreview() {
    PreviewFrame {
        MonarchAssigneePicker(people = PreviewPeople, selected = null, onSelectedChange = {}, currentUserId = "ada")
        MonarchAssigneePicker(people = PreviewPeople, selected = "ben", onSelectedChange = {}, currentUserId = "ada")
        MonarchAssigneePicker(people = PreviewPeople, selected = listOf("ada", "ben"), onSelectedChange = {})
    }
}
