package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGrid
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGridSpan
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of DashboardGrid: switch to Arrange and move widgets with the buttons or a long press. */
object MonarchDashboardGridSection : CataloguePage {
    override val title = "DashboardGrid"

    private const val HEIGHT_STEPS = 6

    private val Widgets = listOf("Revenue" to "12,400", "Team load" to "78%", "Open tickets" to "31")

    @Composable
    override fun Content() {
        var layout by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var editing by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Arrange the widgets")
        MonarchButton(text = if (editing) "Done" else "Arrange", onClick = { editing = !editing })
        MonarchDashboardGrid(
            items = Widgets,
            key = { it.first },
            itemLabel = { it.first },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            layout = layout,
            onLayoutChange = { layout = it },
            span = { if (it.first == "Revenue") MonarchDashboardGridSpan.Two else MonarchDashboardGridSpan.One },
            editing = editing,
        ) { (name, value) ->
            MonarchWidget(title = name) { MonarchText(text = value, style = MonarchTextStyle.Title) }
        }
    }
}
