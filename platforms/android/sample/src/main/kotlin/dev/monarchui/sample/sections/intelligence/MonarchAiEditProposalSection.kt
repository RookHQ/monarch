@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditHunk
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditProposal
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SampleHunks = listOf(
    MonarchAiEditHunk("1", "The release ships on Friday.", "The release ships on Monday.", label = "Release date"),
    MonarchAiEditHunk("2", "The fee is 2% of the order.", "The fee is 3% of the order."),
    MonarchAiEditHunk("3", "", "Questions go to the support team."),
)

/** The catalogue page of AiEditProposal. */
object MonarchAiEditProposalSection : CataloguePage {
    override val title = "AiEditProposal"

    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableStateOf(MonarchAiEditProposalState.Proposed) }
        var round by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Accept or reject each change, or all at once")
        MonarchAiEditProposal(
            part = MonarchAiEditProposalPart(
                id = "proposal-$round",
                target = "doc-1",
                diff = MonarchAiJSON.Null,
                state = state,
            ),
            hunks = SampleHunks,
            onAcceptAll = { state = MonarchAiEditProposalState.Applied },
            onRejectAll = { state = MonarchAiEditProposalState.Rejected },
            onAcceptHunk = {},
            onRejectHunk = {},
            onRevert = { state = MonarchAiEditProposalState.Reverted },
        )
        MonarchButton(
            text = "Start again",
            onClick = {
                round++
                state = MonarchAiEditProposalState.Proposed
            },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}
