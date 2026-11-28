package dev.monarchui.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationLevel
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationPolicyEditor
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of EscalationPolicyEditor: who hears about an unacknowledged incident, and when. */
object MonarchEscalationPolicyEditorSection : CataloguePage {
    override val title = "EscalationPolicyEditor"

    private val targets = listOf(
        MonarchEntityPickerEntity("oncall", "On-call rota", description = "Rota"),
        MonarchEntityPickerEntity("lead", "Team lead", description = "Person"),
        MonarchEntityPickerEntity("slack", "#incidents", description = "Channel"),
    )

    private const val FIRST_MINUTES = 5
    private const val SECOND_MINUTES = 30

    @Composable
    override fun Content() {
        CatalogueCaption("Incident escalation")
        var levels by remember {
            mutableStateOf(
                listOf(
                    MonarchEscalationLevel("l1", FIRST_MINUTES, listOf("oncall")),
                    MonarchEscalationLevel("l2", SECOND_MINUTES, listOf("lead", "slack")),
                ),
            )
        }
        var saveTried by rememberSaveable { mutableStateOf(false) }
        MonarchEscalationPolicyEditor(
            value = levels,
            onValueChange = { levels = it },
            targets = targets,
            description = "Each level fires only if nobody has acknowledged yet.",
            showErrors = saveTried,
        )
        MonarchButton(text = "Save", onClick = { saveTried = true }, variant = MonarchButtonVariant.Primary)
        CatalogueCaption("Nothing to notify yet")
        MonarchEscalationPolicyEditor(
            value = listOf(MonarchEscalationLevel("l1", FIRST_MINUTES)),
            onValueChange = {},
            targets = emptyList(),
        )
    }
}
