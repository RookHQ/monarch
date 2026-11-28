@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aiinlinediff.MonarchAiDiffSegment
import dev.monarchui.components.intelligence.aiinlinediff.MonarchAiInlineDiff
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val Before =
    "Our team meets every week to review the plan. We will ship the new flow in May, and collect feedback after."
private const val After =
    "Our team meets every Monday to review the roadmap. We will ship the redesigned flow in June, and collect " +
        "feedback after launch."

/** The catalogue page of AiInlineDiff. */
object MonarchAiInlineDiffSection : CataloguePage {
    override val title = "AiInlineDiff"

    @Composable
    override fun Content() {
        CatalogueCaption("Step through the suggestions and accept or reject each one")
        var segments by remember { mutableStateOf(MonarchAiDiffSegment.diffWords(Before, After)) }
        MonarchAiInlineDiff(
            segments = segments,
            onAccept = { id -> segments = MonarchAiDiffSegment.resolve(segments, id, accept = true) },
            onReject = { id -> segments = MonarchAiDiffSegment.resolve(segments, id, accept = false) },
        )
        MonarchButton(
            text = "Reset",
            onClick = { segments = MonarchAiDiffSegment.diffWords(Before, After) },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Read only, without the bar")
        MonarchAiInlineDiff(
            segments = MonarchAiDiffSegment.diffWords("Ships on Friday.", "Ships on Monday."),
            showNavigation = false,
        )
    }
}
