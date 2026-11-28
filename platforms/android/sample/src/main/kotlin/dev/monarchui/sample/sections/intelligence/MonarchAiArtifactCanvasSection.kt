@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvas
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvasPlacement
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiCanvasArtifact
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of AiArtifactCanvas: a sheet on phones, and a pane. */
object MonarchAiArtifactCanvasSection : CataloguePage {
    override val title = "AiArtifactCanvas"

    private const val PaneHeight = 480
    private const val SimulatedSaveMillis = 800L

    private val summary = MonarchAiCanvasArtifact(
        id = "summary",
        kind = MonarchAiArtifactKind.Document,
        title = "Q3 summary",
        version = 3,
        content = "## Revenue\n\nRevenue grew **12%** over the quarter.\n\n- New accounts: 48\n- Churn: 2%\n",
    )
    private val page = MonarchAiCanvasArtifact(
        id = "landing",
        kind = MonarchAiArtifactKind.Html,
        title = "Landing page",
        version = 1,
        content = "<h1>Welcome</h1><p>A page drafted by the assistant.</p><script>alert('blocked')</script>",
    )
    private val script = MonarchAiCanvasArtifact(
        id = "script",
        kind = MonarchAiArtifactKind.Code,
        title = "Import script",
        version = 2,
        content = "fun total(values: List<Int>): Int =\n    values.sum()\n",
        language = "kotlin",
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf<String?>(null) }
        var saved by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A sheet on phones, a pane on wide windows")
        MonarchButton(
            text = "Open the summary",
            onClick = { open = summary.id },
            variant = MonarchButtonVariant.Primary,
        )
        MonarchButton(text = "Open the web page", onClick = { open = page.id })
        MonarchText(text = "Saved: $saved", color = MonarchTheme.colors.textMuted)
        listOf(summary, page).forEach { artifact ->
            MonarchAiArtifactCanvas(
                artifact = artifact,
                open = open == artifact.id,
                onDismissRequest = { open = null },
                onDownload = {},
                onSave = {
                    delay(SimulatedSaveMillis)
                    saved += 1
                },
            )
        }
        CatalogueCaption("A pane in the layout: code has no preview")
        MonarchAiArtifactCanvas(
            artifact = script,
            open = true,
            onDismissRequest = {},
            modifier = Modifier.height(PaneHeight.dp),
            placement = MonarchAiArtifactCanvasPlacement.Inline,
        )
    }
}
