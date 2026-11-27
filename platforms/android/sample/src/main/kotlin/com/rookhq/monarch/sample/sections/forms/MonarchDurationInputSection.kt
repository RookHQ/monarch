package com.rookhq.monarch.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInput
import com.rookhq.monarch.components.forms.durationinput.MonarchDurationInputUnit
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of DurationInput. */
object MonarchDurationInputSection : CataloguePage {
    override val title = "DurationInput"

    private const val START_ESTIMATE = 90
    private const val MAX_ESTIMATE = 480
    private const val START_LEAVE = 1560
    private const val WORK_DAY = 480

    @Composable
    override fun Content() {
        var estimate by rememberSaveable { mutableStateOf<Int?>(START_ESTIMATE) }
        var leave by rememberSaveable { mutableStateOf<Int?>(START_LEAVE) }
        CatalogueCaption("Type 1h 30m, 90, 1.5h or 1:30")
        MonarchDurationInput(
            minutes = estimate,
            onMinutesChange = { estimate = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Estimate",
            helperText = "Up to a working day.",
            maximum = MAX_ESTIMATE,
        )
        MonarchText(text = "Saved: ${estimate?.let { "$it minutes" } ?: "nothing"}")
        CatalogueCaption("Days of eight hours")
        MonarchDurationInput(
            minutes = leave,
            onMinutesChange = { leave = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Time off",
            largestUnit = MonarchDurationInputUnit.Days,
            minutesPerDay = WORK_DAY,
            step = WORK_DAY,
        )
        CatalogueCaption("Disabled")
        MonarchDurationInput(
            minutes = START_ESTIMATE,
            onMinutesChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Buffer",
            enabled = false,
        )
    }
}
