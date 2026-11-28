package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanel
import com.rookhq.monarch.components.files.versionhistorypanel.MonarchVersionHistoryPanelVersion
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of VersionHistoryPanel, in a bounded box as it would be in a sheet. */
object MonarchVersionHistoryPanelSection : CataloguePage {
    override val title = "VersionHistoryPanel"

    private const val HOUR = 3_600_000L
    private const val WORK_MILLIS = 600L
    private const val HEIGHT_STEPS = 5

    private val current = listOf(
        "We ship the new billing page on Monday.",
        "Support is told on Friday.",
        "The status page is updated after the release.",
    )

    private val contents = mapOf(
        "v3" to current,
        "v2" to listOf("We ship the new billing page on Monday.", "Support is told on Thursday."),
        "v1" to listOf("We ship on Friday.", "Support is told on Thursday."),
    )

    @Composable
    override fun Content() {
        val now = rememberSaveable { System.currentTimeMillis() }
        var selection by rememberSaveable { mutableStateOf<String?>(null) }
        var notes by rememberSaveable { mutableStateOf(emptyList<String>()) }
        var failing by rememberSaveable { mutableStateOf(false) }
        val saved = notes.mapIndexed { index, note ->
            MonarchVersionHistoryPanelVersion("s$index", now, "You", note = note.ifEmpty { null })
        }.reversed()
        val versions = saved + listOf(
            MonarchVersionHistoryPanelVersion("v3", now - HOUR, "Ana Ruiz", note = "Ready for review"),
            MonarchVersionHistoryPanelVersion(
                id = "v2",
                date = now - 26 * HOUR,
                authorName = "Ben Okafor",
                automatic = true,
            ),
            MonarchVersionHistoryPanelVersion(
                id = "v1",
                date = now - 75 * HOUR,
                authorName = "Ana Ruiz",
                note = "First draft",
            ),
        )
        MonarchSwitchRow(checked = failing, onCheckedChange = { failing = it }, label = "Make save and restore fail")
        CatalogueCaption("Open a version, compare it with now, restore it, or save a new one")
        MonarchVersionHistoryPanel(
            versions = versions,
            selection = selection,
            onSelectionChange = { selection = it },
            currentLines = current,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            selectedLines = selection?.let { contents[it] ?: current },
            currentId = versions.first().id,
            now = now,
            onSaveVersion = { note ->
                delay(WORK_MILLIS)
                check(!failing) { "The server did not answer." }
                notes = notes + note
            },
            onRestore = {
                delay(WORK_MILLIS)
                check(!failing) { "The document is locked by another editor." }
            },
            onClose = { selection = null },
        )
    }
}
