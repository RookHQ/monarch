package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroup
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionEditor
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionParameter
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionParameterKind
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionType
import com.rookhq.monarch.components.workflow.actioneditor.MonarchRuleAction
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionEditor
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionField
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionFieldType
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionMatch
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val RuleFields = listOf(
    MonarchConditionField(
        key = "priority",
        label = "Priority",
        type = MonarchConditionFieldType.Select,
        options = listOf(MonarchSelectOption("urgent", "Urgent"), MonarchSelectOption("normal", "Normal")),
    ),
    MonarchConditionField(key = "subject", label = "Subject", type = MonarchConditionFieldType.Text),
)

private val RuleActionTypes = listOf(
    MonarchActionType(
        key = "team",
        label = "Assign to team",
        parameters = listOf(
            MonarchActionParameter(
                key = "team",
                label = "Team",
                kind = MonarchActionParameterKind.Select,
                options = listOf(MonarchSelectOption("billing", "Billing"), MonarchSelectOption("support", "Support")),
                required = true,
            ),
        ),
    ),
    MonarchActionType(key = "close", label = "Close the ticket"),
)

private val RuleTriggers = listOf(
    MonarchCheckboxGroupOption("created", "A ticket is created"),
    MonarchCheckboxGroupOption("updated", "A ticket is updated"),
)

/** The catalogue page of RuleBuilder: a recipe of existing components. */
object RuleBuilderSection : CataloguePage {
    override val title = "RuleBuilder"

    @Composable
    override fun Content() {
        var name by rememberSaveable { mutableStateOf("") }
        var on by rememberSaveable { mutableStateOf(true) }
        var stop by rememberSaveable { mutableStateOf(false) }
        var chosen by remember { mutableStateOf(listOf("created")) }
        var conditions by remember { mutableStateOf(emptyList<MonarchCondition>()) }
        var match by remember { mutableStateOf(MonarchConditionMatch.All) }
        var actions by remember { mutableStateOf(listOf(MonarchRuleAction("a1", "team"))) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        var saved by rememberSaveable { mutableStateOf(false) }
        val problems = listOfNotNull(
            "Give the rule a name.".takeIf { name.isBlank() },
            "Choose at least one trigger.".takeIf { chosen.isEmpty() },
            "Add at least one action.".takeIf { actions.isEmpty() },
            "Complete or remove the unfinished conditions."
                .takeIf { MonarchCondition.problems(conditions, RuleFields).isNotEmpty() },
            "Complete the unfinished actions."
                .takeIf { MonarchRuleAction.problems(actions, RuleActionTypes).isNotEmpty() },
        )
        CatalogueCaption("Name, when, if, then and options; Save rule lists what is missing")
        if (submitted && problems.isNotEmpty()) {
            MonarchBanner(
                message = problems.joinToString(" "),
                tone = MonarchTone.Danger,
                title = "Not saved yet",
                announcement = MonarchBannerAnnouncement.Assertive,
            )
        }
        MonarchTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            errorText = "Give the rule a name.".takeIf { submitted && name.isBlank() },
        )
        MonarchText(text = "When", style = MonarchTextStyle.Subheading)
        MonarchCheckboxGroup(
            options = RuleTriggers,
            value = chosen,
            onValueChange = { chosen = it },
            label = "Trigger",
            helperText = "The rule is checked whenever any of these happen.",
            errorText = "Choose at least one trigger.".takeIf { submitted && chosen.isEmpty() },
        )
        MonarchText(text = "If", style = MonarchTextStyle.Subheading)
        MonarchConditionEditor(
            value = conditions,
            onValueChange = { conditions = it },
            fields = RuleFields,
            match = match,
            onMatchChange = { match = it },
            showErrors = submitted,
        )
        MonarchText(text = "Then", style = MonarchTextStyle.Subheading)
        MonarchActionEditor(
            value = actions,
            onValueChange = { actions = it },
            actionTypes = RuleActionTypes,
            showErrors = submitted,
        )
        MonarchSwitchRow(
            checked = on,
            onCheckedChange = { on = it },
            label = "Rule is on",
            description = "Rules that are off are kept but never run.",
        )
        MonarchSwitchRow(
            checked = stop,
            onCheckedChange = { stop = it },
            label = "Stop processing",
            description = "Later rules are skipped when this one runs.",
        )
        MonarchButton(
            text = "Save rule",
            onClick = {
                submitted = true
                saved = problems.isEmpty()
            },
            variant = MonarchButtonVariant.Primary,
        )
        if (saved) MonarchText(text = "Saved")
    }
}
