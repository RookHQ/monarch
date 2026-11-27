package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewer
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerGranularity
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
