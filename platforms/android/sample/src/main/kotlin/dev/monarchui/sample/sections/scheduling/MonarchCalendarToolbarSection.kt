package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarView
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of CalendarToolbar. */
object MonarchCalendarToolbarSection : CataloguePage {
    override val title = "CalendarToolbar"

    @Composable
    override fun Content() {
        var view by rememberSaveable { mutableStateOf(MonarchCalendarToolbarView.Week) }
        var offset by rememberSaveable { mutableIntStateOf(0) }
        var busy by rememberSaveable { mutableStateOf(false) }
        val unit = view.name.lowercase()
        CatalogueCaption("Interactive")
        MonarchCalendarToolbar(
            title = if (offset == 0) "This $unit" else "$offset ${unit}s from now",
            view = view,
            onToday = { offset = 0 },
            onPrevious = { offset -= 1 },
            onNext = { offset += 1 },
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            onViewChange = { view = it },
            busy = busy,
            todayEnabled = offset != 0,
            actions = {
                MonarchButton(
                    text = if (busy) "Stop loading" else "Load",
                    onClick = { busy = !busy },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        CatalogueCaption("Without switcher, next disabled")
        MonarchCalendarToolbar(
            title = "November 2026",
            view = MonarchCalendarToolbarView.Month,
            onToday = {},
            onPrevious = {},
            onNext = {},
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            nextEnabled = false,
        )
    }
}
