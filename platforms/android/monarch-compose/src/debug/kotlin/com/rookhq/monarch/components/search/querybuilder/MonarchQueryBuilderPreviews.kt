package com.rookhq.monarch.components.search.querybuilder

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionField
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionFieldType
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewFields = listOf(
    MonarchConditionField(key = "flagged", label = "Flagged", type = MonarchConditionFieldType.Boolean),
)

@MonarchPreviews
@Composable
internal fun MonarchQueryBuilderPreview() {
    PreviewFrame {
        MonarchQueryBuilder(
            value = MonarchQueryGroup(
                id = "root",
                rules = listOf(MonarchCondition("r1", "flagged", MonarchConditionOperator.IsTrue)),
                groups = listOf(MonarchQueryGroup(id = "g1", combinator = MonarchQueryCombinator.Or)),
            ),
            onValueChange = {},
            fields = PreviewFields,
        )
    }
}
