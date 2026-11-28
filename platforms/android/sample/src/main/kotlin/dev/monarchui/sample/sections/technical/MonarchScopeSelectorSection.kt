package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.technical.scopeselector.MonarchScopeGroup
import dev.monarchui.components.technical.scopeselector.MonarchScopeOption
import dev.monarchui.components.technical.scopeselector.MonarchScopeSelector
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ScopeSelector: the scopes of a new API key. */
object MonarchScopeSelectorSection : CataloguePage {
    override val title = "ScopeSelector"

    private val groups = listOf(
        MonarchScopeGroup(
            id = "projects",
            label = "Projects API",
            description = "Owned by Platform",
            scopes = listOf(
                MonarchScopeOption("projects:read", "Read projects", "List and open projects."),
                MonarchScopeOption("projects:write", "Change projects", "Create, rename and archive projects."),
                MonarchScopeOption(
                    "projects:admin",
                    "Administer projects",
                    "Granted by an owner only.",
                    enabled = false,
                ),
            ),
        ),
        MonarchScopeGroup(
            id = "billing",
            label = "Billing API",
            scopes = listOf(MonarchScopeOption("billing:read", "Read invoices")),
        ),
        MonarchScopeGroup(id = "files", label = "Files API", scopes = emptyList(), loading = true),
    )

    @Composable
    override fun Content() {
        var scopes by remember { mutableStateOf(listOf("projects:read")) }
        CatalogueCaption("Scopes of a new API key")
        MonarchScopeSelector(
            groups = groups,
            value = scopes,
            onValueChange = { scopes = it },
            defaultExpanded = setOf("projects"),
        )
        MonarchText(
            text = if (scopes.isEmpty()) "No scopes yet." else scopes.joinToString(" "),
            color = MonarchTheme.colors.textMuted,
        )
    }
}
