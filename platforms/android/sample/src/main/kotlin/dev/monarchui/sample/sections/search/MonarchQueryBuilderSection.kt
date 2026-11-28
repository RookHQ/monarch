package dev.monarchui.sample.sections.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.search.querybuilder.MonarchQueryBuilder
import dev.monarchui.components.search.querybuilder.MonarchQueryCombinator
import dev.monarchui.components.search.querybuilder.MonarchQueryGroup
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionField
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionFieldType
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionValue
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of QueryBuilder: a nested query and whether a sample record matches it. */
object MonarchQueryBuilderSection : CataloguePage {
    override val title = "QueryBuilder"

    private const val SAMPLE_AMOUNT = 120

    private val fields = listOf(
        MonarchConditionField(
            key = "status",
            label = "Status",
            type = MonarchConditionFieldType.Select,
            options = listOf(
                MonarchSelectOption("open", "Open"),
                MonarchSelectOption("pending", "Pending"),
                MonarchSelectOption("closed", "Closed"),
            ),
        ),
        MonarchConditionField(key = "title", label = "Title", type = MonarchConditionFieldType.Text),
        MonarchConditionField(key = "amount", label = "Amount", type = MonarchConditionFieldType.Number),
        MonarchConditionField(key = "flagged", label = "Flagged", type = MonarchConditionFieldType.Boolean),
    )

    private val start = MonarchQueryGroup(
        id = "root",
        rules = listOf(
            MonarchCondition("r1", "status", MonarchConditionOperator.Eq, MonarchConditionValue.Text("open")),
        ),
        groups = listOf(
            MonarchQueryGroup(
                id = "g1",
                combinator = MonarchQueryCombinator.Or,
                rules = listOf(
                    MonarchCondition("r2", "amount", MonarchConditionOperator.Gte, MonarchConditionValue.Number(500.0)),
                    MonarchCondition("r3", "flagged", MonarchConditionOperator.IsTrue),
                ),
            ),
        ),
    )

    @Composable
    override fun Content() {
        var query by remember { mutableStateOf(start) }
        val matches = query.matches(mapOf("status" to "open", "amount" to SAMPLE_AMOUNT, "flagged" to true))
        CatalogueCaption("Open records that are large or flagged")
        MonarchQueryBuilder(value = query, onValueChange = { query = it }, fields = fields)
        MonarchText(
            text = "A record (open, amount 120, flagged) ${if (matches) "matches" else "does not match"}.",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
