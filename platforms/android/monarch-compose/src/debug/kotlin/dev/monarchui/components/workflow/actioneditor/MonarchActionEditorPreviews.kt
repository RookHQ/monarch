package dev.monarchui.components.workflow.actioneditor

import androidx.compose.runtime.Composable
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewTypes = listOf(
    MonarchActionType(
        key = "priority",
        label = "Set priority",
        parameters = listOf(
            MonarchActionParameter(
                key = "level",
                label = "Priority",
                kind = MonarchActionParameterKind.Select,
                options = listOf(MonarchSelectOption("high", "High"), MonarchSelectOption("low", "Low")),
                required = true,
            ),
        ),
    ),
    MonarchActionType(key = "close", label = "Close ticket", description = "Closes the ticket without a reply."),
)

@MonarchPreviews
@Composable
internal fun MonarchActionEditorPreview() {
    PreviewFrame {
        MonarchActionEditor(
            value = listOf(MonarchRuleAction("a1", "priority"), MonarchRuleAction("a2", "close")),
            onValueChange = {},
            actionTypes = PreviewTypes,
            showErrors = true,
        )
    }
}
