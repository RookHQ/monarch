package dev.monarchui.components.datagrid.columnmapping

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

private val PreviewSources = listOf(
    MonarchColumnMappingSource("Full Name", listOf("Sam Lee", "Ana Ruiz")),
    MonarchColumnMappingSource("E-mail", listOf("sam@example.com")),
    MonarchColumnMappingSource("Notes"),
)

private val PreviewFields = listOf(
    MonarchColumnMappingField(id = "name", label = "Name", required = true),
    MonarchColumnMappingField(id = "email", label = "Email", required = true, aliases = listOf("E-mail")),
    MonarchColumnMappingField(id = "company", label = "Company"),
)

private const val PreviewHeightSteps = 6

@MonarchPreviews
@Composable
internal fun MonarchColumnMappingPreview() {
    PreviewFrame {
        MonarchColumnMapping(
            sources = PreviewSources,
            mapping = mapOf("Full Name" to "name", "Notes" to "name"),
            fields = PreviewFields,
            onMappingChange = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
        )
    }
}
