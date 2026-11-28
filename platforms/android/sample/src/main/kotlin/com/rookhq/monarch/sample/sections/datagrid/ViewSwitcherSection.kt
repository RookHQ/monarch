package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ViewSwitcher: Material 3 segmented buttons with an icon and a label per view. */
object ViewSwitcherSection : CataloguePage {
    override val title = "ViewSwitcher"

    private class View(val label: String, val icon: ImageVector)

    private val views = listOf(
        View("Table", SampleIcons.List),
        View("Board", SampleIcons.Board),
        View("Calendar", SampleIcons.CalendarMonth),
    )

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Table, board and calendar")
        MonarchSegmentedControl {
            views.forEachIndexed { index, view ->
                Segment(
                    selected = index == selected,
                    onClick = { selected = index },
                    icon = {
                        MonarchIcon(view.icon, contentDescription = null, size = MonarchIconSize.Sm)
                    },
                    label = { Text(view.label) },
                )
            }
        }
        MonarchText(
            text = "Showing the ${views[selected].label.lowercase()} view.",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
