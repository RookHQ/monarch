package dev.monarchui.components.search.querybuilder

import androidx.compose.runtime.Composable
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionFieldType
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
