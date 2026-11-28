package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of SegmentedControl: the Material 3 segmented buttons in the Monarch theme. */
object SegmentedControlSection : CataloguePage {
    override val title = "SegmentedControl"

    private val ranges = listOf("Day", "Week", "Month")
    private val states = listOf("Open 12", "Closed", "All")

    @Composable
    override fun Content() {
        var range by rememberSaveable { mutableIntStateOf(1) }
        var state by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Single choice")
        MonarchSegmentedControl {
            ranges.forEachIndexed { index, option ->
                Segment(
                    selected = index == range,
                    onClick = { range = index },
                    label = { Text(option) },
                )
            }
        }
        CatalogueCaption("A count in the label, and a disabled option")
        MonarchSegmentedControl {
            states.forEachIndexed { index, option ->
                Segment(
                    selected = index == state,
                    onClick = { state = index },
                    enabled = index != 1,
                    label = { Text(option) },
                )
            }
        }
        MonarchText(
            text = "Showing ${ranges[range]}, ${states[state]}.",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
