package com.rookhq.monarch.sample.sections.datadisplay

import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of RelativeTime: the platform formatters in Monarch type. */
object RelativeTimeSection : CataloguePage {
    override val title = "RelativeTime"

    private const val MinutesAgo = 5L
    private const val HoursAgo = 3L
    private const val DaysAhead = 2L

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val opened = remember { System.currentTimeMillis() }
        val now by produceState(opened) {
            while (true) {
                delay(DateUtils.MINUTE_IN_MILLIS)
                value = System.currentTimeMillis()
            }
        }
        val times = listOf(
            opened - MinutesAgo * DateUtils.MINUTE_IN_MILLIS,
            opened - HoursAgo * DateUtils.HOUR_IN_MILLIS,
            opened + DaysAhead * DateUtils.DAY_IN_MILLIS,
        )
        CatalogueCaption("DateUtils.getRelativeTimeSpanString, absolute time for TalkBack")
        times.forEach { time ->
            MonarchText(
                text = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString(),
                modifier = Modifier.semantics {
                    contentDescription = DateUtils.formatDateTime(
                        context,
                        time,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME,
                    )
                },
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        CatalogueCaption("RelativeDateTimeFormatter")
        val formatter = remember { RelativeDateTimeFormatter.getInstance() }
        val day = RelativeDateTimeFormatter.AbsoluteUnit.DAY
        val week = RelativeDateTimeFormatter.AbsoluteUnit.WEEK
        MonarchText(text = formatter.format(RelativeDateTimeFormatter.Direction.LAST, day))
        MonarchText(text = formatter.format(RelativeDateTimeFormatter.Direction.NEXT, week))
    }
}
