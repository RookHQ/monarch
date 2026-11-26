package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchCondition
import com.rookhq.monarch.components.workflow.conditioneditor.MonarchConditionOperator
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFieldRenderer
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldOption
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of FieldRenderer: a job application form stored as data. */
object MonarchFieldRendererSection : CataloguePage {
    override val title = "FieldRenderer"

    private val form = listOf(
        MonarchFormFieldDefinition("about", MonarchFormFieldType.Section, "About you"),
        MonarchFormFieldDefinition("name", MonarchFormFieldType.ShortText, "Full name", required = true),
        MonarchFormFieldDefinition("email", MonarchFormFieldType.Email, "Email", required = true),
        MonarchFormFieldDefinition("start", MonarchFormFieldType.Date, "Earliest start date"),
        MonarchFormFieldDefinition(
            "team",
            MonarchFormFieldType.Select,
            "Team",
            options = listOf(MonarchFormFieldOption("eng", "Engineering"), MonarchFormFieldOption("design", "Design")),
        ),
        MonarchFormFieldDefinition("remote", MonarchFormFieldType.YesNo, "Do you want to work remotely?"),
        MonarchFormFieldDefinition(
            "where",
            MonarchFormFieldType.ShortText,
            "Which time zone?",
            conditions = listOf(MonarchCondition("c1", "remote", MonarchConditionOperator.IsTrue)),
        ),
        MonarchFormFieldDefinition("rating", MonarchFormFieldType.Rating, "How did you hear about the role?"),
        MonarchFormFieldDefinition("cv", MonarchFormFieldType.File, "CV", accept = listOf("application/pdf")),
        MonarchFormFieldDefinition(
            "terms",
            MonarchFormFieldType.Consent,
            "Privacy",
            required = true,
            consentText = "I agree that my answers are kept for 12 months.",
        ),
    )

    @Composable
    override fun Content() {
        CatalogueCaption("A form stored as data")
        val answers = remember { mutableStateMapOf<String, MonarchFormFieldValue?>() }
        var submitted by rememberSaveable { mutableStateOf(false) }
        form.filter { it.isVisible(answers) }.forEach { field ->
            MonarchFieldRenderer(
                field = field,
                value = answers[field.id],
                onValueChange = { answers[field.id] = it },
                problem = if (submitted) field.problem(answers[field.id]) else null,
                confidence = if (field.id == "email") AUTOFILL_CONFIDENCE else null,
            )
        }
        MonarchButton(text = "Submit", onClick = { submitted = true }, variant = MonarchButtonVariant.Primary)
    }

    private const val AUTOFILL_CONFIDENCE = 0.4f
}
