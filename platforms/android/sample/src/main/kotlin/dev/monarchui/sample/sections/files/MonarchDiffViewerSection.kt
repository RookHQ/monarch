package dev.monarchui.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.files.diffviewer.MonarchDiffViewer
import dev.monarchui.components.files.diffviewer.MonarchDiffViewerGranularity
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val ListHeightSteps = 4

/** The catalogue page of DiffViewer: two versions of release notes, with folding and the word switch. */
object MonarchDiffViewerSection : CataloguePage {
    override val title = "DiffViewer"

    private val before = listOf(
        "Release notes",
        "Ship on Friday.",
        "Tell support.",
        "Update the docs.",
        "Check the dashboards.",
        "Write the changelog.",
        "Thank the team.",
        "Keep the old banner.",
    )
    private val after = before.map {
        when (it) {
            "Ship on Friday." -> "Ship on Monday."
            "Keep the old banner." -> "Update the status page."
            else -> it
        }
    } + "Close the incident."

    @Composable
    override fun Content() {
        var words by rememberSaveable { mutableStateOf(true) }
        var restored by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Version 3 to version 4")
        MonarchSwitchRow(checked = words, onCheckedChange = { words = it }, label = "Mark changed words")
        MonarchDiffViewer(
            before = before,
            after = after,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * ListHeightSteps),
            granularity = if (words) MonarchDiffViewerGranularity.Word else MonarchDiffViewerGranularity.Line,
            context = 1,
            beforeLabel = "Version 3",
            afterLabel = "Version 4",
        ) { MonarchButton(text = "Restore", onClick = { restored = true }, size = MonarchButtonSize.Sm) }
        if (restored) MonarchText(text = "Version 3 restored.", color = MonarchTheme.colors.textMuted)
    }
}
