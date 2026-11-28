package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMapping
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingDefaults
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingField
import dev.monarchui.components.datagrid.columnmapping.MonarchColumnMappingSource
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ColumnMapping: the mapping step of a contacts import. */
object MonarchColumnMappingSection : CataloguePage {
    override val title = "ColumnMapping"

    private const val HEIGHT_STEPS = 5

    private val sources = listOf(
        MonarchColumnMappingSource("Full Name", listOf("Sam Lee", "Ana Ruiz")),
        MonarchColumnMappingSource("E-mail", listOf("sam@example.com", "ana@example.com")),
        MonarchColumnMappingSource("Phone"),
        MonarchColumnMappingSource("Notes", listOf("Met at the fair")),
    )

    private val fields = listOf(
        MonarchColumnMappingField(id = "name", label = "Name", required = true),
        MonarchColumnMappingField(id = "email", label = "Email", required = true, aliases = listOf("E-mail", "Mail")),
        MonarchColumnMappingField(
            id = "phone",
            label = "Phone number",
            description = "International format, for example +44 20 7946 0000",
        ),
        MonarchColumnMappingField(id = "company", label = "Company"),
    )

    @Composable
    override fun Content() {
        var mapping by rememberSaveable {
            mutableStateOf(MonarchColumnMappingDefaults.mapping(MonarchColumnMappingDefaults.suggest(sources, fields)))
        }
        var showIssues by rememberSaveable { mutableStateOf(false) }
        val issues = MonarchColumnMappingDefaults.issues(mapping, fields)
        CatalogueCaption("Map the columns of contacts.csv")
        MonarchColumnMapping(
            sources = sources,
            mapping = mapping,
            fields = fields,
            onMappingChange = { mapping = it },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            showIssues = showIssues,
        )
        MonarchButton(
            text = "Next",
            onClick = { showIssues = true },
            variant = MonarchButtonVariant.Primary,
        )
        MonarchText(
            text = if (showIssues && issues.isEmpty) "Ready to import" else "Press Next to check the mapping",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
