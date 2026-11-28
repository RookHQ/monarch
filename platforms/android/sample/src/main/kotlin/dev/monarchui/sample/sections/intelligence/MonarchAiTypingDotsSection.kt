package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aitypingdots.MonarchAiTypingDots
import dev.monarchui.components.intelligence.aitypingdots.MonarchAiTypingDotsSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiTypingDots. */
object MonarchAiTypingDotsSection : CataloguePage {
    override val title = "AiTypingDots"

    @Composable
    override fun Content() {
        var writing by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Announced when it appears")
        if (writing) MonarchAiTypingDots()
        MonarchButton(
            text = if (writing) "Finish the reply" else "Start a reply",
            onClick = { writing = !writing },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Small, for docks and rows")
        MonarchAiTypingDots(size = MonarchAiTypingDotsSize.Sm, announce = false)
    }
}
