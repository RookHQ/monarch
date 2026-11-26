package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.layout.resizablepanels.MonarchResizablePanels
import com.rookhq.monarch.components.layout.resizablepanels.MonarchResizablePanelsAxis
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ResizablePanels. */
object MonarchResizablePanelsSection : CataloguePage {
    override val title = "ResizablePanels"

    private const val HEIGHT_STEPS = 2
    private const val EDITOR_SHARE = 0.4f
    private const val HALF = 0.5f

    @Composable
    override fun Content() {
        val height = MonarchTheme.spacing.space24 * HEIGHT_STEPS
        var side by rememberSaveable { mutableFloatStateOf(EDITOR_SHARE) }
        CatalogueCaption("Side by side; stacked on a compact window")
        MonarchResizablePanels(
            fraction = side,
            onFractionChange = { side = it },
            modifier = Modifier.height(height),
            showGrip = true,
            first = { Panel("Editor") },
            second = { Panel("Preview") },
        )

        var always by rememberSaveable { mutableFloatStateOf(HALF) }
        CatalogueCaption("Side by side at every width")
        MonarchResizablePanels(
            fraction = always,
            onFractionChange = { always = it },
            modifier = Modifier.height(height),
            stacksWhenCompact = false,
            first = { Panel("Tickets") },
            second = { Panel("Conversation") },
        )

        var stacked by rememberSaveable { mutableFloatStateOf(HALF) }
        CatalogueCaption("Vertical")
        MonarchResizablePanels(
            fraction = stacked,
            onFractionChange = { stacked = it },
            modifier = Modifier.height(height),
            axis = MonarchResizablePanelsAxis.Vertical,
            showGrip = true,
            first = { Panel("Query") },
            second = { Panel("Results") },
        )
    }

    @Composable
    private fun Panel(name: String) {
        Box(modifier = Modifier.fillMaxSize().padding(MonarchTheme.spacing.space2)) {
            MonarchText(text = name, style = MonarchTextStyle.Label)
        }
    }
}
