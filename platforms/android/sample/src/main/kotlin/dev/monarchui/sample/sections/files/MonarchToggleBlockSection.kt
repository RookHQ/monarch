package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.toggleblock.MonarchToggleBlock
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of ToggleBlock. */
object MonarchToggleBlockSection : CataloguePage {
    override val title = "ToggleBlock"

    @Composable
    override fun Content() {
        var expanded by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Keeps its own state")
        MonarchToggleBlock(summary = "Why some records are skipped") {
            MonarchText(text = "Records changed while the import runs are picked up the next night.")
        }
        CatalogueCaption("State kept by the app, with a chevron")
        MonarchToggleBlock(
            summary = "Details",
            expanded = expanded,
            onExpandedChange = { expanded = it },
            trailingIcon = {
                MonarchIcon(
                    imageVector = SampleIcons.ChevronDown,
                    contentDescription = null,
                    size = MonarchIconSize.Sm,
                )
            },
        ) {
            MonarchText(text = "Shown while expanded.")
        }
        CatalogueCaption("Disabled")
        MonarchToggleBlock(summary = "Locked section", enabled = false) { MonarchText(text = "Hidden") }
    }
}
