package com.rookhq.monarch.components.workflow.actioneditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
