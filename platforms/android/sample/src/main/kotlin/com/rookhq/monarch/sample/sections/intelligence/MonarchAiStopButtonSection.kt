package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aistopbutton.MonarchAiStopButton
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiStopButton. */
object MonarchAiStopButtonSection : CataloguePage {
    override val title = "AiStopButton"

    @Composable
    override fun Content() {
        var stopped by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Stop, then Continue")
        MonarchAiStopButton(onStop = { stopped = true }, stopped = stopped, onContinue = { stopped = false })
        CatalogueCaption("Symbol only, medium")
        MonarchAiStopButton(onStop = {}, size = MonarchButtonSize.Md, showLabel = false)
        CatalogueCaption("Disabled")
        MonarchAiStopButton(onStop = {}, enabled = false)
    }
}
