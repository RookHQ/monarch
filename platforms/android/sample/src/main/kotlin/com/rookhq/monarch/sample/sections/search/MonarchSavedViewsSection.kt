package com.rookhq.monarch.sample.sections.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.search.savedviews.MonarchSavedView
import com.rookhq.monarch.components.search.savedviews.MonarchSavedViews
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val SaveDelayMillis = 600L
private const val AllCount = 128
private const val OpenCount = 42

/** The catalogue page of SavedViews. */
object MonarchSavedViewsSection : CataloguePage {
    override val title = "SavedViews"

    @Composable
    override fun Content() {
        val views = remember {
            mutableStateListOf(
                MonarchSavedView("all", "All deals", count = AllCount, readOnly = true, group = "Built-in"),
                MonarchSavedView("open", "Open deals", count = OpenCount, default = true, group = "Mine"),
                MonarchSavedView("q4", "Closing this quarter", shared = true, group = "Mine"),
            )
        }
        var selected by rememberSaveable { mutableStateOf<String?>("open") }
        var modified by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("No action yet") }
        MonarchSwitchRow(checked = modified, onCheckedChange = { modified = it }, label = "Filters changed")
        MonarchText(text = last, style = MonarchTextStyle.Caption)
        CatalogueCaption("Every action")
        MonarchSavedViews(
            views = views,
            selectedId = selected,
            onSelect = {
                selected = it
                modified = false
            },
            modified = modified,
            onSaveNew = { name ->
                delay(SaveDelayMillis)
                val id = "view-${views.size}"
                views += MonarchSavedView(id, name, group = "Mine")
                selected = id
                modified = false
                last = "Saved $name"
            },
            onSaveChanges = {
                modified = false
                last = "Saved changes"
            },
            onDiscardChanges = {
                modified = false
                last = "Discarded changes"
            },
            onRename = { id, name ->
                delay(SaveDelayMillis)
                val index = views.indexOfFirst { it.id == id }
                val old = views[index]
                views[index] = MonarchSavedView(id, name, old.count, old.shared, old.default, old.readOnly, old.group)
                last = "Renamed to $name"
            },
            onDelete = { id ->
                delay(SaveDelayMillis)
                views.removeAll { it.id == id }
                selected = null
                last = "Deleted"
            },
            onSetDefault = { id ->
                views.indices.forEach { index ->
                    val old = views[index]
                    views[index] = MonarchSavedView(
                        old.id, old.name, old.count, old.shared, old.id == id, old.readOnly, old.group,
                    )
                }
                last = "Default changed"
            },
        )
        CatalogueCaption("Switching only, disabled")
        MonarchSavedViews(views = views, selectedId = selected, onSelect = {}, enabled = false)
    }
}
