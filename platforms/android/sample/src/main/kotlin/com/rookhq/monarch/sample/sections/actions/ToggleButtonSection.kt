package com.rookhq.monarch.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ToggleButton: the Material 3 toggles in the Monarch theme. */
object ToggleButtonSection : CataloguePage {
    override val title = "ToggleButton"

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var bold by rememberSaveable { mutableStateOf(false) }
        var pinned by rememberSaveable { mutableStateOf(true) }
        var archived by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Icon toggles: tonal and outlined")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            FilledTonalIconToggleButton(checked = bold, onCheckedChange = { bold = it }) {
                Icon(imageVector = SampleIcons.Bold, contentDescription = "Bold")
            }
            OutlinedIconToggleButton(checked = pinned, onCheckedChange = { pinned = it }) {
                Icon(imageVector = SampleIcons.Pin, contentDescription = "Pin")
            }
            FilledTonalIconToggleButton(checked = true, onCheckedChange = {}, enabled = false) {
                Icon(imageVector = SampleIcons.Bold, contentDescription = "Disabled")
            }
        }
        CatalogueCaption("With a text label: MonarchChip")
        MonarchChip(text = "Show archived", selected = archived, onSelectedChange = { archived = it })
        MonarchText(
            text = "Bold ${if (bold) "on" else "off"}, pin ${if (pinned) "on" else "off"}, " +
                "archived ${if (archived) "shown" else "hidden"}.",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
