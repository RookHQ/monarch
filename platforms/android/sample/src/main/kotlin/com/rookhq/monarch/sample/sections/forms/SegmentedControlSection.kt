package com.rookhq.monarch.sample.sections.forms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
