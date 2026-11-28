package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchFieldRendererPreview() {
    PreviewFrame {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            MonarchFieldRenderer(
                field = MonarchFormFieldDefinition("s", MonarchFormFieldType.Section, "About you"),
                value = null,
                onValueChange = {},
            )
            MonarchFieldRenderer(
                field = MonarchFormFieldDefinition("email", MonarchFormFieldType.Email, "Work email", required = true),
                value = MonarchFormFieldValue.Text("asha@"),
                onValueChange = {},
                problem = MonarchFormFieldProblem.Email,
            )
            MonarchFieldRenderer(
                field = MonarchFormFieldDefinition("company", MonarchFormFieldType.ShortText, "Company"),
                value = MonarchFormFieldValue.Text("Rook"),
                onValueChange = {},
                confidence = 0.4f,
            )
            MonarchFieldRenderer(
                field = MonarchFormFieldDefinition("remote", MonarchFormFieldType.YesNo, "Remote role?"),
                value = MonarchFormFieldValue.Flag(true),
                onValueChange = {},
            )
        }
    }
}
