package com.rookhq.monarch.sample.sections.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.files.filecard.MonarchFileCard
import com.rookhq.monarch.components.files.filecard.MonarchFileCardLayout
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of FileCard: two cards with selection and a menu, and rows for a list. */
object MonarchFileCardSection : CataloguePage {
    override val title = "FileCard"

    private const val REPORT_BYTES = 2_400_000L
    private const val PHOTO_BYTES = 5_100_000L

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf(listOf("Q3 report.pdf")) }
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Cards in a grid")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            listOf("Q3 report.pdf" to REPORT_BYTES, "team-photo.jpg" to PHOTO_BYTES).forEach { (name, size) ->
                MonarchFileCard(
                    name = name,
                    modifier = Modifier.weight(1f),
                    sizeBytes = size,
                    meta = "Edited yesterday",
                    badges = if (name.endsWith(".pdf")) listOf("Shared") else emptyList(),
                    selected = name in selected,
                    onSelectedChange = { on -> selected = if (on) selected + name else selected - name },
                    onOpen = { opened = name },
                ) { FileActions(name) }
            }
        }
        CatalogueCaption("Rows in a list")
        MonarchFileCard(name = "notes.txt", sizeBytes = 900, layout = MonarchFileCardLayout.Row, onOpen = {
            opened = "notes.txt"
        })
        MonarchFileCard(
            name = "archive.zip",
            sizeBytes = 9_800_000,
            meta = "Shared by Sam",
            layout = MonarchFileCardLayout.Row,
            enabled = false,
        )
        if (opened.isNotEmpty()) MonarchText(text = "Opened $opened", color = MonarchTheme.colors.textMuted)
    }

    @Composable
    private fun FileActions(name: String) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }) {
                MonarchIcon(
                    SampleIcons.More,
                    contentDescription = "Actions for $name",
                )
            }
            MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MonarchMenuItem(text = "Rename", onClick = { expanded = false })
                MonarchMenuItem(
                    text = "Delete",
                    onClick = { expanded = false },
                    destructive = true,
                )
            }
        }
    }
}
