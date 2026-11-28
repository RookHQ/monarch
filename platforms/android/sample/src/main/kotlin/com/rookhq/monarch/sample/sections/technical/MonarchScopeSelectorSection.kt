package com.rookhq.monarch.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.technical.scopeselector.MonarchScopeGroup
import com.rookhq.monarch.components.technical.scopeselector.MonarchScopeOption
import com.rookhq.monarch.components.technical.scopeselector.MonarchScopeSelector
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
