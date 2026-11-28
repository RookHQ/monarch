package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of OverflowTabs: PrimaryScrollableTabRow scrolls the tabs that do not fit. */
object OverflowTabsSection : CataloguePage {
    override val title = "OverflowTabs"

    private val sections = listOf(
        "Overview", "Activity", "Invoices", "Contacts", "Files", "Notes", "Integrations", "Settings",
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Eight sections; swipe the tabs sideways")
        PrimaryScrollableTabRow(selectedTabIndex = selected) {
            sections.forEachIndexed { index, name ->
                Tab(selected = selected == index, onClick = { selected = index }, text = { Text(name) })
            }
        }
        MonarchText(text = "The ${sections[selected]} section.")
    }
}
