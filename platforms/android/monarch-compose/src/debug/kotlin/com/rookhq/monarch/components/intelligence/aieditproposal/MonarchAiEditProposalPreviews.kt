@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aieditproposal

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
