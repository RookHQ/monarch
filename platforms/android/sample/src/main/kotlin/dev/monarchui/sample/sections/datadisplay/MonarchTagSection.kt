package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.datadisplay.tag.MonarchTagSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Tag. */
object MonarchTagSection : CataloguePage {
    override val title = "Tag"

    private const val CLOSE_MARK_ROTATION = 45f

    @Composable
    override fun Content() {
        val gap = Arrangement.spacedBy(MonarchTheme.spacing.space2)
        CatalogueCaption("Read-only")
        MonarchTagSize.entries.forEach { size ->
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                MonarchTone.entries.forEach { tone -> MonarchTag(text = tone.name, tone = tone, size = size) }
            }
        }
        CatalogueCaption("Removable")
        var labels by rememberSaveable { mutableStateOf(listOf("Design", "Billing", "Urgent")) }
        FlowRow(horizontalArrangement = gap) {
            labels.forEach { label ->
                MonarchTag(
                    text = label,
                    onRemove = { labels = labels - label },
                    removeIcon = {
                        // The catalogue has no close icon; a plus turned by 45 degrees stands in for one.
                        MonarchIcon(
                            imageVector = SampleIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.rotate(CLOSE_MARK_ROTATION),
                            size = MonarchIconSize.Sm,
                        )
                    },
                    tone = MonarchTone.Info,
                )
            }
        }
        CatalogueCaption("Selectable")
        var open by rememberSaveable { mutableStateOf(true) }
        var closed by rememberSaveable { mutableStateOf(false) }
        FlowRow(horizontalArrangement = gap) {
            MonarchTag(text = "Open", selected = open, onSelectedChange = { open = it })
            MonarchTag(text = "Closed", selected = closed, onSelectedChange = { closed = it })
            MonarchTag(text = "Archived", selected = false, onSelectedChange = {}, enabled = false)
        }
    }
}
