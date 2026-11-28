package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiAccessLevel
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScope
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScopeItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiPermissionScope. */
object MonarchAiPermissionScopeSection : CataloguePage {
    override val title = "AiPermissionScope"

    @Composable
    override fun Content() {
        var calendar by rememberSaveable { mutableIntStateOf(MonarchAiAccessLevel.Read.ordinal) }
        var drive by rememberSaveable { mutableIntStateOf(MonarchAiAccessLevel.Write.ordinal) }
        var managed by remember { mutableStateOf(false) }
        val editable = listOf(
            MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.entries[calendar]),
            MonarchAiPermissionScopeItem(
                "Drive",
                MonarchAiAccessLevel.entries[drive],
                resources = "Shared projects only",
            ),
        )
        CatalogueCaption("Read only")
        MonarchAiPermissionScope(scopes = editable + MonarchAiPermissionScopeItem("Mail", MonarchAiAccessLevel.None))
        CatalogueCaption("Editable, with a manage link")
        MonarchAiPermissionScope(
            scopes = editable,
            onScopesChange = { list ->
                calendar = list[0].access.ordinal
                drive = list[1].access.ordinal
            },
            onManage = { managed = true },
        )
        if (managed) MonarchText("The app opens its permission settings here.")
        CatalogueCaption("Empty")
        MonarchAiPermissionScope(scopes = emptyList())
    }
}
