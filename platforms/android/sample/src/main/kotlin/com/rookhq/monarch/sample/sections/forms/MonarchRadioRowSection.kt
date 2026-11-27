package com.rookhq.monarch.sample.sections.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.radiorow.MonarchRadioRow
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of RadioGroup. */
object MonarchRadioRowSection : CataloguePage {
    override val title = "RadioGroup"

    @Composable
    override fun Content() {
        var frequency by rememberSaveable { mutableIntStateOf(0) }
        Column(modifier = Modifier.selectableGroup()) {
            listOf("Daily", "Weekly", "Never").forEachIndexed { index, option ->
                MonarchRadioRow(selected = frequency == index, onClick = { frequency = index }, label = option)
            }
            MonarchRadioRow(
                selected = false,
                onClick = {},
                label = "Hourly",
                description = "Not on your plan.",
                enabled = false,
            )
        }
    }
}
