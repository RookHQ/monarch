package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aistopbutton.MonarchAiStopButton
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
