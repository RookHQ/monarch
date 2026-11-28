package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionEditor
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionField
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionFieldType
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionMatch
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionValue
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ConditionEditor: a ticket rule with every kind of value and a sample ticket. */
object MonarchConditionEditorSection : CataloguePage {
    override val title = "ConditionEditor"

    private const val SAMPLE_HOURS = 6

    private val fields = listOf(
        MonarchConditionField(
            key = "priority",
            label = "Priority",
            type = MonarchConditionFieldType.Select,
            options = listOf(
                MonarchSelectOption("urgent", "Urgent"),
                MonarchSelectOption("high", "High"),
                MonarchSelectOption("normal", "Normal"),
            ),
        ),
        MonarchConditionField(key = "subject", label = "Subject", type = MonarchConditionFieldType.Text),
        MonarchConditionField(
            key = "hours",
            label = "Hours since created",
            type = MonarchConditionFieldType.Number,
            placeholder = "Hours",
        ),
        MonarchConditionField(key = "due", label = "Due date", type = MonarchConditionFieldType.Date),
        MonarchConditionField(key = "tags", label = "Tags", type = MonarchConditionFieldType.Tags),
        MonarchConditionField(key = "vip", label = "VIP customer", type = MonarchConditionFieldType.Boolean),
    )

    private val start = listOf(
        MonarchCondition("c1", "priority", MonarchConditionOperator.In, MonarchConditionValue.Values(listOf("urgent"))),
        MonarchCondition("c2", "hours", MonarchConditionOperator.Gt, MonarchConditionValue.Number("4")),
    )

    @Composable
    override fun Content() {
        var conditions by remember { mutableStateOf(start) }
        var match by rememberSaveable { mutableStateOf(MonarchConditionMatch.All) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        val sample = mapOf("priority" to "urgent", "hours" to SAMPLE_HOURS, "vip" to true)
        val problems = MonarchCondition.problems(conditions, fields)
        CatalogueCaption("Escalate a ticket when")
        MonarchConditionEditor(
            value = conditions,
            onValueChange = { conditions = it },
            fields = fields,
            match = match,
            onMatchChange = { match = it },
            showErrors = submitted,
        )
        MonarchButton(text = "Save rule", onClick = { submitted = true })
        val matches = MonarchCondition.evaluate(conditions, sample, match)
        MonarchText(
            text = if (problems.isNotEmpty() && submitted) {
                "${problems.size} condition(s) need attention."
            } else {
                "An urgent ticket open for 6 hours from a VIP ${if (matches) "matches" else "does not match"}."
            },
            color = MonarchTheme.colors.textMuted,
        )
    }
}
