package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.navigation.tabs.MonarchTabs
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Tabs: the Monarch line tabs. */
object TabsSection : CataloguePage {
    override val title = "Tabs"

    private val sections = listOf("Summary", "Contacts", "Deals", "Invoices", "Files", "History")

    @Composable
    override fun Content() {
        var filter by rememberSaveable { mutableIntStateOf(0) }
        var section by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("With counts and a disabled tab, and the content of the selection")
        MonarchTabs(
            selectedIndex = filter,
            onSelectedIndexChange = { filter = it },
            panel = { MonarchText(text = if (it == 0) "12 open tickets" else "No closed tickets") },
        ) {
            tab("Open", count = "12", countTone = MonarchTone.Accent)
            tab("Closed")
            tab("Archived", enabled = false)
        }
        CatalogueCaption("Scrolls when the labels do not fit")
        MonarchTabs(
            selectedIndex = section,
            onSelectedIndexChange = { section = it },
            modifier = Modifier.width(280.dp),
        ) {
            sections.forEach { tab(it) }
        }
    }
}
