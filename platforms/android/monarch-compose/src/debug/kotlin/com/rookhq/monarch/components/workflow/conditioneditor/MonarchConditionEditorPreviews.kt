package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewFields = listOf(
    MonarchConditionField(
        key = "priority",
        label = "Priority",
        type = MonarchConditionFieldType.Select,
        options = listOf(MonarchSelectOption("high", "High"), MonarchSelectOption("low", "Low")),
    ),
    MonarchConditionField(key = "hours", label = "Hours since created", type = MonarchConditionFieldType.Number),
)

@MonarchPreviews
@Composable
internal fun MonarchConditionEditorPreview() {
    PreviewFrame {
        MonarchConditionEditor(
            value = listOf(
                MonarchCondition("c1", "priority", MonarchConditionOperator.Eq, MonarchConditionValue.Text("high")),
                MonarchCondition("c2", "hours", MonarchConditionOperator.Gt, MonarchConditionValue.Number("")),
            ),
            onValueChange = {},
            fields = PreviewFields,
            match = MonarchConditionMatch.All,
            onMatchChange = {},
            showErrors = true,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchConditionEditorEmptyPreview() {
    PreviewFrame {
        MonarchConditionEditor(value = emptyList(), onValueChange = {}, fields = PreviewFields)
    }
}
