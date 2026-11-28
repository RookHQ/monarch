package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.schemadrivenform.MonarchSchemaDrivenForm
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionOperator
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldOption
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldType
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldValue
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of SchemaDrivenForm: a job application with a section shown only for remote work. */
object MonarchSchemaDrivenFormSection : CataloguePage {
    override val title = "SchemaDrivenForm"

    private const val SAVE_MILLIS = 800L
    private const val HEIGHT_STEPS = 6

    private val fields = listOf(
        MonarchFormFieldDefinition("about", MonarchFormFieldType.Section, "About you"),
        MonarchFormFieldDefinition("name", MonarchFormFieldType.ShortText, "Full name", required = true),
        MonarchFormFieldDefinition("email", MonarchFormFieldType.Email, "Email", required = true),
        MonarchFormFieldDefinition(
            "role",
            MonarchFormFieldType.Select,
            "Role",
            required = true,
            options = listOf(
                MonarchFormFieldOption("design", "Designer"),
                MonarchFormFieldOption("eng", "Engineer"),
            ),
        ),
        MonarchFormFieldDefinition("remote", MonarchFormFieldType.YesNo, "Do you want to work remotely?"),
        MonarchFormFieldDefinition(
            "setup",
            MonarchFormFieldType.Section,
            "Your setup",
            description = "Only for remote work.",
            conditions = listOf(MonarchCondition("c1", "remote", MonarchConditionOperator.IsTrue)),
        ),
        MonarchFormFieldDefinition("timezone", MonarchFormFieldType.ShortText, "Time zone", required = true),
    )

    @Composable
    override fun Content() {
        val answers = remember { mutableStateMapOf<String, MonarchFormFieldValue?>() }
        var readOnly by rememberSaveable { mutableStateOf(false) }
        var sent by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Answer Yes to remote work to show a section")
        MonarchSwitchRow(checked = readOnly, onCheckedChange = { readOnly = it }, label = "Read only")
        MonarchSchemaDrivenForm(
            fields = fields,
            values = answers,
            onValuesChange = { all, _ ->
                answers.clear()
                answers.putAll(all)
            },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            onSubmit = {
                delay(SAVE_MILLIS)
                sent = true
            },
            readOnly = readOnly,
        )
        if (sent) MonarchText("Application sent.")
    }
}
