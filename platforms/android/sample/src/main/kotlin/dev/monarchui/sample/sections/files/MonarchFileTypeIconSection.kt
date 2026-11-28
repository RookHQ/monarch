package dev.monarchui.sample.sections.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeFamily
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of FileTypeIcon. */
object MonarchFileTypeIconSection : CataloguePage {
    override val title = "FileTypeIcon"

    private val files = listOf(
        "Holiday.heic", "Launch.mov", "Interview.m4a", "Contract.pdf", "Brief.docx", "Budget.xlsx",
        "Roadmap.key", "Backup.zip", "main.kt", "notes.md", "data.bin",
    )

    @Composable
    override fun Content() {
        MonarchFileTypeIconSize.entries.forEach { size ->
            CatalogueCaption(size.name)
            Families(size)
        }
        CatalogueCaption("From a file name")
        files.forEach { name ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchFileTypeIcon(fileName = name)
                MonarchText(name)
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Families(size: MonarchFileTypeIconSize) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchFileTypeFamily.entries.forEach { MonarchFileTypeIcon(family = it, size = size) }
        }
    }
}
