@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aieditproposal

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewHunks = listOf(
    MonarchAiEditHunk("1", "Ships on Friday.", "Ships on Monday.", label = "Release date"),
    MonarchAiEditHunk("2", "The fee is 2%.", "The fee is 3%.", status = MonarchAiEditHunkStatus.Accepted),
    MonarchAiEditHunk("3", "", "A new closing paragraph.", status = MonarchAiEditHunkStatus.Rejected),
)

@MonarchPreviews
@Composable
internal fun MonarchAiEditProposalPreview() {
    PreviewFrame {
        MonarchAiEditProposal(
            part = MonarchAiEditProposalPart(id = "p1", target = "doc-1", diff = MonarchAiJSON.Null),
            hunks = PreviewHunks,
            onAcceptAll = {},
            onRejectAll = {},
            onAcceptHunk = {},
            onRejectHunk = {},
        )
        MonarchAiEditProposal(
            part = MonarchAiEditProposalPart(
                id = "p2",
                target = "doc-1",
                diff = MonarchAiJSON.Null,
                state = MonarchAiEditProposalState.Applied,
            ),
            hunks = PreviewHunks.take(1),
            onRevert = {},
        )
    }
}
