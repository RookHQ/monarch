package dev.monarchui.components.forms.schemadrivenform

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldType
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldValue
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

private val previewFields = listOf(
    MonarchFormFieldDefinition("contact", MonarchFormFieldType.Section, "Contact"),
    MonarchFormFieldDefinition("name", MonarchFormFieldType.ShortText, "Full name", required = true),
    MonarchFormFieldDefinition("remote", MonarchFormFieldType.YesNo, "Do you want to work remotely?"),
)

private val previewValues = mapOf<String, MonarchFormFieldValue?>(
    "name" to MonarchFormFieldValue.Text("Asha Rao"),
    "remote" to MonarchFormFieldValue.Flag(true),
)

/** A bounded height, as the lazy list needs. */
private val PreviewHeight = MonarchTokens.Space.space24.dp * 3

@MonarchPreviews
@Composable
internal fun MonarchSchemaDrivenFormPreview() {
    PreviewFrame {
        MonarchSchemaDrivenForm(
            fields = previewFields,
            values = previewValues,
            onValuesChange = { _, _ -> },
            modifier = Modifier.height(PreviewHeight),
            onSubmit = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchSchemaDrivenFormReadOnlyPreview() {
    PreviewFrame {
        MonarchSchemaDrivenForm(
            fields = previewFields,
            values = previewValues,
            onValuesChange = { _, _ -> },
            modifier = Modifier.height(PreviewHeight),
            readOnly = true,
            locale = Locale.US,
        )
    }
}
