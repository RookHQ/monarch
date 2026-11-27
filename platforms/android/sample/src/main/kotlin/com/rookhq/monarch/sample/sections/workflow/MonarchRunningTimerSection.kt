package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.runningtimer.MonarchRunningTimer
import com.rookhq.monarch.components.workflow.runningtimer.MonarchRunningTimerVariant
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import kotlinx.coroutines.delay

private const val ActionDelayMillis = 500L
private const val MinuteMillis = 60_000L

/** The catalogue page of RunningTimer. */
object MonarchRunningTimerSection : CataloguePage {
    override val title = "RunningTimer"

    @Composable
    override fun Content() {
        var startedAt by rememberSaveable { mutableStateOf<Long?>(null) }
        var fail by rememberSaveable { mutableStateOf(false) }
        var logged by rememberSaveable { mutableStateOf("Nothing logged yet") }
        val stop: suspend () -> Unit = {
            delay(ActionDelayMillis)
            check(!fail) { "The server did not answer. Try again." }
            val start = startedAt
            if (start != null) logged = "Logged ${(System.currentTimeMillis() - start) / MinuteMillis} min"
            startedAt = null
        }
        MonarchSwitchRow(checked = fail, onCheckedChange = { fail = it }, label = "Make actions fail")
        MonarchText(text = logged, style = MonarchTextStyle.Caption)
        CatalogueCaption("Panel")
        MonarchRunningTimer(
            startedAt = startedAt,
            label = "ENG-142",
            onStart = {
                delay(ActionDelayMillis)
                startedAt = System.currentTimeMillis()
            },
            onStop = stop,
            onStopAt = { at ->
                delay(ActionDelayMillis)
                logged = "Logged until $at"
                startedAt = null
            },
            onDiscard = {
                startedAt = null
                logged = "Discarded"
            },
        )
        CatalogueCaption("Indicator")
        MonarchRunningTimer(
            startedAt = startedAt,
            variant = MonarchRunningTimerVariant.Indicator,
            label = "ENG-142",
            showWhenStopped = true,
            onStop = stop,
            onDiscard = { startedAt = null },
            onOpen = { logged = "Opened ENG-142" },
        )
    }
}
