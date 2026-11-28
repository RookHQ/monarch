package com.rookhq.monarch.sample.sections.forms

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
import com.rookhq.monarch.components.forms.schemadrivenform.MonarchSchemaDrivenForm
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldOption
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
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
