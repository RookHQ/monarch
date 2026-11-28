package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.scheduling.minimonth.MonarchMiniMonth
import dev.monarchui.components.scheduling.minimonth.MonarchMiniMonthDayInfo
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import java.util.Calendar
import java.util.TimeZone

/** The catalogue page of MiniMonth. */
object MonarchMiniMonthSection : CataloguePage {
    override val title = "MiniMonth"

    private const val DAY = 86_400_000L
    private const val BOOKABLE_DAYS = 30
    private const val EVENT_EVERY = 5

    @Composable
    override fun Content() {
        var day by rememberSaveable { mutableStateOf<Long?>(null) }
        CatalogueCaption("Events, week numbers")
        MonarchMiniMonth(
            selection = day,
            onSelectionChange = { day = it },
            previousMonthIcon = { Previous() },
            nextMonthIcon = { Next() },
            showWeekNumbers = true,
            dayInfo = { date ->
                if (dayOfMonth(date) % EVENT_EVERY == 0) {
                    MonarchMiniMonthDayInfo(listOf(null, MonarchTone.Warning), "2 events")
                } else {
                    null
                }
            },
        )
        MonarchText(text = if (day == null) "No day chosen" else "Chosen: $day", style = MonarchTextStyle.Caption)

        var booking by rememberSaveable { mutableStateOf<Long?>(null) }
        val now = System.currentTimeMillis()
        CatalogueCaption("Booking: the next 30 days, no weekends")
        MonarchMiniMonth(
            selection = booking,
            onSelectionChange = { booking = it },
            previousMonthIcon = { Previous() },
            nextMonthIcon = { Next() },
            minimumDate = now,
            maximumDate = now + BOOKABLE_DAYS * DAY,
            isDateDisabled = { date -> dayOfWeek(date) == Calendar.SATURDAY || dayOfWeek(date) == Calendar.SUNDAY },
        )
    }

    @Composable
    private fun Previous() {
        MonarchIcon(imageVector = SampleIcons.ChevronLeft, contentDescription = null)
    }

    @Composable
    private fun Next() {
        MonarchIcon(
            imageVector = SampleIcons.ChevronRight,
            contentDescription = null,
        )
    }

    private fun utc(date: Long): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = date }

    private fun dayOfMonth(date: Long): Int = utc(date).get(Calendar.DAY_OF_MONTH)

    private fun dayOfWeek(date: Long): Int = utc(date).get(Calendar.DAY_OF_WEEK)
}
