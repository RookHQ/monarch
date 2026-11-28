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
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionEditor
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionParameter
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionParameterKind
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionType
import com.rookhq.monarch.components.workflow.actioneditor.MonarchActionValue
import com.rookhq.monarch.components.workflow.actioneditor.MonarchRuleAction
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ActionEditor: the actions of a ticket routing rule. */
object MonarchActionEditorSection : CataloguePage {
    override val title = "ActionEditor"

    private val types = listOf(
        MonarchActionType(
            key = "team",
            label = "Assign to team",
            group = "Assignment",
            parameters = listOf(
                MonarchActionParameter(
                    key = "team",
                    label = "Team",
                    kind = MonarchActionParameterKind.Select,
                    options = listOf(
                        MonarchSelectOption("billing", "Billing"),
                        MonarchSelectOption("support", "Support"),
                    ),
                    required = true,
                ),
            ),
        ),
        MonarchActionType(
            key = "notify",
            label = "Notify agents",
            description = "Sends a push notification to the team.",
            parameters = listOf(
                MonarchActionParameter(
                    key = "urgent",
                    label = "Mark as urgent",
                    kind = MonarchActionParameterKind.Boolean,
                ),
                MonarchActionParameter(
                    key = "message",
                    label = "Message",
                    kind = MonarchActionParameterKind.TextArea,
                    required = true,
                    visibleWhen = { (it["urgent"] as? MonarchActionValue.Flag)?.checked == true },
                ),
            ),
        ),
        MonarchActionType(
            key = "tags",
            label = "Add tags",
            parameters = listOf(
                MonarchActionParameter(key = "tags", label = "Tags", kind = MonarchActionParameterKind.Tags),
            ),
        ),
        MonarchActionType(
            key = "sms",
            label = "Send SMS",
            enabled = false,
            disabledReason = "Connect an SMS provider first.",
        ),
    )

    @Composable
    override fun Content() {
        var actions by remember {
            mutableStateOf(listOf(MonarchRuleAction("a1", "team"), MonarchRuleAction("a2", "notify")))
        }
        var submitted by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Then do this")
        MonarchActionEditor(
            value = actions,
            onValueChange = { actions = it },
            actionTypes = types,
            showErrors = submitted,
        )
        MonarchButton(text = "Save rule", onClick = { submitted = true })
        val incomplete = MonarchRuleAction.problems(actions, types).size
        if (submitted) {
            MonarchText(
                text = if (incomplete == 0) "Saved." else "$incomplete action(s) need attention.",
                color = MonarchTheme.colors.textMuted,
            )
        }
    }
}
