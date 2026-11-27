package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicator
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicatorVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiThinkingIndicator. */
object MonarchAiThinkingIndicatorSection : CataloguePage {
    override val title = "AiThinkingIndicator"

    @Composable
    override fun Content() {
        var thinking by rememberSaveable { mutableStateOf(true) }
        var startedAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
        CatalogueCaption("Shimmer, announced when it appears")
        if (thinking) MonarchAiThinkingIndicator(startedAt = startedAt)
        MonarchButton(
            text = if (thinking) "Answer arrives" else "Send another prompt",
            onClick = {
                if (!thinking) startedAt = System.currentTimeMillis()
                thinking = !thinking
            },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Mark, with a custom label")
        MonarchAiThinkingIndicator(
            variant = MonarchAiThinkingIndicatorVariant.Mark,
            label = "Searching your files…",
            startedAt = startedAt,
            announce = false,
        )
    }
}
