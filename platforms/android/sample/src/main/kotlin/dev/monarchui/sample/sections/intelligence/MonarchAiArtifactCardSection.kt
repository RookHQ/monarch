package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.aiartifactcard.MonarchAiArtifactCard
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

private val Artifacts = listOf(
    Triple("Q3 summary", MonarchAiArtifactKind.Document, 3),
    Triple("Revenue by region", MonarchAiArtifactKind.Chart, 1),
    Triple("Import script", MonarchAiArtifactKind.Code, 2),
)

/** The catalogue page of AiArtifactCard. */
object MonarchAiArtifactCardSection : CataloguePage {
    override val title = "AiArtifactCard"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Tap a card to show its artifact")
        Artifacts.forEach { (title, kind, version) ->
            MonarchAiArtifactCard(
                title = title,
                kind = kind,
                version = version,
                onClick = { shown = title },
                description = if (shown == title) "On show" else null,
                active = shown == title,
                icon = { MonarchIcon(SampleIcons.Document, contentDescription = null) },
            )
        }
        CatalogueCaption("Disabled")
        MonarchAiArtifactCard(
            title = "Sales",
            kind = MonarchAiArtifactKind.Table,
            version = 1,
            onClick = {},
            enabled = false,
        )
    }
}
