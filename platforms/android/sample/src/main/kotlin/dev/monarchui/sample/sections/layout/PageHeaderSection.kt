package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of PageHeader: a LargeTopAppBar with actions, then the status and tabs as content. */
object PageHeaderSection : CataloguePage {
    override val title = "PageHeader"

    private val tabs = listOf("Overview", "Invoices", "Contacts")

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var menu by rememberSaveable { mutableStateOf(false) }
        var lastAction by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Title, back, actions and an overflow menu")
        Column {
            LargeTopAppBar(
                title = { Text("Acme Ltd") },
                navigationIcon = {
                    IconButton(onClick = { lastAction = "Back" }) {
                        MonarchIcon(SampleIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { lastAction = "Add contact" }) {
                        MonarchIcon(SampleIcons.Plus, contentDescription = "Add contact")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(SampleIcons.MoreVertical, contentDescription = "More actions")
                        }
                        MonarchMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf("Rename", "Archive").forEach { action ->
                                MonarchMenuItem(
                                    text = action,
                                    onClick = {
                                        lastAction = action
                                        menu = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchBadge(text = "Active", tone = MonarchTone.Success)
                MonarchText(
                    text = "Updated 5 minutes ago",
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, name ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(name) })
                }
            }
        }
        if (lastAction.isNotEmpty()) {
            MonarchText(text = "Last action: $lastAction", style = MonarchTextStyle.Caption)
        }
    }
}
