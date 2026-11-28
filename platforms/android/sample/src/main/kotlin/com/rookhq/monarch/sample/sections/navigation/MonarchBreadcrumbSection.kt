package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumb
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumbItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Breadcrumb. */
object MonarchBreadcrumbSection : CataloguePage {
    override val title = "Breadcrumb"

    private val folders = listOf("Drive", "Clients", "Acme Ltd", "2026", "Contracts", "Signed")

    @Composable
    override fun Content() {
        // How deep the person is in the folders: tapping a level trims the path, as a back stack would.
        var depth by rememberSaveable { mutableIntStateOf(folders.size) }
        val path = folders.take(depth).map { MonarchBreadcrumbItem(it) }
        CatalogueCaption("A folder path; the middle levels collapse into a menu")
        MonarchBreadcrumb(items = path, onItemClick = { item -> depth = folders.indexOf(item.id) + 1 })
        MonarchText(text = "Showing ${folders[depth - 1]}", style = MonarchTextStyle.Heading)
        if (depth < folders.size) {
            MonarchButton(
                text = "Go back to Signed",
                onClick = { depth = folders.size },
                variant = MonarchButtonVariant.Ghost,
            )
        }
        CatalogueCaption("A level without a screen")
        MonarchBreadcrumb(
            items = listOf(MonarchBreadcrumbItem("Archive", linked = false), MonarchBreadcrumbItem("Q3 reports")),
            onItemClick = {},
        )
        CatalogueCaption("Disabled")
        MonarchBreadcrumb(items = folders.take(2).map { MonarchBreadcrumbItem(it) }, onItemClick = {}, enabled = false)
    }
}
