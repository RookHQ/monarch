package com.rookhq.monarch.sample.sections.layout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The catalogue page of SplitLayout.
 *
 * Apps use the Material ListDetailPaneScaffold, as the inventory examples
 * show. The catalogue app does not depend on the adaptive artifacts (they are
 * implementation dependencies of monarch-compose), so this page shows the same
 * behaviour with a Row and BackHandler: both panes from 600dp, one pane at a
 * time below it.
 */
object SplitLayoutSection : CataloguePage {
    override val title = "SplitLayout"

    private const val TICKETS = 12
    private const val HEIGHT_STEPS = 3
    private const val LIST_SHARE = 0.4f
    private val MediumWidth = 600.dp

    private val tickets = List(TICKETS) { "Ticket ${it + 1}" }

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Two panes from 600dp, one pane below")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS)) {
            val width = maxWidth
            val twoPanes = width >= MediumWidth
            BackHandler(enabled = !twoPanes && selected != null) { selected = null }
            when {
                twoPanes -> Row {
                    TicketList(Modifier.width(width * LIST_SHARE)) { selected = it }
                    MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
                    TicketDetail(selected, onBack = null)
                }
                selected == null -> TicketList(Modifier.fillMaxWidth()) { selected = it }
                else -> TicketDetail(selected, onBack = { selected = null })
            }
        }
    }

    @Composable
    private fun TicketList(modifier: Modifier, onSelect: (String) -> Unit) {
        LazyColumn(modifier = modifier) {
            items(items = tickets, key = { it }) { ticket ->
                ListItem(
                    headlineContent = { Text(ticket) },
                    modifier = Modifier.clickable { onSelect(ticket) },
                    colors = ListItemDefaults.colors(containerColor = MonarchTheme.colors.bgCanvas),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TicketDetail(ticket: String?, onBack: (() -> Unit)?) {
        Column {
            if (onBack != null) {
                TopAppBar(
                    title = { Text(ticket.orEmpty()) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { MonarchIcon(SampleIcons.ArrowBack, contentDescription = "Back") }
                    },
                )
            }
            MonarchText(
                text = ticket ?: "Select a ticket",
                modifier = Modifier.padding(MonarchTheme.spacing.space4),
                style = MonarchTextStyle.Heading,
            )
        }
    }
}
